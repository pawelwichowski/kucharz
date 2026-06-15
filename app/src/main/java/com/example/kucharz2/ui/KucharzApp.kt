package com.example.kucharz2.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.kucharz2.data.PantryIngredientEntity
import com.example.kucharz2.data.Recipe
import com.example.kucharz2.data.RecipeHistoryEntity
import com.example.kucharz2.data.RecipeRepository
import com.example.kucharz2.data.ShoppingItemEntity
import com.example.kucharz2.data.StandardIngredientCatalog
import com.example.kucharz2.data.toRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INGREDIENT_PREFS_NAME = "ingredient_input_state"
private const val PREF_SELECTED_INGREDIENTS = "selected_ingredients"
private const val PREF_REQUIRED_PANTRY = "required_pantry_ingredients"
private const val PREF_INCLUDE_PANTRY = "include_pantry_ingredients"
private const val SAVED_ITEM_SEPARATOR = "\u001E"
private const val SAVED_FIELD_SEPARATOR = "\u001F"

private sealed class Screen(val route: String, val label: String, val icon: String) {
    data object Ingredients : Screen("ingredients", "Składniki", "🥕")
    data object Recipes : Screen("recipes", "Przepisy", "🍲")
    data object Shopping : Screen("shopping", "Zakupy", "✅")
    data object History : Screen("history", "Zapisane", "⭐")
    data object Pantry : Screen("pantry", "Stałe", "🧂")
    data object Settings : Screen("settings", "Ustawienia", "⚙️")
}

private val bottomScreens = listOf(
    Screen.Ingredients,
    Screen.Recipes,
    Screen.Shopping,
    Screen.History,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KucharzApp(
    isDarkTheme: Boolean = false,
    onDarkThemeChange: (Boolean) -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.Ingredients.route

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kucharz", fontWeight = FontWeight.Bold) }) },
        bottomBar = {
            NavigationBar {
                bottomScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route || (screen == Screen.Settings && currentRoute == Screen.Pantry.route),
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = false
                                popUpTo(Screen.Ingredients.route) { saveState = false }
                            }
                        },
                        icon = { Text(screen.icon) },
                        label = { Text(screen.label, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Ingredients.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Ingredients.route) { IngredientInputScreen() }
            composable(Screen.Recipes.route) { RecipeResultsScreen() }
            composable(Screen.Shopping.route) { ShoppingListScreen() }
            composable(Screen.History.route) { SavedRecipesScreen() }
            composable(Screen.Pantry.route) { PantryScreen() }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = onDarkThemeChange,
                    onOpenPantry = { navController.navigate(Screen.Pantry.route) { launchSingleTop = true } }
                )
            }
        }
    }
}

data class SelectedIngredient(
    val name: String,
    val required: Boolean = false
)

