package com.example.kucharz2.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Model używany przez UI niezależnie od dokładnego kształtu odpowiedzi API. */
data class Recipe(
    val id: String,
    val title: String,
    val ingredients: List<String>,
    val instructions: List<String> = emptyList(),
    val imageUrl: String? = null,
    val sourceUrl: String? = null,
    val tags: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val missingCount: Int = missingIngredients.size
)

data class AvailableIngredientsRequest(
    val available: List<String>,
    val required: List<String> = emptyList(),
    val limit: Int = 100
)

data class IngredientFilterRequest(
    val required: List<String>,
    val allowed: List<String>
)

@Entity(tableName = "shopping_items", indices = [Index(value = ["name"], unique = true)])
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pantry_ingredients", indices = [Index(value = ["name"], unique = true)])
data class PantryIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "permanent_excluded_ingredients", indices = [Index(value = ["name"], unique = true)])
data class PermanentExcludedIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recipe_history")
data class RecipeHistoryEntity(
    @PrimaryKey val recipeId: String,
    val title: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val imageUrl: String?,
    val sourceUrl: String?,
    val tags: List<String>,
    val viewedAt: Long = System.currentTimeMillis()
)

fun Recipe.toHistoryEntity(): RecipeHistoryEntity = RecipeHistoryEntity(
    recipeId = id,
    title = title,
    ingredients = ingredients,
    instructions = instructions,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    tags = tags,
    viewedAt = System.currentTimeMillis()
)

fun RecipeHistoryEntity.toRecipe(): Recipe = Recipe(
    id = recipeId,
    title = title,
    ingredients = ingredients,
    instructions = instructions,
    imageUrl = imageUrl,
    sourceUrl = sourceUrl,
    tags = tags
)
