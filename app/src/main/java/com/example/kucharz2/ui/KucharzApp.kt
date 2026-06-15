package com.example.kucharz2.ui

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
import com.example.kucharz2.data.toRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kucharz",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
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
                    onOpenPantry = {
                        navController.navigate(Screen.Pantry.route) {
                            launchSingleTop = true
                        }
                    }
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
    val input: String = "",
    val ingredients: List<SelectedIngredient> = emptyList(),
    val pantryIngredients: List<String> = emptyList(),
    val requiredPantryIngredients: Set<String> = emptySet(),
    val includePantryIngredients: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class IngredientInputViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    private val editableState = MutableStateFlow(IngredientInputUiState())

    val uiState: StateFlow<IngredientInputUiState> = combine(
        editableState,
        repository.observePantryIngredients()
    ) { state, pantry ->
        val pantryNames = pantry.map { it.name }
        state.copy(
            pantryIngredients = pantryNames,
            requiredPantryIngredients = state.requiredPantryIngredients.intersect(pantryNames.toSet())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IngredientInputUiState())

    val searchLoading = repository.searchLoading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val searchError = repository.searchError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onInputChange(value: String) = editableState.update { it.copy(input = value, error = null) }

    fun addIngredient() {
        val names = editableState.value.input
            .split(',', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (names.isEmpty()) return
        editableState.update { state ->
            val currentByName = state.ingredients.associateBy { it.name.lowercase() }
            val added = names.map { name -> currentByName[name.lowercase()] ?: SelectedIngredient(name = name) }
            val merged = (state.ingredients + added).distinctBy { it.name.lowercase() }
            state.copy(input = "", ingredients = merged, error = null)
        }
    }

    fun removeIngredient(name: String) = editableState.update { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.name == name })
    }

    fun toggleIngredientRequired(name: String, required: Boolean) = editableState.update { state ->
        state.copy(
            ingredients = state.ingredients.map { ingredient ->
                if (ingredient.name == name) ingredient.copy(required = required) else ingredient
            },
            error = null
        )
    }

    fun togglePantryRequired(name: String, required: Boolean) = editableState.update { state ->
        val updated = if (required) {
            state.requiredPantryIngredients + name
        } else {
            state.requiredPantryIngredients - name
        }
        state.copy(requiredPantryIngredients = updated, error = null)
    }

    fun clearIngredients() = editableState.update { it.copy(ingredients = emptyList()) }

    fun setIncludePantryIngredients(enabled: Boolean) = editableState.update {
        it.copy(includePantryIngredients = enabled, error = null)
    }

    fun clearSearchError() = repository.clearSearchError()

    fun search() {
        addIngredient()
        val state = editableState.value
        val availableIngredients = state.ingredients.map { it.name }
        val requiredUserIngredients = state.ingredients.filter { it.required }.map { it.name }
        val requiredPantryIngredients = if (state.includePantryIngredients) state.requiredPantryIngredients.toList() else emptyList()
        val requiredIngredients = requiredUserIngredients + requiredPantryIngredients
        val hasPantryIngredients = uiState.value.pantryIngredients.isNotEmpty()
        if (availableIngredients.isEmpty() && (!state.includePantryIngredients || !hasPantryIngredients)) {
            editableState.update { it.copy(error = "Dodaj przynajmniej jeden składnik albo włącz stałe składniki.") }
            return
        }
        repository.refreshRecipesInBackground(
            userIngredients = availableIngredients,
            requiredIngredients = requiredIngredients,
            limit = 20,
            includePantryIngredients = state.includePantryIngredients
        )
    }
}

