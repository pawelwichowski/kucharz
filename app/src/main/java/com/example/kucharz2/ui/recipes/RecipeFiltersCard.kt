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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.kucharz2.data.MissingIngredientMode
import com.example.kucharz2.data.RecipeFilterOptions
import com.example.kucharz2.data.RecipeFilters
import com.example.kucharz2.data.StandardIngredientCatalog
import com.example.kucharz2.data.UsedIngredientsSortMode
import kotlin.math.roundToInt

private enum class FilterSection {
    MAIN,
    EXCLUDED,
    MEAL,
    CUISINE,
    DIET,
    TIME,
    RATING,
    MAX_INGREDIENTS,
    MISSING,
    SORT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeFiltersCard(
    filters: RecipeFilters,
    availableIngredients: List<String>,
    onFiltersChange: (RecipeFilters) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    var expandedSection by rememberSaveable { mutableStateOf<FilterSection?>(null) }
    var excludeQuery by rememberSaveable { mutableStateOf("") }
    val excludeSuggestions = StandardIngredientCatalog.suggestions(
        query = excludeQuery,
        excluded = filters.excludedIngredients.toSet(),
        limit = 8
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filtry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (filters.hasActiveFilters) "Aktywne" else "Domyślne",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            ExpandableFilterSection(
                title = "Główny składnik",
                value = filters.mainIngredient ?: "brak",
                expanded = expandedSection == FilterSection.MAIN,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.MAIN) }
            ) {
                if (availableIngredients.isEmpty()) {
                    Text("Najpierw wyszukaj przepisy po składnikach.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableIngredients.forEach { ingredient ->
                            FilterChip(
                                selected = filters.mainIngredient == ingredient,
                                onClick = {
                                    onFiltersChange(
                                        filters.copy(mainIngredient = if (filters.mainIngredient == ingredient) null else ingredient)
                                    )
                                },
                                label = { Text(ingredient) }
                            )
                        }
                    }
                }
            }

            ExpandableFilterSection(
                title = "Wyklucz składniki",
                value = if (filters.excludedIngredients.isEmpty()) "brak" else "${filters.excludedIngredients.size} wybrane",
                expanded = expandedSection == FilterSection.EXCLUDED,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.EXCLUDED) }
            ) {
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
            }

            ExpandableChoiceFilterSection(
                section = FilterSection.MEAL,
                title = "Rodzaj posiłku",
                value = RecipeFilterOptions.mealTypes.labelFor(filters.mealTypeTag) ?: "dowolny",
                options = RecipeFilterOptions.mealTypes,
                selectedValue = filters.mealTypeTag,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.MEAL) },
                onSelected = { onFiltersChange(filters.copy(mealTypeTag = it)) }
            )
            ExpandableChoiceFilterSection(
                section = FilterSection.CUISINE,
                title = "Kuchnia regionalna",
                value = RecipeFilterOptions.cuisines.labelFor(filters.cuisineTag) ?: "dowolna",
                options = RecipeFilterOptions.cuisines,
                selectedValue = filters.cuisineTag,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.CUISINE) },
                onSelected = { onFiltersChange(filters.copy(cuisineTag = it)) }
            )
            ExpandableChoiceFilterSection(
                section = FilterSection.DIET,
                title = "Dieta",
                value = RecipeFilterOptions.diets.labelFor(filters.dietTag) ?: "dowolna",
                options = RecipeFilterOptions.diets,
                selectedValue = filters.dietTag,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.DIET) },
                onSelected = { onFiltersChange(filters.copy(dietTag = it)) }
            )
            ExpandableChoiceFilterSection(
                section = FilterSection.TIME,
                title = "Czas przygotowania",
                value = RecipeFilterOptions.readyTimes.labelFor(filters.readyTimeTag) ?: "dowolny",
                options = RecipeFilterOptions.readyTimes,
                selectedValue = filters.readyTimeTag,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.TIME) },
                onSelected = { onFiltersChange(filters.copy(readyTimeTag = it)) }
            )
            ExpandableChoiceFilterSection(
                section = FilterSection.RATING,
                title = "Ocena",
                value = RecipeFilterOptions.ratings.labelFor(filters.minRatingTag) ?: "dowolna",
                options = RecipeFilterOptions.ratings,
                selectedValue = filters.minRatingTag,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.RATING) },
                onSelected = { onFiltersChange(filters.copy(minRatingTag = it)) }
            )
            ExpandableChoiceFilterSection(
                section = FilterSection.MAX_INGREDIENTS,
                title = "Maksymalna liczba składników",
                value = filters.maxIngredients?.let { "≤ $it" } ?: "bez limitu",
                options = RecipeFilterOptions.maxIngredients,
                selectedValue = filters.maxIngredients?.toString(),
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.MAX_INGREDIENTS) },
                onSelected = { value -> onFiltersChange(filters.copy(maxIngredients = value?.toIntOrNull())) }
            )

            ExpandableFilterSection(
                title = "Ile składników może brakować",
                value = filters.missingIngredientMode.label,
                expanded = expandedSection == FilterSection.MISSING,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.MISSING) }
            ) {
                MissingIngredientsSlider(
                    selectedMode = filters.missingIngredientMode,
                    onModeChange = { mode -> onFiltersChange(filters.copy(missingIngredientMode = mode)) }
                )
            }

            ExpandableChoiceFilterSection(
                section = FilterSection.SORT,
                title = "Sortowanie",
                value = filters.usedIngredientsSortMode?.label ?: "domyślne",
                options = RecipeFilterOptions.usedIngredientsSortModes,
                selectedValue = filters.usedIngredientsSortMode?.name,
                expandedSection = expandedSection,
                onToggle = { expandedSection = expandedSection.toggle(FilterSection.SORT) },
                onSelected = { value ->
                    onFiltersChange(filters.copy(usedIngredientsSortMode = value?.let { UsedIngredientsSortMode.valueOf(it) }))
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tylko wideo", modifier = Modifier.weight(1f))
                Switch(
                    checked = filters.videoOnly,
                    onCheckedChange = { onFiltersChange(filters.copy(videoOnly = it)) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Zastosuj") }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Wyczyść") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableChoiceFilterSection(
    section: FilterSection,
    title: String,
    value: String,
    options: List<FilterOption>,
    selectedValue: String?,
    expandedSection: FilterSection?,
    onToggle: () -> Unit,
    onSelected: (String?) -> Unit
) {
    ExpandableFilterSection(
        title = title,
        value = value,
        expanded = expandedSection == section,
        onToggle = onToggle
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = selectedValue == option.value
                FilterChip(
                    selected = selected,
                    onClick = { onSelected(if (selected) null else option.value) },
                    label = { Text(option.label) }
                )
            }
        }
    }
}

@Composable
private fun ExpandableFilterSection(
    title: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable Column.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(if (expanded) "  ▲" else "  ▼")
            }
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun MissingIngredientsSlider(
    selectedMode: MissingIngredientMode,
    onModeChange: (MissingIngredientMode) -> Unit
) {
    val modes = RecipeFilterOptions.missingIngredientModes
    val selectedIndex = modes.indexOf(selectedMode).takeIf { it >= 0 }
        ?: modes.indexOf(MissingIngredientMode.MAX_2)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wybrane: ${modes[selectedIndex].label}", color = MaterialTheme.colorScheme.primary)
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value ->
                val index = value.roundToInt().coerceIn(0, modes.lastIndex)
                onModeChange(modes[index])
            },
            valueRange = 0f..modes.lastIndex.toFloat(),
            steps = modes.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            modes.forEach { mode ->
                Text(mode.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun FilterSection?.toggle(section: FilterSection): FilterSection? = if (this == section) null else section

private fun List<FilterOption>.labelFor(value: String?): String? = firstOrNull { it.value == value }?.label
