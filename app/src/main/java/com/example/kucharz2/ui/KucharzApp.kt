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
    data object Pantry : Screen("pantry", "Stałe", "🧂")
    data object History : Screen("history", "Historia", "🕘")
}

private val bottomScreens = listOf(
    Screen.Ingredients,
    Screen.Recipes,
    Screen.Shopping,
    Screen.Pantry,
    Screen.History
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KucharzApp(navController: NavHostController = rememberNavController()) {
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
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Screen.Ingredients.route) { saveState = true }
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
            composable(Screen.Ingredients.route) {
                IngredientInputScreen(onSearchFinished = { navController.navigate(Screen.Recipes.route) })
            }
            composable(Screen.Recipes.route) { RecipeResultsScreen() }
            composable(Screen.Shopping.route) { ShoppingListScreen() }
            composable(Screen.Pantry.route) { PantryScreen() }
            composable(Screen.History.route) { HistoryScreen() }
        }
    }
}

data class IngredientInputUiState(
    val input: String = "",
    val ingredients: List<String> = emptyList(),
    val pantryIngredients: List<String> = emptyList(),
    val loading: Boolean = false,
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
        state.copy(pantryIngredients = pantry.map { it.name })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IngredientInputUiState())

    fun onInputChange(value: String) = editableState.update { it.copy(input = value, error = null) }

    fun addIngredient() {
        val names = editableState.value.input
            .split(',', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (names.isEmpty()) return
        editableState.update { state ->
            val merged = (state.ingredients + names).distinctBy { it.lowercase() }
            state.copy(input = "", ingredients = merged, error = null)
        }
    }

    fun removeIngredient(name: String) = editableState.update { state ->
        state.copy(ingredients = state.ingredients - name)
    }

    fun clearIngredients() = editableState.update { it.copy(ingredients = emptyList()) }

    fun search(onSuccess: () -> Unit) {
        addIngredient()
        val ingredients = editableState.value.ingredients
        val hasPantryIngredients = uiState.value.pantryIngredients.isNotEmpty()
        if (ingredients.isEmpty() && !hasPantryIngredients) {
            editableState.update { it.copy(error = "Dodaj przynajmniej jeden składnik albo stały składnik.") }
            return
        }
        viewModelScope.launch {
            editableState.update { it.copy(loading = true, error = null) }
            runCatching { repository.refreshRecipes(ingredients) }
                .onSuccess {
                    editableState.update { it.copy(loading = false) }
                    onSuccess()
                }
                .onFailure { throwable ->
                    editableState.update { it.copy(loading = false, error = throwable.message ?: "Nie udało się pobrać przepisów.") }
                }
        }
    }
}

@Composable
private fun IngredientInputScreen(
    onSearchFinished: () -> Unit,
    viewModel: IngredientInputViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                title = "Co masz w lodówce?",
                subtitle = "Wpisz składniki po przecinku albo dodawaj je pojedynczo. Stałe składniki są doliczane automatycznie."
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
            IngredientChips(
                title = "Wybrane składniki",
                items = state.ingredients,
                emptyText = "Brak składników z lodówki.",
                onRemove = viewModel::removeIngredient
            )
        }
        item {
            IngredientChips(
                title = "Stałe składniki uwzględniane w szukaniu",
                items = state.pantryIngredients,
                emptyText = "Dodaj stałe składniki w zakładce Stałe.",
                onRemove = null
            )
        }
        item {
            Button(
                onClick = { viewModel.search(onSearchFinished) },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                }
                Text("Szukaj przepisów")
            }
        }
        state.error?.let { error ->
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
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun openRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
        viewModelScope.launch { repository.addToHistory(recipe) }
    }

    fun closeRecipe() { _selectedRecipe.value = null }

    fun addMissingToShopping(recipe: Recipe) {
        viewModelScope.launch {
            repository.addMissingIngredientsToShoppingList(recipe)
            _message.value = "Dodano do listy zakupów: ${recipe.missingIngredients.joinToString()}"
        }
    }

    fun clearMessage() { _message.value = null }
}

