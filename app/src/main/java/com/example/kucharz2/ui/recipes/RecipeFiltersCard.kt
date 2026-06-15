package com.example.kucharz2.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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

private enum class FilterSection { MAIN, EXCLUDED, MEAL, CUISINE, DIET, TIME, RATING, MAX_INGREDIENTS, MISSING, SORT }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeFiltersCard(
    filters: RecipeFilters,
    availableIngredients: List<String>,
    onFiltersChange: (RecipeFilters) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var expandedSection by rememberSaveable { mutableStateOf<FilterSection?>(null) }
    var excludeQuery by rememberSaveable { mutableStateOf("") }
    val excludeSuggestions = StandardIngredientCatalog.suggestions(excludeQuery, filters.excludedIngredients.toSet(), 8)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { filtersExpanded = !filtersExpanded }, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Filtry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(if (filters.hasActiveFilters) "Aktywne" else "Domyślne", color = MaterialTheme.colorScheme.primary)
                    Text(if (filtersExpanded) "  ▲" else "  ▼")
                }
            }

            if (!filtersExpanded) return@Column

            Section("Główny składnik", filters.mainIngredient ?: "brak", expandedSection == FilterSection.MAIN, { expandedSection = expandedSection.toggle(FilterSection.MAIN) }) {
                if (availableIngredients.isEmpty()) Text("Najpierw wyszukaj przepisy po składnikach.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableIngredients.forEach { ingredient ->
                        FilterChip(
                            selected = filters.mainIngredient == ingredient,
                            onClick = { onFiltersChange(filters.copy(mainIngredient = if (filters.mainIngredient == ingredient) null else ingredient)) },
                            label = { Text(ingredient) }
                        )
                    }
                }
            }

            Section("Wyklucz składniki", if (filters.excludedIngredients.isEmpty()) "brak" else "${filters.excludedIngredients.size} wybrane", expandedSection == FilterSection.EXCLUDED, { expandedSection = expandedSection.toggle(FilterSection.EXCLUDED) }) {
                OutlinedTextField(
                    value = excludeQuery,
                    onValueChange = { excludeQuery = it },
                    label = { Text("Szukaj składnika do wykluczenia") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (excludeQuery.isNotBlank()) {
                    TextChips(excludeSuggestions) { suggestion ->
                        onFiltersChange(filters.copy(excludedIngredients = filters.excludedIngredients + suggestion))
                        excludeQuery = ""
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

            ChoiceSection(FilterSection.MEAL, "Rodzaj posiłku", RecipeFilterOptions.mealTypes.labelFor(filters.mealTypeTag) ?: "dowolny", RecipeFilterOptions.mealTypes, filters.mealTypeTag, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.MEAL) }) { onFiltersChange(filters.copy(mealTypeTag = it)) }
            ChoiceSection(FilterSection.CUISINE, "Kuchnia regionalna", RecipeFilterOptions.cuisines.labelFor(filters.cuisineTag) ?: "dowolna", RecipeFilterOptions.cuisines, filters.cuisineTag, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.CUISINE) }) { onFiltersChange(filters.copy(cuisineTag = it)) }
            ChoiceSection(FilterSection.DIET, "Dieta", RecipeFilterOptions.diets.labelFor(filters.dietTag) ?: "dowolna", RecipeFilterOptions.diets, filters.dietTag, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.DIET) }) { onFiltersChange(filters.copy(dietTag = it)) }
            ChoiceSection(FilterSection.TIME, "Czas przygotowania", RecipeFilterOptions.readyTimes.labelFor(filters.readyTimeTag) ?: "dowolny", RecipeFilterOptions.readyTimes, filters.readyTimeTag, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.TIME) }) { onFiltersChange(filters.copy(readyTimeTag = it)) }
            ChoiceSection(FilterSection.RATING, "Ocena", RecipeFilterOptions.ratings.labelFor(filters.minRatingTag) ?: "dowolna", RecipeFilterOptions.ratings, filters.minRatingTag, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.RATING) }) { onFiltersChange(filters.copy(minRatingTag = it)) }
            ChoiceSection(FilterSection.MAX_INGREDIENTS, "Maksymalna liczba składników", filters.maxIngredients?.let { "≤ $it" } ?: "bez limitu", RecipeFilterOptions.maxIngredients, filters.maxIngredients?.toString(), expandedSection, { expandedSection = expandedSection.toggle(FilterSection.MAX_INGREDIENTS) }) { value -> onFiltersChange(filters.copy(maxIngredients = value?.toIntOrNull())) }

            Section("Ile składników może brakować", filters.missingIngredientMode.label, expandedSection == FilterSection.MISSING, { expandedSection = expandedSection.toggle(FilterSection.MISSING) }) {
                MissingIngredientsSlider(filters.missingIngredientMode) { mode -> onFiltersChange(filters.copy(missingIngredientMode = mode)) }
            }

            ChoiceSection(FilterSection.SORT, "Sortowanie", filters.usedIngredientsSortMode?.label ?: "domyślne", RecipeFilterOptions.usedIngredientsSortModes, filters.usedIngredientsSortMode?.name, expandedSection, { expandedSection = expandedSection.toggle(FilterSection.SORT) }) { value ->
                onFiltersChange(filters.copy(usedIngredientsSortMode = value?.let { UsedIngredientsSortMode.valueOf(it) }))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tylko wideo", modifier = Modifier.weight(1f))
                Switch(checked = filters.videoOnly, onCheckedChange = { onFiltersChange(filters.copy(videoOnly = it)) })
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
private fun ChoiceSection(
    section: FilterSection,
    title: String,
    value: String,
    options: List<FilterOption>,
    selectedValue: String?,
    expandedSection: FilterSection?,
    onToggle: () -> Unit,
    onSelected: (String?) -> Unit
) {
    Section(title, value, expandedSection == section, onToggle) {
        OptionChips(options, selectedValue, onSelected)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(options: List<FilterOption>, selectedValue: String?, onSelected: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = selectedValue == option.value
            FilterChip(selected = selected, onClick = { onSelected(if (selected) null else option.value) }, label = { Text(option.label) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextChips(options: List<String>, onSelected: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(selected = false, onClick = { onSelected(option) }, label = { Text(option) })
        }
    }
}

@Composable
private fun Section(title: String, value: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(if (expanded) "  ▲" else "  ▼")
            }
        }
        if (expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun MissingIngredientsSlider(selectedMode: MissingIngredientMode, onModeChange: (MissingIngredientMode) -> Unit) {
    val modes = RecipeFilterOptions.missingIngredientModes
    val selectedIndex = modes.indexOf(selectedMode).takeIf { it >= 0 } ?: modes.indexOf(MissingIngredientMode.MAX_2)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wybrane: ${modes[selectedIndex].label}", color = MaterialTheme.colorScheme.primary)
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { value -> onModeChange(modes[value.roundToInt().coerceIn(0, modes.lastIndex)]) },
            valueRange = 0f..modes.lastIndex.toFloat(),
            steps = modes.size - 2,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            modes.forEach { mode -> Text(mode.label, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun FilterSection?.toggle(section: FilterSection): FilterSection? = if (this == section) null else section
private fun List<FilterOption>.labelFor(value: String?): String? = firstOrNull { it.value == value }?.label
