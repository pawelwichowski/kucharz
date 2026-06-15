package com.example.kucharz2.ui.ingredients

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kucharz2.data.StandardIngredientCatalog
import com.example.kucharz2.ui.components.ErrorCard
import com.example.kucharz2.ui.components.PlainIngredientChips
import com.example.kucharz2.ui.components.SelectedIngredientChips
import com.example.kucharz2.ui.components.SuggestionChips

@Composable
fun IngredientInputScreen(
    onSearchCompleted: () -> Unit,
    viewModel: IngredientInputViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val successfulSearchVersion by viewModel.successfulSearchVersion.collectAsState()
    var searchRequestVersion by rememberSaveable { mutableStateOf<Int?>(null) }
    val excluded = state.ingredients.map { it.name }.toSet()
    val suggestions = StandardIngredientCatalog.suggestions(state.query, excluded)
    val hasSelectedItems = state.ingredients.isNotEmpty()

    LaunchedEffect(successfulSearchVersion, searchRequestVersion) {
        val requestedVersion = searchRequestVersion ?: return@LaunchedEffect
        if (successfulSearchVersion > requestedVersion) {
            searchRequestVersion = null
            onSearchCompleted()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Co masz w lodówce?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = viewModel::onQueryChange,
                            label = { Text("Składnik") },
                            placeholder = { Text("np. jajka") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            item {
                SuggestionChips(
                    title = "Składniki z listy",
                    query = state.query,
                    suggestions = suggestions,
                    onSelect = viewModel::selectIngredient
                )
            }

            item {
                SelectedIngredientChips(
                    title = "Wybrane składniki",
                    ingredients = state.ingredients,
                    emptyText = "Nie wybrano jeszcze składników.",
                    onRemove = viewModel::removeIngredient
                )
            }

            item {
                OutlinedButton(
                    onClick = viewModel::clearSelectedIngredients,
                    enabled = hasSelectedItems,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Wyczyść listę") }
            }

            item {
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Stałe składniki", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (state.includePantryIngredients) {
                                    "Będą automatycznie dodane do wyszukiwania."
                                } else {
                                    "Są tymczasowo pomijane."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Switch(checked = state.includePantryIngredients, onCheckedChange = viewModel::setIncludePantryIngredients)
                    }
                }
            }

            item {
                PlainIngredientChips(
                    title = if (state.includePantryIngredients) "Uwzględniane stałe składniki" else "Stałe składniki pomijane",
                    items = state.pantryIngredients,
                    enabled = state.includePantryIngredients,
                    emptyText = "Dodaj stałe składniki w ustawieniach."
                )
            }

            state.error?.let { item { ErrorCard(it) } }
            searchError?.let { item { ErrorCard(it) } }
        }

        Button(
            onClick = {
                searchRequestVersion = successfulSearchVersion
                viewModel.search()
            },
            enabled = !searchLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
}
