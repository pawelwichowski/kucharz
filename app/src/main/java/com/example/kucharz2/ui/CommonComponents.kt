package com.example.kucharz2.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun HeaderCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle)
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
}

@Composable
internal fun EmptyState(text: String) {
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
internal fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
internal fun SuccessCard(message: String, onDismiss: () -> Unit) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SuggestionChips(
    title: String,
    query: String,
    suggestions: List<String>,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (query.isBlank()) {
            Text("Zacznij wpisywać, aby zobaczyć podpowiedzi.")
        } else if (suggestions.isEmpty()) {
            Text("Brak pasującego składnika na liście.", color = MaterialTheme.colorScheme.error)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    FilterChip(selected = false, onClick = { onSelect(suggestion) }, label = { Text(suggestion) })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RequiredIngredientChips(
    title: String,
    ingredients: List<SelectedIngredient>,
    emptyText: String,
    onRequiredChange: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        if (ingredients.isEmpty()) {
            Text(emptyText)
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
internal fun PantryRequiredIngredientChips(
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
            Text(emptyText)
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
internal fun CompactIngredientChip(
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
            Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
            Text(text = name, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onRemove != null) TextButton(onClick = onRemove) { Text("×") }
        }
    }
}