@Composable
private fun RecipeResultsScreen(viewModel: RecipeResultsViewModel = hiltViewModel()) {
    val exactRecipes by viewModel.recipes.collectAsState()
    val nearRecipes by viewModel.nearRecipes.collectAsState()
    val available by viewModel.availableIngredients.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()
    val message by viewModel.message.collectAsState()
    var showMissingRecipes by rememberSaveable { mutableStateOf(false) }

    val visibleRecipes = if (showMissingRecipes) {
        (exactRecipes + nearRecipes).distinctBy { it.id }
    } else {
        exactRecipes
    }

    Column(Modifier.fillMaxSize()) {
        message?.let {
            SuccessCard(message = it, onDismiss = viewModel::clearMessage)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderCard(
                    title = "Przepisy",
                    subtitle = if (available.isEmpty()) "Najpierw wyszukaj po składnikach." else "Składniki: ${available.joinToString()}"
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
                            text = "Pokazuj też przepisy, gdzie brakuje składników",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = showMissingRecipes,
                            onCheckedChange = { showMissingRecipes = it }
                        )
                    }
                }
            }
            if (visibleRecipes.isEmpty()) {
                item {
                    EmptyState(
                        if (showMissingRecipes) {
                            "Nie znaleziono przepisów pasujących ani takich, gdzie brakuje 1–2 składników."
                        } else {
                            "Nie znaleziono przepisów, do których masz wszystkie składniki."
                        }
                    )
                }
            } else {
                items(visibleRecipes, key = { it.id }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onOpen = { viewModel.openRecipe(recipe) },
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

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val history = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe

    fun open(item: RecipeHistoryEntity) { _selectedRecipe.value = item.toRecipe() }
    fun close() { _selectedRecipe.value = null }
    fun clear() = viewModelScope.launch { repository.clearHistory() }
}

@Composable
private fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Historia przepisów", "Tutaj zapisują się przepisy otwarte w szczegółach.") }
        item {
            OutlinedButton(onClick = viewModel::clear, enabled = history.isNotEmpty()) { Text("Wyczyść historię") }
        }
        if (history.isEmpty()) {
            item { EmptyState("Historia jest pusta.") }
        } else {
            items(history, key = { it.recipeId }) { item ->
                RecipeHistoryCard(item = item, onOpen = { viewModel.open(item) })
            }
        }
    }

    selected?.let { RecipeDetailsDialog(recipe = it, onDismiss = viewModel::close) }
}

@Composable
private fun RecipeListContent(
    title: String,
    subtitle: String,
    emptyText: String,
    recipes: List<Recipe>,
    onOpen: (Recipe) -> Unit,
    onAddMissing: ((Recipe) -> Unit)?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard(title, subtitle) }
        if (recipes.isEmpty()) {
            item { EmptyState(emptyText) }
        } else {
            items(recipes, key = { it.id }) { recipe ->
                RecipeCard(recipe = recipe, onOpen = { onOpen(recipe) }, onAddMissing = onAddMissing?.let { { it(recipe) } })
            }
        }
    }
}

@Composable
private fun RecipeCard(recipe: Recipe, onOpen: () -> Unit, onAddMissing: (() -> Unit)?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Szczegóły") }
                if (onAddMissing != null && recipe.missingIngredients.isNotEmpty()) {
                    OutlinedButton(onClick = onAddMissing) { Text("Dodaj braki") }
                }
            }
        }
    }
}

@Composable
private fun RecipeHistoryCard(item: RecipeHistoryEntity, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Składników: ${item.ingredients.size}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpen) { Text("Otwórz") }
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
                if (recipe.instructions.isEmpty()) Text("API nie zwróciło instrukcji przygotowania.")
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