@Composable
private fun IngredientInputScreen(
    viewModel: IngredientInputViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Co masz w lodówce?",
                subtitle = "Wpisz składniki po przecinku albo dodawaj je pojedynczo. Checkbox przy składniku oznacza, że ma trafić też do required."
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    label = { Text("np. jajka, mleko, pomidor") },
                    modifier = Modifier.weight(1f),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = viewModel::addIngredient) { Text("Dodaj") }
            }
        }
        item {
            RequiredIngredientChips(
                title = "Wybrane składniki",
                ingredients = state.ingredients,
                emptyText = "Brak składników z lodówki.",
                onRequiredChange = viewModel::toggleIngredientRequired,
                onRemove = viewModel::removeIngredient
            )
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
                                "Stałe składniki zostaną dodane do available. Zaznacz checkbox przy stałym składniku, żeby dodać go też do required."
                            } else {
                                "Wyszukiwanie użyje tylko składników wpisanych powyżej."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = state.includePantryIngredients,
                        onCheckedChange = viewModel::setIncludePantryIngredients
                    )
                }
            }
        }
        item {
            PantryRequiredIngredientChips(
                title = if (state.includePantryIngredients) "Stałe składniki dodawane do available" else "Stałe składniki pomijane w szukaniu",
                items = state.pantryIngredients,
                requiredItems = state.requiredPantryIngredients,
                enabled = state.includePantryIngredients,
                emptyText = "Dodaj stałe składniki w zakładce Ustawienia.",
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
                    Text("Pobieram przepisy…")
                } else {
                    Text("Szukaj przepisów")
                }
            }
        }
        if (searchLoading) {
            item { Text("Możesz przejść na inne ekrany — pobieranie będzie działać w tle.") }
        }
        state.error?.let { error ->
            item { ErrorCard(error) }
        }
        searchError?.let { error ->
            item { ErrorCard(error) }
        }
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
            if (_selectedRecipe.value?.id == recipe.id) {
                _selectedRecipe.value = detailed
            }
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
            _message.value = "Dodano do listy zakupów: ${recipe.missingIngredients.joinToString()}"
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearSearchError() = repository.clearSearchError()

    fun loadRecipesWithMissingIngredients() {
        repository.refreshCurrentRecipesInBackground(limit = 100)
    }
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
        message?.let {
            SuccessCard(message = it, onDismiss = viewModel::clearMessage)
        }
        searchError?.let {
            ErrorCard(message = it)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderCard(
                    title = if (showMissingRecipes) "Przepisy z brakującymi składnikami" else "Pełne przepisy",
                    subtitle = if (available.isEmpty()) "Najpierw wyszukaj po składnikach." else "Available: ${available.joinToString()}"
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Pokazuj tylko przepisy, gdzie brakuje składników",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                            Text(
                                if (showMissingRecipes) {
                                    "Pobieram przepisy z brakującymi składnikami…"
                                } else {
                                    "Pobieram pełne przepisy…"
                                }
                            )
                        }
                    }
                }
            }
            if (showMissingRecipes && !searchLoading && nearRecipes.isEmpty() && exactRecipes.isNotEmpty()) {
                item {
                    Text(
                        text = "Nie znaleziono przepisów z 1–2 brakującymi składnikami w pobranej puli wyników.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (!searchLoading && visibleRecipes.isEmpty()) {
                item {
                    EmptyState(
                        if (showMissingRecipes) {
                            "Nie znaleziono przepisów z 1–2 brakującymi składnikami."
                        } else {
                            "Nie znaleziono przepisów, do których masz wszystkie składniki."
                        }
                    )
                }
            } else {
                items(visibleRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isSaved = recipe.id in savedRecipeIds,
                        onOpen = { viewModel.openRecipe(recipe) },
                        onSave = { viewModel.saveRecipe(recipe) },
                        onAddMissing = if (recipe.missingIngredients.isNotEmpty()) {
                            { viewModel.addMissingToShopping(recipe) }
                        } else {
                            null
                        }
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
        item { HeaderCard("Lista zakupów", "Składniki z brakujących przepisów trafiają tutaj jednym kliknięciem.") }
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
        item {
            OutlinedButton(onClick = viewModel::deleteChecked, enabled = items.any { it.checked }) {
                Text("Usuń kupione")
            }
        }
        if (items.isEmpty()) {
            item { EmptyState("Lista zakupów jest pusta.") }
        } else {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
            if (_selectedRecipe.value?.id == recipe.id) {
                _selectedRecipe.value = detailed
            }
        }
    }

    fun close() {
        _selectedRecipe.value = null
    }

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
        item { HeaderCard("Zapisane przepisy", "Tutaj trafiają przepisy zapisane przyciskiem Zapisz.") }
        item {
            OutlinedButton(onClick = viewModel::clear, enabled = savedRecipes.isNotEmpty()) {
                Text("Wyczyść zapisane")
            }
        }
        if (savedRecipes.isEmpty()) {
            item { EmptyState("Nie masz jeszcze zapisanych przepisów.") }
        } else {
            items(savedRecipes, key = { it.recipeId }) { item ->
                SavedRecipeCard(
                    item = item,
                    onOpen = { viewModel.open(item) },
                    onDelete = { viewModel.delete(item) }
                )
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
            Text("Składników: ${item.ingredients.size}", style = MaterialTheme.typography.bodyMedium)
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
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    fun onInputChange(value: String) { _input.value = value }
    fun addItem() {
        val text = _input.value
        _input.value = ""
        viewModelScope.launch { repository.addPantryIngredient(text) }
    }
    fun deleteItem(item: PantryIngredientEntity) = viewModelScope.launch { repository.deletePantryIngredient(item.id) }
}

@Composable
private fun PantryScreen(viewModel: PantryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val input by viewModel.input.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Stałe składniki", "Dodaj rzeczy, które zwykle masz w domu, np. sól, pieprz, cukier, olej.") }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = viewModel::onInputChange,
                    label = { Text("np. cukier") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = viewModel::addItem) { Text("Dodaj") }
            }
        }
        if (items.isEmpty()) {
            item { EmptyState("Nie dodano jeszcze stałych składników.") }
        } else {
            items(items, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
        item {
            HeaderCard(
                title = "Ustawienia",
                subtitle = "Dodatkowe opcje aplikacji."
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tryb ciemny", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isDarkTheme) "Aplikacja używa ciemnego motywu." else "Aplikacja używa jasnego motywu.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChange
                    )
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stałe składniki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Zarządzaj składnikami, które zawsze masz w domu i które mogą być automatycznie doliczane do wyszukiwania.",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (recipe.ingredients.isEmpty()) "Brak listy składników w odpowiedzi API." else recipe.ingredients.take(4).joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (recipe.missingCount > 0) {
                        Text(
                            text = "Brakuje: ${recipe.missingIngredients.joinToString()}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                recipe.imageUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Zdjęcie przepisu ${recipe.title}",
                        modifier = Modifier
                            .width(104.dp)
                            .height(104.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpen) { Text("Szczegóły") }
                OutlinedButton(onClick = onSave, enabled = !isSaved) {
                    Text(if (isSaved) "Zapisany" else "Zapisz")
                }
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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (recipe.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recipe.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
                if (recipe.missingIngredients.isNotEmpty()) {
                    SectionTitle("Brakujące składniki")
                    recipe.missingIngredients.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                }
                SectionTitle("Składniki")
                if (recipe.ingredients.isEmpty()) Text("API nie zwróciło składników dla tego przepisu.")
                recipe.ingredients.forEach { Text("• $it") }
                SectionTitle("Przygotowanie")
                if (recipe.instructions.isEmpty()) Text("Pobieram instrukcje albo API ich nie zwróciło dla tego przepisu.")
                recipe.instructions.forEachIndexed { index, step -> Text("${index + 1}. $step") }
                recipe.sourceUrl?.let { Text("Źródło: $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

@Composable
private fun HeaderCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
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
            Text(emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
            Text(emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
    Card(
        modifier = Modifier.widthIn(max = 240.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
            Text(
                text = name,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onRemove != null) {
                TextButton(onClick = onRemove) { Text("×") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientChips(title: String, items: List<String>, emptyText: String, onRemove: ((String) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (items.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    FilterChip(
                        selected = true,
                        modifier = Modifier.widthIn(max = 220.dp),
                        onClick = { onRemove?.invoke(item) },
                        label = {
                            Text(
                                text = if (onRemove == null) item else "$item  ×",
                                softWrap = true,
                                maxLines = 3,
                                overflow = TextOverflow.Visible
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SuccessCard(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSecondaryContainer)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}
