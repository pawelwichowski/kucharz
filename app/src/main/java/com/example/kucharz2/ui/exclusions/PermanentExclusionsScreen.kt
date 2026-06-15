package com.example.kucharz2.ui.exclusions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.kucharz2.data.StandardIngredientCatalog
import com.example.kucharz2.ui.components.EmptyState
import com.example.kucharz2.ui.components.HeaderCard
import com.example.kucharz2.ui.components.SuggestionChips

@Composable
fun PermanentExclusionsScreen(viewModel: PermanentExclusionsViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    val query by viewModel.query.collectAsState()
    val suggestions = StandardIngredientCatalog.suggestions(query, items.map { it.name }.toSet())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderCard(
                title = "Niechciane",
                subtitle = "Składniki z tej listy będą zawsze wysyłane do Supercook jako wykluczone."
            )
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Szukaj niechcianego składnika") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        item {
            SuggestionChips(
                title = "Pasujące składniki",
                query = query,
                suggestions = suggestions,
                onSelect = viewModel::selectExcludedIngredient
            )
        }
        if (items.isEmpty()) {
            item { EmptyState("Nie dodano jeszcze niechcianych składników.") }
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