data class IngredientInputUiState(
    val query: String = "",
    val ingredients: List<SelectedIngredient> = emptyList(),
    val pantryIngredients: List<String> = emptyList(),
    val requiredPantryIngredients: Set<String> = emptySet(),
    val includePantryIngredients: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class IngredientInputViewModel @Inject constructor(
    private val repository: RecipeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(INGREDIENT_PREFS_NAME, Context.MODE_PRIVATE)
    private val editableState = MutableStateFlow(loadSavedState())

    val uiState: StateFlow<IngredientInputUiState> = combine(
        editableState,
        repository.observePantryIngredients()
    ) { state, pantry ->
        val pantryNames = pantry.map { it.name }
        state.copy(
            pantryIngredients = pantryNames,
            requiredPantryIngredients = state.requiredPantryIngredients.intersect(pantryNames.toSet())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), loadSavedState())

    val searchLoading = repository.searchLoading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val searchError = repository.searchError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) = editableState.update { it.copy(query = value, error = null) }

    fun selectIngredient(name: String) = updateSavedState { state ->
        if (!StandardIngredientCatalog.contains(name) || state.ingredients.any { it.name.equals(name, ignoreCase = true) }) {
            state.copy(query = "", error = null)
        } else {
            state.copy(
                query = "",
                ingredients = state.ingredients + SelectedIngredient(name = name),
                error = null
            )
        }
    }

    fun removeIngredient(name: String) = updateSavedState { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.name == name })
    }

    fun toggleIngredientRequired(name: String, required: Boolean) = updateSavedState { state ->
        state.copy(
            ingredients = state.ingredients.map { ingredient ->
                if (ingredient.name == name) ingredient.copy(required = required) else ingredient
            },
            error = null
        )
    }

    fun togglePantryRequired(name: String, required: Boolean) = updateSavedState { state ->
        state.copy(
            requiredPantryIngredients = if (required) state.requiredPantryIngredients + name else state.requiredPantryIngredients - name,
            error = null
        )
    }

    fun setIncludePantryIngredients(enabled: Boolean) = updateSavedState {
        it.copy(includePantryIngredients = enabled, error = null)
    }

    fun clearSelectedIngredients() = updateSavedState {
        it.copy(
            query = "",
            ingredients = emptyList(),
            requiredPantryIngredients = emptySet(),
            error = null
        )
    }

    fun search() {
        val state = editableState.value
        val availableIngredients = state.ingredients.map { it.name }
        val requiredUserIngredients = state.ingredients.filter { it.required }.map { it.name }
        val requiredPantryIngredients = if (state.includePantryIngredients) state.requiredPantryIngredients.toList() else emptyList()
        val hasPantryIngredients = uiState.value.pantryIngredients.isNotEmpty()

        if (availableIngredients.isEmpty() && (!state.includePantryIngredients || !hasPantryIngredients)) {
            editableState.update { it.copy(error = "Wybierz przynajmniej jeden składnik albo włącz stałe składniki.") }
            return
        }

        repository.refreshRecipesInBackground(
            userIngredients = availableIngredients,
            requiredIngredients = requiredUserIngredients + requiredPantryIngredients,
            limit = 20,
            includePantryIngredients = state.includePantryIngredients
        )
    }

    private fun updateSavedState(transform: (IngredientInputUiState) -> IngredientInputUiState) {
        editableState.update { current ->
            transform(current).also { saveState(it) }
        }
    }

    private fun loadSavedState(): IngredientInputUiState {
        val selectedIngredients = preferences.getString(PREF_SELECTED_INGREDIENTS, null)
            ?.toSelectedIngredients()
            .orEmpty()
        val requiredPantry = preferences.getStringSet(PREF_REQUIRED_PANTRY, emptySet()).orEmpty().toSet()
        val includePantry = preferences.getBoolean(PREF_INCLUDE_PANTRY, true)

        return IngredientInputUiState(
            ingredients = selectedIngredients,
            requiredPantryIngredients = requiredPantry,
            includePantryIngredients = includePantry
        )
    }

    private fun saveState(state: IngredientInputUiState) {
        preferences.edit()
            .putString(PREF_SELECTED_INGREDIENTS, state.ingredients.toSavedString())
            .putStringSet(PREF_REQUIRED_PANTRY, state.requiredPantryIngredients)
            .putBoolean(PREF_INCLUDE_PANTRY, state.includePantryIngredients)
            .apply()
    }

    private fun List<SelectedIngredient>.toSavedString(): String = joinToString(SAVED_ITEM_SEPARATOR) { ingredient ->
        val requiredFlag = if (ingredient.required) "1" else "0"
        "$requiredFlag$SAVED_FIELD_SEPARATOR${ingredient.name}"
    }

    private fun String.toSelectedIngredients(): List<SelectedIngredient> = split(SAVED_ITEM_SEPARATOR)
        .mapNotNull { item ->
            val parts = item.split(SAVED_FIELD_SEPARATOR, limit = 2)
            if (parts.size != 2 || parts[1].isBlank()) {
                null
            } else {
                SelectedIngredient(name = parts[1], required = parts[0] == "1")
            }
        }
}

