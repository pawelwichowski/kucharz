package com.example.kucharz2.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kucharz2.data.FilterOption
import com.example.kucharz2.data.RecipeFilterOptions
import com.example.kucharz2.data.RecipeFilters
import com.example.kucharz2.data.StandardIngredientCatalog
import com.example.kucharz2.ui.components.SectionTitle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeFiltersCard(
    filters: RecipeFilters,
    onFiltersChange: (RecipeFilters) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    var mainQuery by rememberSaveable { mutableStateOf("") }
    var excludeQuery by rememberSaveable { mutableStateOf("") }
    val mainSuggestions = StandardIngredientCatalog.suggestions(
        query = mainQuery,
        excluded = setOfNotNull(filters.mainIngredient),
        limit = 8
    )
    val excludeSuggestions = StandardIngredientCatalog.suggestions(
        query = excludeQuery,
        excluded = filters.excludedIngredients.toSet(),
        limit = 8
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filtry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (filters.hasActiveFilters) {
                    Text("Aktywne", color = MaterialTheme.colorScheme.primary)
                }
            }

            SectionTitle("Główny składnik")
            filters.mainIngredient?.let { ingredient ->
                FilterChip(
                    selected = true,
                    onClick = { onFiltersChange(filters.copy(mainIngredient = null)) },
                    label = { Text("$ingredient ×") }
                )
            } ?: run {
                OutlinedTextField(
                    value = mainQuery,
                    onValueChange = { mainQuery = it },
                    label = { Text("Szukaj głównego składnika") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (mainQuery.isNotBlank()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mainSuggestions.forEach { suggestion ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    onFiltersChange(filters.copy(mainIngredient = suggestion))
                                    mainQuery = ""
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }
            }

            SectionTitle("Wyklucz składniki")
            OutlinedTextField(
                value = excludeQuery,
                onValueChange = { excludeQuery = it },
                label = { Text("Szukaj składnika do wykluczenia") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (excludeQuery.isNotBlank()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    excludeSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onFiltersChange(filters.copy(excludedIngredients = filters.excludedIngredients + suggestion))
                                excludeQuery = ""
                            },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
            if (filters.excludedIngredients.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.excludedIngredients.forEach { ingredient ->
                        FilterChip(
                            selected = true,
                            onClick = { onFiltersChange(filters.copy(excludedIngredients = filters.excludedIngredients - ingredient)) },
                            label = { Text("$ingredient ×") }
                        )
                    }
                }
            }

            SingleChoiceFilterChips("Rodzaj posiłku", RecipeFilterOptions.mealTypes, filters.mealTypeTag) {
                onFiltersChange(filters.copy(mealTypeTag = it))
            }
            SingleChoiceFilterChips("Kuchnia regionalna", RecipeFilterOptions.cuisines, filters.cuisineTag) {
                onFiltersChange(filters.copy(cuisineTag = it))
            }
            SingleChoiceFilterChips("Dieta", RecipeFilterOptions.diets, filters.dietTag) {
                onFiltersChange(filters.copy(dietTag = it))
            }
            SingleChoiceFilterChips("Czas przygotowania", RecipeFilterOptions.readyTimes, filters.readyTimeTag) {
                onFiltersChange(filters.copy(readyTimeTag = it))
            }
            SingleChoiceFilterChips("Ocena", RecipeFilterOptions.ratings, filters.minRatingTag) {
                onFiltersChange(filters.copy(minRatingTag = it))
            }
            SingleChoiceFilterChips("Maksymalna liczba składników", RecipeFilterOptions.maxIngredients, filters.maxIngredients?.toString()) { value ->
                onFiltersChange(filters.copy(maxIngredients = value?.toIntOrNull()))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Brakujący dokładnie 1 składnik", modifier = Modifier.weight(1f))
                Switch(
                    checked = filters.oneMissingIngredientOnly,
                    onCheckedChange = { onFiltersChange(filters.copy(oneMissingIngredientOnly = it)) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tylko wideo", modifier = Modifier.weight(1f))
                Switch(
                    checked = filters.videoOnly,
                    onCheckedChange = { onFiltersChange(filters.copy(videoOnly = it)) }
                )
            }

            Text(
                text = "Filtry typu posiłku, kuchni, diety, wideo, czasu i oceny są wysyłane do Supercook w polu catname.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Zastosuj") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Wyczyść") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleChoiceFilterChips(
    title: String,
    options: List<FilterOption>,
    selectedValue: String?,
    onSelected: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedValue == null,
                onClick = { onSelected(null) },
                label = { Text("Dowolne") }
            )
            options.forEach { option ->
                FilterChip(
                    selected = selectedValue == option.value,
                    onClick = { onSelected(option.value) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}
