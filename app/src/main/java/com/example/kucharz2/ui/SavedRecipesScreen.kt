package com.example.kucharz2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kucharz2.data.RecipeHistoryEntity

@Composable
internal fun SavedRecipesScreen(viewModel: SavedRecipesViewModel = hiltViewModel()) {
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val selected by viewModel.selectedRecipe.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard("Zapisane przepisy", "Tutaj pojawiają się przepisy zapisane przyciskiem Zapisz.") }
        item {
            OutlinedButton(onClick = viewModel::clear, enabled = savedRecipes.isNotEmpty()) {
                Text("Wyczyść zapisane")
            }
        }
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
