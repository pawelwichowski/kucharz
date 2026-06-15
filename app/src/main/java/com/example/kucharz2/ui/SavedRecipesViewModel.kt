package com.example.kucharz2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.Recipe
import com.example.kucharz2.data.RecipeHistoryEntity
import com.example.kucharz2.data.RecipeRepository
import com.example.kucharz2.data.toRecipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val savedRecipes = repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe

    fun open(item: RecipeHistoryEntity) {
        val recipe = item.toRecipe()
        _selectedRecipe.value = recipe
        viewModelScope.launch {
            val detailed = repository.getRecipeDetails(recipe)
            if (_selectedRecipe.value?.id == recipe.id) {
                _selectedRecipe.value = detailed
            }
        }
    }

    fun close() {
        _selectedRecipe.value = null
    }

    fun delete(item: RecipeHistoryEntity) = viewModelScope.launch {
        repository.deleteHistory(item.recipeId)
    }

    fun clear() = viewModelScope.launch {
        repository.clearHistory()
    }
}
