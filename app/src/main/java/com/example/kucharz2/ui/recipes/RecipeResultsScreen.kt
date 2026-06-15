package com.example.kucharz2.ui.recipes

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kucharz2.ui.components.EmptyState
import com.example.kucharz2.ui.components.ErrorCard
import com.example.kucharz2.ui.components.HeaderCard
import com.example.kucharz2.ui.components.SuccessCard

@Composable
fun RecipeResultsScreen(viewModel: RecipeResultsViewModel = hiltViewModel()) {
    val exactRecipes by viewModel.recipes.collectAsState()
    val nearRecipes by viewModel.nearRecipes.collectAsState()
    val available by viewModel.availableIngredients.collectAsState()
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()
    val message by viewModel.message.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val filters by viewModel.filters.collectAsState()

    val visibleRecipes = exactRecipes + nearRecipes
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
                    title = "Wyniki przepisów",
                    subtitle = if (available.isEmpty()) {
                        "Najpierw wyszukaj po składnikach."
                    } else {
                        "Dostępne: ${available.joinToString()}"
                    }
                )
            }
            item {
                RecipeFiltersCard(
                    filters = filters,
                    availableIngredients = available,
                    onFiltersChange = viewModel::setFilters,
                    onApply = viewModel::applyFilters,
                    onClear = viewModel::clearFilters
                )
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
                item { EmptyState("Nie znaleziono przepisów dla aktualnych składników i filtrów.") }
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