@Composable
private fun IngredientInputScreen(viewModel: IngredientInputViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val excluded = state.ingredients.map { it.name }.toSet()
    val suggestions = StandardIngredientCatalog.suggestions(state.query, excluded)
    val hasSelectedItems = state.ingredients.isNotEmpty() || state.requiredPantryIngredients.isNotEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Wybierz składniki",
                subtitle = "Wpisz nazwę składnika i wybierz jedną z ustandaryzowanych podpowiedzi. Checkbox przy składniku oznacza, że zostanie wysłany także jako wymagany."
            )
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Szukaj składnika z listy") },
                placeholder = { Text("np. jajka, maka, pomidor") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            SuggestionChips(
                title = "Pasujące składniki",
                query = state.query,
                suggestions = suggestions,
                onSelect = viewModel::selectIngredient
            )
        }
        item {
            RequiredIngredientChips(
                title = "Wybrane składniki",
                ingredients = state.ingredients,
                emptyText = "Nie wybrano jeszcze składników z lodówki.",
                onRequiredChange = viewModel::toggleIngredientRequired,
                onRemove = viewModel::removeIngredient
            )
        }
        item {
            OutlinedButton(
                onClick = viewModel::clearSelectedIngredients,
                enabled = hasSelectedItems,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wyczyść listę")
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Uwzględnij stałe składniki", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (state.includePantryIngredients) {
                                "Stałe składniki zostaną dodane do dostępnych. Zaznacz checkbox przy stałym składniku, żeby dodać go też do wymaganych."
                            } else {
                                "Wyszukiwanie użyje tylko składników wybranych powyżej."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = state.includePantryIngredients, onCheckedChange = viewModel::setIncludePantryIngredients)
                }
            }
        }
        item {
            PantryRequiredIngredientChips(
                title = if (state.includePantryIngredients) "Stałe składniki" else "Stałe składniki pomijane",
                items = state.pantryIngredients,
                requiredItems = state.requiredPantryIngredients,
                enabled = state.includePantryIngredients,
                emptyText = "Dodaj stałe składniki w ustawieniach.",
                onRequiredChange = viewModel::togglePantryRequired
            )
        }
        item {
            Button(
                onClick = viewModel::search,
                enabled = !searchLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (searchLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Ładuję przepisy…")
                } else {
                    Text("Szukaj przepisów")
                }
            }
        }
        state.error?.let { item { ErrorCard(it) } }
        searchError?.let { item { ErrorCard(it) } }
    }
}

@HiltViewModel
class RecipeResultsViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val recipes = repository.exactRecipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val nearRecipes = repository.nearRecipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val availableIngredients = repository.availableIngredients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val savedRecipes = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val searchLoading = repository.searchLoading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val searchError = repository.searchError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun openRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
        viewModelScope.launch {
            val detailed = repository.getRecipeDetails(recipe)
            if (_selectedRecipe.value?.id == recipe.id) _selectedRecipe.value = detailed
        }
    }

    fun closeRecipe() { _selectedRecipe.value = null }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.addToHistory(repository.getRecipeDetails(recipe))
            _message.value = "Zapisano przepis: ${recipe.title}"
        }
    }

    fun addMissingToShopping(recipe: Recipe) {
        viewModelScope.launch {
            repository.addMissingIngredientsToShoppingList(recipe)
            _message.value = "Dodano brakujące składniki do listy zakupów."
        }
    }

    fun clearMessage() { _message.value = null }
    fun loadRecipesWithMissingIngredients() { repository.refreshCurrentRecipesInBackground(limit = 100) }
}

@Composable
private fun RecipeResultsScreen(viewModel: RecipeResultsViewModel = hiltViewModel()) {
    val exactRecipes by viewModel.recipes.collectAsState()
    val nearRecipes by viewModel.nearRecipes.collectAsState()
    val available by viewModel.availableIngredients.collectAsState()
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()
    val message by viewModel.message.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    var showMissingRecipes by rememberSaveable { mutableStateOf(false) }

    val visibleRecipes = if (showMissingRecipes) nearRecipes else exactRecipes
    val savedRecipeIds = savedRecipes.map { it.recipeId }.toSet()

    Column(Modifier.fillMaxSize()) {
        message?.let { SuccessCard(message = it, onDismiss = viewModel::clearMessage) }
        searchError?.let { ErrorCard(message = it) }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderCard(
                    title = if (showMissingRecipes) "Przepisy z brakującymi składnikami" else "Pełne przepisy",
                    subtitle = if (available.isEmpty()) "Najpierw wyszukaj po składnikach." else "Dostępne: ${available.joinToString()}"
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pokaż tylko przepisy z brakującymi składnikami", modifier = Modifier.weight(1f))
                        Switch(
                            checked = showMissingRecipes,
                            enabled = !searchLoading,
                            onCheckedChange = { enabled ->
                                showMissingRecipes = enabled
                                if (enabled) viewModel.loadRecipesWithMissingIngredients()
                            }
                        )
                    }
                }
            }
            if (searchLoading) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Ładuję przepisy…")
                        }
                    }
                }
            }
            if (!searchLoading && visibleRecipes.isEmpty()) {
                item { EmptyState(if (showMissingRecipes) "Nie znaleziono podobnych przepisów." else "Nie znaleziono pełnych przepisów.") }
            } else {
                items(visibleRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isSaved = recipe.id in savedRecipeIds,
                        onOpen = { viewModel.openRecipe(recipe) },
                        onSave = { viewModel.saveRecipe(recipe) },
                        onAddMissing = if (recipe.missingIngredients.isNotEmpty()) { { viewModel.addMissingToShopping(recipe) } } else null
                    )
                }
            }
        }
    }

    selected?.let { RecipeDetailsDialog(recipe = it, onDismiss = viewModel::closeRecipe) }
}

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val items = repository.observeShoppingItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    fun onInputChange(value: String) { _input.value = value }
    fun addItem() {
        val text = _input.value
        _input.value = ""
        viewModelScope.launch { repository.addShoppingItem(text) }
    }
    fun setChecked(item: ShoppingItemEntity, checked: Boolean) = viewModelScope.launch { repository.setShoppingChecked(item.id, checked) }
    fun deleteItem(item: ShoppingItemEntity) = viewModelScope.launch { repository.deleteShoppingItem(item.id) }
    fun deleteChecked() = viewModelScope.launch { repository.deleteCheckedShoppingItems() }
}

