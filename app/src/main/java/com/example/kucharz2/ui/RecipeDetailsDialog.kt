package com.example.kucharz2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.kucharz2.data.Recipe

@Composable
internal fun RecipeDetailsDialog(recipe: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(recipe.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(12))
            ) {
                if (recipe.tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(8))) {
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
