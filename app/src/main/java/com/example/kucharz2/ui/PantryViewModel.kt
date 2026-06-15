package com.example.kucharz2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.PantryIngredientEntity
import com.example.kucharz2.data.RecipeRepository
import com.example.kucharz2.data.StandardIngredientCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PantryViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val items = repository.observePantryIngredients().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun selectPantryIngredient(name: String) {
        if (!StandardIngredientCatalog.contains(name)) return
        _query.value = ""
        viewModelScope.launch { repository.addPantryIngredient(name) }
    }

    fun deleteItem(item: PantryIngredientEntity) = viewModelScope.launch {
        repository.deletePantryIngredient(item.id)
    }
}
