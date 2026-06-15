package com.example.kucharz2.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kucharz2.data.RecipeRepository
import com.example.kucharz2.data.ShoppingItemEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: RecipeRepository
) : ViewModel() {
    val items = repository.observeShoppingItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input

    fun onInputChange(value: String) { _input.value = value }

    fun addItem() {
        val text = _input.value
        _input.value = ""
        viewModelScope.launch { repository.addShoppingItem(text) }
    }

    fun setChecked(item: ShoppingItemEntity, checked: Boolean) = viewModelScope.launch {
        repository.setShoppingChecked(item.id, checked)
    }

    fun deleteItem(item: ShoppingItemEntity) = viewModelScope.launch {
        repository.deleteShoppingItem(item.id)
    }

    fun deleteChecked() = viewModelScope.launch {
        repository.deleteCheckedShoppingItems()
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAllShoppingItems()
    }
}
