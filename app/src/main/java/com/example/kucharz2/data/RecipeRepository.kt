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

private const val DEFAULT_RECIPE_PAGE_LIMIT = 61

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
    private var lastFilters: RecipeFilters = RecipeFilters()
    private var lastLimit: Int = DEFAULT_RECIPE_PAGE_LIMIT
    private var nextStart: Int = 0

    private val _availableIngredients = MutableStateFlow<List<String>>(emptyList())
    val availableIngredients: StateFlow<List<String>> = _availableIngredients.asStateFlow()

    private val _exactRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val exactRecipes: StateFlow<List<Recipe>> = _exactRecipes.asStateFlow()

    private val _nearRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val nearRecipes: StateFlow<List<Recipe>> = _nearRecipes.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    private val _loadMoreLoading = MutableStateFlow(false)
    val loadMoreLoading: StateFlow<Boolean> = _loadMoreLoading.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _successfulSearchVersion = MutableStateFlow(0)
    val successfulSearchVersion: StateFlow<Int> = _successfulSearchVersion.asStateFlow()

    fun observeShoppingItems() = dao.observeShoppingItems()
    fun observePantryIngredients() = dao.observePantryIngredients()
    fun observePermanentExcludedIngredients() = dao.observePermanentExcludedIngredients()
    fun observeHistory() = dao.observeHistory()

    fun refreshRecipesInBackground(
        userIngredients: List<String>,
        requiredIngredients: List<String> = emptyList(),
        limit: Int = DEFAULT_RECIPE_PAGE_LIMIT,
        includePantryIngredients: Boolean = true,
        filters: RecipeFilters = RecipeFilters()
    ) {
        lastUserIngredients = userIngredients
        lastRequiredIngredients = requiredIngredients
        lastIncludePantryIngredients = includePantryIngredients
        lastFilters = filters
        lastLimit = limit
        refreshJob?.cancel()
        refreshJob = searchScope.launch {
            _searchLoading.value = true
            _loadMoreLoading.value = false
            _canLoadMore.value = false
            _searchError.value = null
            runCatching { refreshRecipes(userIngredients, requiredIngredients, limit, includePantryIngredients, filters) }
                .onSuccess { _successfulSearchVersion.value += 1 }
                .onFailure { throwable ->
                    _searchError.value = throwable.message ?: "Nie udało się pobrać przepisów."
                }
            _searchLoading.value = false
        }
    }

    fun refreshCurrentRecipesInBackground(limit: Int = lastLimit, filters: RecipeFilters = lastFilters) {
        refreshRecipesInBackground(
            userIngredients = lastUserIngredients,
            requiredIngredients = lastRequiredIngredients,
            limit = limit,
            includePantryIngredients = lastIncludePantryIngredients,
            filters = filters
        )
    }

    fun loadMoreRecipesInBackground() {
        if (_searchLoading.value || _loadMoreLoading.value || !_canLoadMore.value) return

        val start = nextStart
        searchScope.launch {
            _loadMoreLoading.value = true
            _searchError.value = null
            runCatching {
                fetchRecipesPage(
                    userIngredients = lastUserIngredients,
                    requiredIngredients = lastRequiredIngredients,
                    limit = lastLimit,
                    includePantryIngredients = lastIncludePantryIngredients,
                    filters = lastFilters,
                    start = start
                )
            }.onSuccess { page ->
                nextStart = start + page.rawCount
                _canLoadMore.value = page.rawCount > 0

                if (page.recipes.isNotEmpty()) {
                    val combined = (_exactRecipes.value + _nearRecipes.value + page.recipes)
                        .distinctBy { it.id }
                        .sortByUsedIngredients(lastFilters.usedIngredientsSortMode)
                    setRecipeResults(combined)
                }
            }.onFailure { throwable ->
                _searchError.value = throwable.message ?: "Nie udało się pobrać kolejnych przepisów."
            }
            _loadMoreLoading.value = false
        }
    }

    fun clearSearchError() {
        _searchError.value = null
    }

    suspend fun refreshRecipes(
        userIngredients: List<String>,
        requiredIngredients: List<String> = emptyList(),
        limit: Int = DEFAULT_RECIPE_PAGE_LIMIT,
        includePantryIngredients: Boolean = true,
        filters: RecipeFilters = RecipeFilters()
    ) = withContext(Dispatchers.IO) {
        val page = fetchRecipesPage(
            userIngredients = userIngredients,
            requiredIngredients = requiredIngredients,
            limit = limit,
            includePantryIngredients = includePantryIngredients,
            filters = filters,
            start = 0
        )
        nextStart = page.rawCount
        _canLoadMore.value = page.rawCount > 0
        setRecipeResults(page.recipes)
    }

    private suspend fun fetchRecipesPage(
        userIngredients: List<String>,
        requiredIngredients: List<String>,
        limit: Int,
        includePantryIngredients: Boolean,
        filters: RecipeFilters,
        start: Int
    ): RecipePage = withContext(Dispatchers.IO) {
        val pantry = if (includePantryIngredients) dao.getPantryIngredientsOnce().map { it.name } else emptyList()
        val permanentExclusions = dao.getPermanentExcludedIngredientsOnce().map { it.name }
        val available = normalizeInput(userIngredients + pantry)
        val apiKitchenIngredients = IngredientQueryExpander.expandForApiQuery(available)
        val required = normalizeInput(requiredIngredients + listOfNotNull(filters.mainIngredient))
        val excluded = normalizeInput(filters.excludedIngredients + permanentExclusions)
        _availableIngredients.value = available

        val response = api.getSupercookResults(
            kitchen = apiKitchenIngredients.joinToString(","),
            focus = required.joinToString(","),
            exclude = excluded.joinToString(","),
            categoryName = filters.categoryNames(),
            start = start,
            limit = limit,
            lang = "pl"
        )

        if (!response.isSuccessful) {
            throw IllegalStateException("Supercook zwrócił błąd ${response.code()}: ${response.errorBody()?.string().orEmpty()}")
        }

        val rawRecipes = SupercookParser.parse(response.body())
        val filteredRecipes = rawRecipes
            .filter { recipe -> filters.maxIngredients?.let { recipe.ingredients.size <= it } ?: true }
            .filter { recipe -> recipe.matchesMissingIngredientMode(filters.missingIngredientMode) }
            .sortByUsedIngredients(filters.usedIngredientsSortMode)

        RecipePage(rawCount = rawRecipes.size, recipes = filteredRecipes)
    }

    private fun setRecipeResults(recipes: List<Recipe>) {
        _exactRecipes.value = recipes.filter { it.missingCount == 0 }
        _nearRecipes.value = recipes.filter { it.missingCount > 0 }
    }

    suspend fun getRecipeDetails(recipe: Recipe): Recipe = withContext(Dispatchers.IO) {
        recipe
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

    suspend fun deleteAllShoppingItems() = withContext(Dispatchers.IO) {
        dao.deleteAllShoppingItems()
    }

    suspend fun addPantryIngredient(name: String) = withContext(Dispatchers.IO) {
        val clean = name.cleanupName()
        if (clean.isNotBlank()) dao.insertPantryIngredient(PantryIngredientEntity(name = clean))
    }

    suspend fun deletePantryIngredient(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePantryIngredient(id)
    }

    suspend fun addPermanentExcludedIngredient(name: String) = withContext(Dispatchers.IO) {
        val clean = name.cleanupName()
        if (clean.isNotBlank()) dao.insertPermanentExcludedIngredient(PermanentExcludedIngredientEntity(name = clean))
    }

    suspend fun deletePermanentExcludedIngredient(id: Long) = withContext(Dispatchers.IO) {
        dao.deletePermanentExcludedIngredient(id)
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

    private fun Recipe.matchesMissingIngredientMode(mode: MissingIngredientMode): Boolean = when (mode) {
        MissingIngredientMode.EXACT_0 -> missingCount == 0
        MissingIngredientMode.MAX_1 -> missingCount <= 1
        MissingIngredientMode.EXACT_1 -> missingCount == 1
        MissingIngredientMode.MAX_2 -> missingCount <= 2
        MissingIngredientMode.EXACT_2 -> missingCount == 2
        MissingIngredientMode.AT_LEAST_2 -> missingCount >= 2
    }

    private fun List<Recipe>.sortByUsedIngredients(mode: UsedIngredientsSortMode?): List<Recipe> = when (mode) {
        UsedIngredientsSortMode.MOST_USED -> sortedWith(
            compareByDescending<Recipe> { it.usedIngredientsCount() }
                .thenBy { it.missingCount }
                .thenBy { it.title.lowercase() }
        )
        UsedIngredientsSortMode.LEAST_USED -> sortedWith(
            compareBy<Recipe> { it.usedIngredientsCount() }
                .thenBy { it.missingCount }
                .thenBy { it.title.lowercase() }
        )
        null -> sortedWith(compareBy<Recipe> { it.missingCount }.thenBy { it.title.lowercase() })
    }

    private fun Recipe.usedIngredientsCount(): Int = (ingredients.size - missingIngredients.size).coerceAtLeast(0)

    private data class RecipePage(
        val rawCount: Int,
        val recipes: List<Recipe>
    )
}

object SupercookParser {
    fun parse(body: ResponseBody?): List<Recipe> {
        val text = body?.string().orEmpty().trim()
        if (text.isBlank()) return emptyList()

        val root = JSONObject(text)
        val results = root.optJSONArray("results") ?: return emptyList()

        return buildList {
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val title = obj.optString("title", "Przepis").cleanupName()
                if (title.isBlank()) continue

                val id = obj.optString("id", title.normalizedKey().ifBlank { "supercook-$i" }).cleanupName()
                val uses = obj.optString("uses", "")
                    .split(",")
                    .map { it.cleanupName() }
                    .filter { it.isNotBlank() }
                val needs = obj.optJSONArray("needs").toStringList()
                val ingredients = (uses + needs).distinctBy { it.normalizedKey() }
                val imageUrl = obj.optString("img").takeIf { it.isNotBlank() && it != "null" }
                val domain = obj.optString("domain").takeIf { it.isNotBlank() && it != "null" }

                add(
                    Recipe(
                        id = id,
                        title = title,
                        ingredients = ingredients,
                        instructions = emptyList(),
                        imageUrl = imageUrl,
                        sourceUrl = "https://www.supercook.com/#/recipes/$id",
                        tags = listOfNotNull(domain),
                        missingIngredients = needs,
                        missingCount = needs.size
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val value = optString(i).cleanupName()
                if (value.isNotBlank() && value != "null") add(value)
            }
        }
    }
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
