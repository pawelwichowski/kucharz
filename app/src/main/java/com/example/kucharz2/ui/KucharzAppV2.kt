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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kucharz2.data.PantryIngredientEntity
import com.example.kucharz2.data.Recipe
import com.example.kucharz2.data.RecipeHistoryEntity
import com.example.kucharz2.data.ShoppingItemEntity

private sealed class ScreenV2(val route: String, val label: String, val icon: String) {
    data object Ingredients : ScreenV2("ingredients", "Składniki", "🥕")
    data object Recipes : ScreenV2("recipes", "Pasujące", "🍲")
    data object Missing : ScreenV2("missing", "Brakuje", "🛒")
    data object Shopping : ScreenV2("shopping", "Zakupy", "✅")
    data object Pantry : ScreenV2("pantry", "Stałe", "🧂")
    data object History : ScreenV2("history", "Historia", "🕘")
}

private val screensV2 = listOf(
    ScreenV2.Ingredients,
    ScreenV2.Recipes,
    ScreenV2.Missing,
    ScreenV2.Shopping,
    ScreenV2.Pantry,
    ScreenV2.History
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KucharzAppV2(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: ScreenV2.Ingredients.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kucharz", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar {
                screensV2.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(ScreenV2.Ingredients.route) { saveState = true }
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
            startDestination = ScreenV2.Ingredients.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(ScreenV2.Ingredients.route) {
                IngredientInputScreenV2(onSearchFinished = { navController.navigate(ScreenV2.Recipes.route) })
            }
            composable(ScreenV2.Recipes.route) { RecipeResultsScreenV2() }
            composable(ScreenV2.Missing.route) { MissingRecipesScreenV2() }
            composable(ScreenV2.Shopping.route) { ShoppingListScreenV2() }
            composable(ScreenV2.Pantry.route) { PantryScreenV2() }
            composable(ScreenV2.History.route) { HistoryScreenV2() }
        }
    }
}

@Composable
private fun IngredientInputScreenV2(
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
            HeaderCardV2(
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
            WrappingIngredientChipsV2(
                title = "Wybrane składniki",
                items = state.ingredients,
                emptyText = "Brak składników z lodówki.",
                onRemove = viewModel::removeIngredient
            )
        }
        item {
            WrappingIngredientChipsV2(
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
        state.error?.let { error -> item { ErrorCardV2(error) } }
    }
}

@Composable
private fun RecipeResultsScreenV2(viewModel: RecipeResultsViewModel = hiltViewModel()) {
    val recipes by viewModel.recipes.collectAsState()
    val available by viewModel.availableIngredients.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()

    RecipeListContentV2(
        title = "Pasujące przepisy",
        subtitle = if (available.isEmpty()) "Najpierw wyszukaj po składnikach." else "Składniki: ${available.joinToString()}",
        emptyText = "Nie znaleziono przepisów, do których masz wszystkie składniki.",
        recipes = recipes,
        onOpen = viewModel::openRecipe,
        onAddMissing = null
    )

    selected?.let { RecipeDetailsDialogV2(recipe = it, onDismiss = viewModel::closeRecipe) }
}

@Composable
private fun MissingRecipesScreenV2(viewModel: MissingRecipesViewModel = hiltViewModel()) {
    val recipes by viewModel.recipes.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()
    val message by viewModel.message.collectAsState()

    Column(Modifier.fillMaxSize()) {
        message?.let { SuccessCardV2(message = it, onDismiss = viewModel::clearMessage) }
        Box(Modifier.weight(1f)) {
            RecipeListContentV2(
                title = "Przepisy, gdzie brakuje 1–2 składników",
                subtitle = "Brakujące składniki możesz od razu dodać do listy zakupów.",
                emptyText = "Brak propozycji z jednym lub dwoma brakującymi składnikami.",
                recipes = recipes,
                onOpen = viewModel::openRecipe,
                onAddMissing = viewModel::addMissingToShopping
            )
        }
    }

    selected?.let { RecipeDetailsDialogV2(recipe = it, onDismiss = viewModel::closeRecipe) }
}

@Composable
private fun ShoppingListScreenV2(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val input by viewModel.input.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCardV2("Lista zakupów", "Składniki z brakujących przepisów trafiają tutaj jednym kliknięciem.") }
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
            item { EmptyStateV2("Lista zakupów jest pusta.") }
        } else {
            items(items, key = { it.id }) { item -> ShoppingItemRowV2(item, viewModel) }
        }
    }
}

