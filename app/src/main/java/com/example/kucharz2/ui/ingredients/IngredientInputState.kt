package com.example.kucharz2.ui.ingredients

data class SelectedIngredient(
    val name: String
)

data class IngredientInputUiState(
    val query: String = "",
    val ingredients: List<SelectedIngredient> = emptyList(),
    val pantryIngredients: List<String> = emptyList(),
    val includePantryIngredients: Boolean = true,
    val error: String? = null
)
