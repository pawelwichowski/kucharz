package com.example.kucharz2.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.Recipe
import com.example.kucharz2.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeResultsViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val recipes = repository.exactRecipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val nearRecipes = repository.nearRecipes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val availableIngredients = repository.availableIngredients.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val savedRecipes = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val searchLoading = repository.searchLoading.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val searchError = repository.searchError.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun openRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
        viewModelScope.launch {
            val detailed = repository.getRecipeDetails(recipe)
            if (_selectedRecipe.value?.id == recipe.id) _selectedRecipe.value = detailed
        }
    }

    fun closeRecipe() { _selectedRecipe.value = null }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.addToHistory(repository.getRecipeDetails(recipe))
            _message.value = "Zapisano przepis: ${recipe.title}"
        }
    }

    fun addMissingToShopping(recipe: Recipe) {
        viewModelScope.launch {
            repository.addMissingIngredientsToShoppingList(recipe)
            _message.value = "Dodano brakujące składniki do listy zakupów."
        }
    }

    fun clearMessage() { _message.value = null }

    fun loadRecipesWithMissingIngredients() {
        repository.refreshCurrentRecipesInBackground(limit = 100)
    }
}
