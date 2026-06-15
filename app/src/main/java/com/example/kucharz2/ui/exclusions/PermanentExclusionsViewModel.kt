package com.example.kucharz2.ui.exclusions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.PermanentExcludedIngredientEntity
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
class PermanentExclusionsViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val items = repository.observePermanentExcludedIngredients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun selectExcludedIngredient(name: String) {
        if (!StandardIngredientCatalog.contains(name)) return
        _query.value = ""
        viewModelScope.launch { repository.addPermanentExcludedIngredient(name) }
    }

    fun deleteItem(item: PermanentExcludedIngredientEntity) = viewModelScope.launch {
        repository.deletePermanentExcludedIngredient(item.id)
    }
}
