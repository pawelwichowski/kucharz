package com.example.kucharz2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun RecipeResultsScreen(viewModel: RecipeResultsViewModel = hiltViewModel()) {
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