@Composable
private fun ShoppingListScreen(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val input by viewModel.input.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Lista zakupów", "Brakujące składniki można dodać tutaj z kart przepisów.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = viewModel::onInputChange,
                    label = { Text("Dodaj produkt") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = viewModel::addItem) { Text("Dodaj") }
            }
        }
        item { OutlinedButton(onClick = viewModel::deleteChecked, enabled = items.any { it.checked }) { Text("Usuń kupione") } }
        if (items.isEmpty()) {
            item { EmptyState("Lista zakupów jest pusta.") }
        } else {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.checked, onCheckedChange = { viewModel.setChecked(item, it) })
                        Text(item.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.deleteItem(item) }) { Text("Usuń") }
                    }
                }
            }
        }
    }
}

@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val savedRecipes = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe

    fun open(item: RecipeHistoryEntity) {
        val recipe = item.toRecipe()
        _selectedRecipe.value = recipe
        viewModelScope.launch {
            val detailed = repository.getRecipeDetails(recipe)
            if (_selectedRecipe.value?.id == recipe.id) _selectedRecipe.value = detailed
        }
    }

    fun close() { _selectedRecipe.value = null }
    fun delete(item: RecipeHistoryEntity) = viewModelScope.launch { repository.deleteHistory(item.recipeId) }
    fun clear() = viewModelScope.launch { repository.clearHistory() }
}

@Composable
private fun SavedRecipesScreen(viewModel: SavedRecipesViewModel = hiltViewModel()) {
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Zapisane przepisy", "Tutaj pojawiają się przepisy zapisane przyciskiem Zapisz.") }
        item { OutlinedButton(onClick = viewModel::clear, enabled = savedRecipes.isNotEmpty()) { Text("Wyczyść zapisane") } }
        if (savedRecipes.isEmpty()) {
            item { EmptyState("Nie masz jeszcze zapisanych przepisów.") }
        } else {
            items(savedRecipes, key = { it.recipeId }) { item ->
                SavedRecipeCard(item = item, onOpen = { viewModel.open(item) }, onDelete = { viewModel.delete(item) })
            }
        }
    }

    selected?.let { RecipeDetailsDialog(recipe = it, onDismiss = viewModel::close) }
}

@Composable
private fun SavedRecipeCard(item: RecipeHistoryEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Składniki: ${item.ingredients.size}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Otwórz") }
                OutlinedButton(onClick = onDelete) { Text("Usuń") }
            }
        }
    }
}

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val items = repository.observePantryIngredients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun onQueryChange(value: String) { _query.value = value }
    fun selectPantryIngredient(name: String) {
        if (!StandardIngredientCatalog.contains(name)) return
        _query.value = ""
        viewModelScope.launch { repository.addPantryIngredient(name) }
    }
    fun deleteItem(item: PantryIngredientEntity) = viewModelScope.launch { repository.deletePantryIngredient(item.id) }
}

