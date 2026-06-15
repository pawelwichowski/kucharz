package com.example.kucharz2.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.RecipeRepository
import com.example.kucharz2.data.StandardIngredientCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val INGREDIENT_PREFS_NAME = "ingredient_input_state"
private const val PREF_SELECTED_INGREDIENTS = "selected_ingredients"
private const val PREF_REQUIRED_PANTRY = "required_pantry_ingredients"
private const val PREF_INCLUDE_PANTRY = "include_pantry_ingredients"
private const val SAVED_ITEM_SEPARATOR = "\u001E"
private const val SAVED_FIELD_SEPARATOR = "\u001F"

@HiltViewModel
class IngredientInputViewModel @Inject constructor(
    private val repository: RecipeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences(INGREDIENT_PREFS_NAME, Context.MODE_PRIVATE)
    private val editableState = MutableStateFlow(loadSavedState())

    val uiState: StateFlow<IngredientInputUiState> = combine(
        editableState,
        repository.observePantryIngredients()
    ) { state, pantry ->
        val pantryNames = pantry.map { it.name }
        state.copy(
            pantryIngredients = pantryNames,
            requiredPantryIngredients = state.requiredPantryIngredients.intersect(pantryNames.toSet())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), loadSavedState())

    val searchLoading = repository.searchLoading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val searchError = repository.searchError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onQueryChange(value: String) = editableState.update { it.copy(query = value, error = null) }

    fun selectIngredient(name: String) = updateSavedState { state ->
        if (!StandardIngredientCatalog.contains(name) || state.ingredients.any { it.name.equals(name, ignoreCase = true) }) {
            state.copy(query = "", error = null)
        } else {
            state.copy(
                query = "",
                ingredients = state.ingredients + SelectedIngredient(name = name),
                error = null
            )
        }
    }

    fun removeIngredient(name: String) = updateSavedState { state ->
        state.copy(ingredients = state.ingredients.filterNot { it.name == name })
    }

    fun toggleIngredientRequired(name: String, required: Boolean) = updateSavedState { state ->
        state.copy(
            ingredients = state.ingredients.map { ingredient ->
                if (ingredient.name == name) ingredient.copy(required = required) else ingredient
            },
            error = null
        )
    }

    fun togglePantryRequired(name: String, required: Boolean) = updateSavedState { state ->
        state.copy(
            requiredPantryIngredients = if (required) state.requiredPantryIngredients + name else state.requiredPantryIngredients - name,
            error = null
        )
    }

    fun setIncludePantryIngredients(enabled: Boolean) = updateSavedState {
        it.copy(includePantryIngredients = enabled, error = null)
    }

    fun clearSelectedIngredients() = updateSavedState {
        it.copy(
            query = "",
            ingredients = emptyList(),
            requiredPantryIngredients = emptySet(),
            error = null
        )
    }

    fun search() {
        val state = editableState.value
        val availableIngredients = state.ingredients.map { it.name }
        val requiredUserIngredients = state.ingredients.filter { it.required }.map { it.name }
        val requiredPantryIngredients = if (state.includePantryIngredients) state.requiredPantryIngredients.toList() else emptyList()
        val hasPantryIngredients = uiState.value.pantryIngredients.isNotEmpty()

        if (availableIngredients.isEmpty() && (!state.includePantryIngredients || !hasPantryIngredients)) {
            editableState.update { it.copy(error = "Wybierz przynajmniej jeden składnik albo włącz stałe składniki.") }
            return
        }

        repository.refreshRecipesInBackground(
            userIngredients = availableIngredients,
            requiredIngredients = requiredUserIngredients + requiredPantryIngredients,
            limit = 20,
            includePantryIngredients = state.includePantryIngredients
        )
    }

    private fun updateSavedState(transform: (IngredientInputUiState) -> IngredientInputUiState) {
        editableState.update { current ->
            transform(current).also { saveState(it) }
        }
    }

    private fun loadSavedState(): IngredientInputUiState {
        val selectedIngredients = preferences.getString(PREF_SELECTED_INGREDIENTS, null)
            ?.toSelectedIngredients()
            .orEmpty()
        val requiredPantry = preferences.getStringSet(PREF_REQUIRED_PANTRY, emptySet()).orEmpty().toSet()
        val includePantry = preferences.getBoolean(PREF_INCLUDE_PANTRY, true)

        return IngredientInputUiState(
            ingredients = selectedIngredients,
            requiredPantryIngredients = requiredPantry,
            includePantryIngredients = includePantry
        )
    }

    private fun saveState(state: IngredientInputUiState) {
        preferences.edit()
            .putString(PREF_SELECTED_INGREDIENTS, state.ingredients.toSavedString())
            .putStringSet(PREF_REQUIRED_PANTRY, state.requiredPantryIngredients)
            .putBoolean(PREF_INCLUDE_PANTRY, state.includePantryIngredients)
            .apply()
    }

    private fun List<SelectedIngredient>.toSavedString(): String = joinToString(SAVED_ITEM_SEPARATOR) { ingredient ->
        val requiredFlag = if (ingredient.required) "1" else "0"
        "$requiredFlag$SAVED_FIELD_SEPARATOR${ingredient.name}"
    }

    private fun String.toSelectedIngredients(): List<SelectedIngredient> = split(SAVED_ITEM_SEPARATOR)
        .mapNotNull { item ->
            val parts = item.split(SAVED_FIELD_SEPARATOR, limit = 2)
            if (parts.size != 2 || parts[1].isBlank()) {
                null
            } else {
                SelectedIngredient(name = parts[1], required = parts[0] == "1")
            }
        }
}
