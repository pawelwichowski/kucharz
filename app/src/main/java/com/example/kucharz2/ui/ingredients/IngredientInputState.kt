package com.example.kucharz2.ui.ingredients

data class SelectedIngredient(
    val name: String,
    val required: Boolean = false
)

data class IngredientInputUiState(
    val query: String = "",
    val ingredients: List<SelectedIngredient> = emptyList(),
    val pantryIngredients: List<String> = emptyList(),
    val requiredPantryIngredients: Set<String> = emptySet(),
    val includePantryIngredients: Boolean = true,
    val error: String? = null
)