@Composable
private fun PantryScreenV2(viewModel: PantryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val input by viewModel.input.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCardV2("Stałe składniki", "Dodaj rzeczy, które zwykle masz w domu, np. sól, pieprz, cukier, olej.") }
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
            item { EmptyStateV2("Nie dodano jeszcze stałych składników.") }
        } else {
            items(items, key = { it.id }) { item -> PantryItemRowV2(item, viewModel) }
        }
    }
}

@Composable
private fun HistoryScreenV2(viewModel: HistoryViewModel = hiltViewModel()) {
    val history by viewModel.history.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCardV2("Historia przepisów", "Tutaj zapisują się przepisy otwarte w szczegółach.") }
        item { OutlinedButton(onClick = viewModel::clear, enabled = history.isNotEmpty()) { Text("Wyczyść historię") } }
        if (history.isEmpty()) {
            item { EmptyStateV2("Historia jest pusta.") }
        } else {
            items(history, key = { it.recipeId }) { item -> RecipeHistoryCardV2(item = item, onOpen = { viewModel.open(item) }) }
        }
    }

    selected?.let { RecipeDetailsDialogV2(recipe = it, onDismiss = viewModel::close) }
}

@Composable
private fun RecipeListContentV2(
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
        item { HeaderCardV2(title, subtitle) }
        if (recipes.isEmpty()) {
            item { EmptyStateV2(emptyText) }
        } else {
            items(recipes, key = { it.id }) { recipe ->
                RecipeCardV2(
                    recipe = recipe,
                    onOpen = { onOpen(recipe) },
                    onAddMissing = onAddMissing?.let { add -> { add(recipe) } }
                )
            }
        }
    }
}

@Composable
private fun RecipeCardV2(recipe: Recipe, onOpen: () -> Unit, onAddMissing: (() -> Unit)?) {
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
                onAddMissing?.let { OutlinedButton(onClick = it) { Text("Dodaj braki") } }
            }
        }
    }
}

@Composable
private fun ShoppingItemRowV2(item: ShoppingItemEntity, viewModel: ShoppingListViewModel) {
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

@Composable
private fun PantryItemRowV2(item: PantryIngredientEntity, viewModel: PantryViewModel) {
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

@Composable
private fun RecipeHistoryCardV2(item: RecipeHistoryEntity, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Składników: ${item.ingredients.size}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onOpen) { Text("Otwórz") }
        }
    }
}

@Composable
private fun RecipeDetailsDialogV2(recipe: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipe.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (recipe.missingIngredients.isNotEmpty()) {
                    SectionTitleV2("Brakujące składniki")
                    recipe.missingIngredients.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                }
                SectionTitleV2("Składniki")
                if (recipe.ingredients.isEmpty()) Text("API nie zwróciło składników dla tego przepisu.")
                recipe.ingredients.forEach { Text("• $it") }
                SectionTitleV2("Przygotowanie")
                if (recipe.instructions.isEmpty()) Text("API nie zwróciło instrukcji przygotowania.")
                recipe.instructions.forEachIndexed { index, step -> Text("${index + 1}. $step") }
                recipe.sourceUrl?.let { Text("Źródło: $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WrappingIngredientChipsV2(
    title: String,
    items: List<String>,
    emptyText: String,
    onRemove: ((String) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitleV2(title)
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
private fun HeaderCardV2(title: String, subtitle: String) {
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

@Composable
private fun SectionTitleV2(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyStateV2(text: String) {
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
private fun ErrorCardV2(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SuccessCardV2(message: String, onDismiss: () -> Unit) {
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
