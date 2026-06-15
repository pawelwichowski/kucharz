package com.example.kucharz2.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.kucharz2.data.Recipe
import com.example.kucharz2.ui.components.SectionTitle

@Composable
fun RecipeDetailsDialog(recipe: Recipe, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val displayIngredients = recipe.displayIngredientLines.ifEmpty { recipe.ingredients }

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
                        items(recipe.tags) { tag ->
                            AssistChip(
                                onClick = { recipe.sourceUrl?.let(uriHandler::openUri) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                if (recipe.missingIngredients.isNotEmpty()) {
                    SectionTitle("Brakujące składniki")
                    recipe.missingIngredients.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                }
                if (displayIngredients.isNotEmpty()) {
                    SectionTitle("Składniki")
                    displayIngredients.forEach { Text("• $it") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Zamknij") } }
    )
}