@Composable
private fun PantryScreen(viewModel: PantryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val query by viewModel.query.collectAsState()
    val suggestions = StandardIngredientCatalog.suggestions(query, items.map { it.name }.toSet())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Stałe składniki", "Wybierz stałe składniki z tego samego ustandaryzowanego katalogu.") }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Szukaj stałego składnika z listy") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item { SuggestionChips("Pasujące stałe składniki", query, suggestions, viewModel::selectPantryIngredient) }
        if (items.isEmpty()) {
            item { EmptyState("Nie dodano jeszcze stałych składników.") }
        } else {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        TextButton(onClick = { viewModel.deleteItem(item) }) { Text("Usuń") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onOpenPantry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Ustawienia", "Preferencje aplikacji.") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tryb ciemny", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isDarkTheme) "Aplikacja używa ciemnego motywu." else "Aplikacja używa jasnego motywu.")
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stałe składniki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Zarządzaj składnikami, które zwykle masz w domu.")
                    Button(onClick = onOpenPantry) { Text("Otwórz stałe składniki") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(
    recipe: Recipe,
    isSaved: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit,
    onAddMissing: (() -> Unit)?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (recipe.ingredients.isNotEmpty()) {
                        Text(
                            text = recipe.ingredients.take(4).joinToString(),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (recipe.missingCount > 0) {
                        Text("Brakuje: ${recipe.missingIngredients.joinToString()}", color = MaterialTheme.colorScheme.error)
                    }
                }
                recipe.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Zdjęcie przepisu ${recipe.title}",
                        modifier = Modifier.width(104.dp).height(104.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Szczegóły") }
                OutlinedButton(onClick = onSave, enabled = !isSaved) { Text(if (isSaved) "Zapisany" else "Zapisz") }
                if (onAddMissing != null && recipe.missingIngredients.isNotEmpty()) {
                    OutlinedButton(onClick = onAddMissing) { Text("Dodaj braki") }
                }
            }
        }
    }
}

@Composable
private fun RecipeDetailsDialog(recipe: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipe.title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (recipe.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recipe.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
                if (recipe.missingIngredients.isNotEmpty()) {
                    SectionTitle("Brakujące składniki")
                    recipe.missingIngredients.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                }
                if (recipe.ingredients.isNotEmpty()) {
                    SectionTitle("Składniki z wyniku")
                    recipe.ingredients.forEach { Text("• $it") }
                }
                SectionTitle("Pełny przepis")
                Text("Supercook zwraca link do strony z przepisem, a nie pełną instrukcję przygotowania w aplikacji.")
                recipe.sourceUrl?.let { Text("Źródło: $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(
    title: String,
    query: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (query.isBlank()) {
            Text("Zacznij wpisywać, aby zobaczyć podpowiedzi.")
        } else if (suggestions.isEmpty()) {
            Text("Brak pasującego składnika na liście.", color = MaterialTheme.colorScheme.error)
        } else {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    FilterChip(selected = false, onClick = { onSelect(suggestion) }, label = { Text(suggestion) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RequiredIngredientChips(
    title: String,
    ingredients: List<SelectedIngredient>,
    emptyText: String,
    onRequiredChange: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (ingredients.isEmpty()) {
            Text(emptyText)
        } else {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ingredients.forEach { ingredient ->
                    CompactIngredientChip(
                        name = ingredient.name,
                        checked = ingredient.required,
                        enabled = true,
                        onCheckedChange = { onRequiredChange(ingredient.name, it) },
                        onRemove = { onRemove(ingredient.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PantryRequiredIngredientChips(
    title: String,
    items: List<String>,
    requiredItems: Set<String>,
    enabled: Boolean,
    emptyText: String,
    onRequiredChange: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (items.isEmpty()) {
            Text(emptyText)
        } else {
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    CompactIngredientChip(
                        name = item,
                        checked = item in requiredItems,
                        enabled = enabled,
                        onCheckedChange = { onRequiredChange(item, it) },
                        onRemove = null
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactIngredientChip(
    name: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRemove: (() -> Unit)?
) {
    Card(modifier = Modifier.widthIn(max = 240.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
            Text(text = name, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onRemove != null) TextButton(onClick = onRemove) { Text("×") }
        }
    }
}

@Composable
private fun HeaderCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
