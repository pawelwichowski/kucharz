package com.example.kucharz2.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val api: RecipeApi,
    private val dao: KucharzDao
) {
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshJob: Job? = null
    private var lastUserIngredients: List<String> = emptyList()
    private var lastRequiredIngredients: List<String> = emptyList()
    private var lastIncludePantryIngredients: Boolean = true

    private val _availableIngredients = MutableStateFlow<List<String>>(emptyList())
    val availableIngredients: StateFlow<List<String>> = _availableIngredients.asStateFlow()

    private val _exactRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val exactRecipes: StateFlow<List<Recipe>> = _exactRecipes.asStateFlow()

    private val _nearRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val nearRecipes: StateFlow<List<Recipe>> = _nearRecipes.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    fun observeShoppingItems() = dao.observeShoppingItems()
    fun observePantryIngredients() = dao.observePantryIngredients()
    fun observeHistory() = dao.observeHistory()

    fun refreshRecipesInBackground(
        userIngredients: List<String>,
        requiredIngredients: List<String> = emptyList(),
        limit: Int = 20,
        includePantryIngredients: Boolean = true
    ) {
        lastUserIngredients = userIngredients
        lastRequiredIngredients = requiredIngredients
        lastIncludePantryIngredients = includePantryIngredients
        refreshJob?.cancel()
        refreshJob = searchScope.launch {
            _searchLoading.value = true
            _searchError.value = null
            runCatching { refreshRecipes(userIngredients, requiredIngredients, limit, includePantryIngredients) }
                .onFailure { throwable ->
                    _searchError.value = throwable.message ?: "Nie udało się pobrać przepisów."
                }
            _searchLoading.value = false
        }
    }

    fun refreshCurrentRecipesInBackground(limit: Int = 100) {
        refreshRecipesInBackground(
            userIngredients = lastUserIngredients,
            requiredIngredients = lastRequiredIngredients,
            limit = limit,
            includePantryIngredients = lastIncludePantryIngredients
        )
    }

    fun clearSearchError() {
        _searchError.value = null
    }

    suspend fun refreshRecipes(
        userIngredients: List<String>,
        requiredIngredients: List<String> = emptyList(),
        limit: Int = 20,
        includePantryIngredients: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val pantry = if (includePantryIngredients) dao.getPantryIngredientsOnce().map { it.name } else emptyList()
        val available = normalizeInput(userIngredients + pantry)
        val required = normalizeInput(requiredIngredients)
        _availableIngredients.value = available

        val response = api.recipesByAvailableIngredients(
            AvailableIngredientsRequest(available = available, required = required, limit = limit)
        )
        if (!response.isSuccessful) {
            throw IllegalStateException("API zwróciło błąd ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }

        val parsed = RecipeResponseParser.parse(response.body())
            .map { it.withCalculatedMissingIngredients(available) }
            .sortedWith(compareBy<Recipe> { it.missingCount }.thenBy { it.title.lowercase() })

        _exactRecipes.value = parsed.filter { it.missingCount == 0 }
        _nearRecipes.value = parsed.filter { it.missingCount in 1..2 }
    }

    suspend fun getRecipeDetails(recipe: Recipe): Recipe = withContext(Dispatchers.IO) {
        val response = api.getRecipe(recipe.id)
        if (!response.isSuccessful) return@withContext recipe

        val detailed = RecipeResponseParser.parse(response.body()).firstOrNull() ?: return@withContext recipe
        recipe.mergeDetails(detailed)
    }

    suspend fun addMissingIngredientsToShoppingList(recipe: Recipe) = withContext(Dispatchers.IO) {
        recipe.missingIngredients.forEach { addShoppingItem(it) }
    }

    suspend fun addShoppingItem(name: String) = withContext(Dispatchers.IO) {
        val clean = name.cleanupName()
        if (clean.isNotBlank()) dao.insertShoppingItem(ShoppingItemEntity(name = clean))
    }

    suspend fun setShoppingChecked(id: Long, checked: Boolean) = withContext(Dispatchers.IO) {
        dao.setShoppingItemChecked(id, checked)
    }

    suspend fun deleteShoppingItem(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteShoppingItem(id)
    }

    suspend fun deleteCheckedShoppingItems() = withContext(Dispatchers.IO) {
        dao.deleteCheckedShoppingItems()
    }

    suspend fun addPantryIngredient(name: String) = withContext(Dispatchers.IO) {
        val clean = name.cleanupName()
        if (clean.isNotBlank()) dao.insertPantryIngredient(PantryIngredientEntity(name = clean))
    }

    suspend fun deletePantryIngredient(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePantryIngredient(id)
    }

    suspend fun addToHistory(recipe: Recipe) = withContext(Dispatchers.IO) {
        dao.upsertHistory(recipe.toHistoryEntity())
    }

    suspend fun deleteHistory(recipeId: String) = withContext(Dispatchers.IO) {
        dao.deleteHistory(recipeId)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearHistory()
    }

    private fun normalizeInput(items: List<String>): List<String> =
        items.map { it.cleanupName() }.filter { it.isNotBlank() }.distinctBy { it.normalizedKey() }

    private fun Recipe.withCalculatedMissingIngredients(available: List<String>): Recipe {
        val missingFromApi = missingIngredients.map { it.cleanupName() }.filter { it.isNotBlank() }
        if (missingFromApi.isNotEmpty()) return copy(missingIngredients = missingFromApi, missingCount = missingFromApi.size)

        val missing = ingredients
            .map { it.cleanupName() }
            .filter { ingredient -> available.none { availableItem -> ingredient.matchesIngredient(availableItem) } }
            .distinctBy { it.normalizedKey() }

        return copy(missingIngredients = missing, missingCount = missing.size)
    }

    private fun Recipe.mergeDetails(detailed: Recipe): Recipe = copy(
        title = detailed.title.ifBlank { title },
        ingredients = detailed.ingredients.ifEmpty { ingredients },
        instructions = detailed.instructions.ifEmpty { instructions },
        imageUrl = detailed.imageUrl ?: imageUrl,
        sourceUrl = detailed.sourceUrl ?: sourceUrl,
        tags = detailed.tags.ifEmpty { tags }
    )
}

object RecipeResponseParser {
    fun parse(body: ResponseBody?): List<Recipe> {
        val text = body?.string().orEmpty().trim()
        if (text.isBlank()) return emptyList()

        val root = JSONTokener(text).nextValue()
        return when (root) {
            is JSONArray -> root.toRecipeList()
            is JSONObject -> root.extractRecipeArray()?.toRecipeList() ?: listOf(root.toRecipe())
            else -> emptyList()
        }.filter { it.title.isNotBlank() }
    }

    private fun JSONObject.extractRecipeArray(): JSONArray? =
        optJSONArray("recipes")
            ?: optJSONArray("items")
            ?: optJSONArray("results")
            ?: optJSONArray("data")

    private fun JSONArray.toRecipeList(): List<Recipe> = buildList {
        for (i in 0 until length()) {
            when (val item = opt(i)) {
                is JSONObject -> add(item.toRecipe())
                is String -> add(Recipe(id = item.normalizedKey().ifBlank { "recipe-$i" }, title = item, ingredients = emptyList()))
            }
        }
    }

    private fun JSONObject.toRecipe(): Recipe {
        val nested = optJSONObject("recipe")
        val source = nested ?: this

        val title = source.firstString("title", "name", "recipe_name", "recipeName", "nazwa")
            ?: firstString("title", "name", "recipe_name", "recipeName", "nazwa")
            ?: "Przepis"
        val id = source.firstString("id", "recipe_id", "recipeId", "_id")
            ?: firstString("id", "recipe_id", "recipeId", "_id")
            ?: title.normalizedKey().ifBlank { title.hashCode().toString() }

        val ingredients = source.firstStringList("ingredients", "ingredientLines", "ingredient_lines", "skladniki", "required_ingredients")
            .ifEmpty { firstStringList("ingredients", "ingredientLines", "ingredient_lines", "skladniki", "required_ingredients") }
        val instructions = source.firstStringList("instructions", "instruction", "steps", "directions", "method", "preparation", "przygotowanie")
            .ifEmpty { firstStringList("instructions", "instruction", "steps", "directions", "method", "preparation", "przygotowanie") }
        val missing = firstStringList("missing_ingredients", "missingIngredients", "missing", "brakujace_skladniki")
            .ifEmpty { source.firstStringList("missing_ingredients", "missingIngredients", "missing", "brakujace_skladniki") }
        val missingCount = optIntOrNull("missing_count")
            ?: optIntOrNull("missingCount")
            ?: source.optIntOrNull("missing_count")
            ?: source.optIntOrNull("missingCount")
            ?: missing.size
        val imageUrl = source.firstString("image", "imageUrl", "image_url", "thumbnail")
            ?: firstString("image", "imageUrl", "image_url", "thumbnail")
            ?: source.firstImageUrl("images")
            ?: firstImageUrl("images")

        return Recipe(
            id = id,
            title = title,
            ingredients = ingredients,
            instructions = instructions,
            imageUrl = imageUrl,
            sourceUrl = source.firstString("url", "sourceUrl", "source_url", "link") ?: firstString("url", "sourceUrl", "source_url", "link"),
            tags = source.firstStringList("tags", "cuisines", "categories").ifEmpty { firstStringList("tags", "cuisines", "categories") },
            missingIngredients = missing,
            missingCount = missingCount
        )
    }

    private fun JSONObject.firstString(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> optString(key).takeIf { it.isNotBlank() && it != "null" } }

    private fun JSONObject.firstStringList(vararg keys: String): List<String> =
        keys.firstNotNullOfOrNull { key -> optValueAsList(key).takeIf { it.isNotEmpty() } }.orEmpty()

    private fun JSONObject.firstImageUrl(key: String): String? {
        val images = optJSONArray(key) ?: return null
        for (i in 0 until images.length()) {
            when (val item = images.opt(i)) {
                is String -> if (item.isNotBlank()) return item
                is JSONObject -> item.firstString("image_url", "imageUrl", "url", "src", "link")?.let { return it }
            }
        }
        return null
    }

    private fun JSONObject.optValueAsList(key: String): List<String> = when (val value = opt(key)) {
        is JSONArray -> value.toStringList()
        is String -> value.split('\n', ';').map { it.cleanupName() }.filter { it.isNotBlank() }
        is JSONObject -> value.names()?.toStringList().orEmpty()
        else -> emptyList()
    }

    private fun JSONArray.toStringList(): List<String> = buildList {
        for (i in 0 until length()) {
            when (val item = opt(i)) {
                is String -> add(item.cleanupName())
                is JSONObject -> add(item.firstString("name", "ingredient", "text", "original", "title", "display_text", "step", "instruction", "description").orEmpty().cleanupName())
            }
        }
    }.filter { it.isNotBlank() }

    private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key)) optInt(key) else null
}

private fun String.cleanupName(): String = trim().replace(Regex("\\s+"), " ")

private fun String.normalizedKey(): String = Normalizer.normalize(lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
    .replace(Regex("[^a-z0-9ąćęłńóśźż ]"), "")
    .trim()

private fun String.matchesIngredient(available: String): Boolean {
    val ingredient = normalizedKey()
    val item = available.normalizedKey()
    return item.isNotBlank() && (ingredient.contains(item) || item.contains(ingredient))
}
