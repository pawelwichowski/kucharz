package com.example.kucharz2.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

private const val LIST_SEPARATOR = "\u001F"

class StringListConverters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(LIST_SEPARATOR)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        value.split(LIST_SEPARATOR).map { it.trim() }.filter { it.isNotBlank() }
}

@Dao
interface KucharzDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, createdAt DESC")
    fun observeShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET checked = :checked WHERE id = :id")
    suspend fun setShoppingItemChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteShoppingItem(id: Long)

    @Query("DELETE FROM shopping_items WHERE checked = 1")
    suspend fun deleteCheckedShoppingItems()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAllShoppingItems()

    @Query("SELECT * FROM pantry_ingredients ORDER BY name COLLATE NOCASE ASC")
    fun observePantryIngredients(): Flow<List<PantryIngredientEntity>>

    @Query("SELECT * FROM pantry_ingredients ORDER BY name COLLATE NOCASE ASC")
    suspend fun getPantryIngredientsOnce(): List<PantryIngredientEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPantryIngredient(item: PantryIngredientEntity)

    @Query("DELETE FROM pantry_ingredients WHERE id = :id")
    suspend fun deletePantryIngredient(id: Long)

    @Query("SELECT * FROM permanent_excluded_ingredients ORDER BY name COLLATE NOCASE ASC")
    fun observePermanentExcludedIngredients(): Flow<List<PermanentExcludedIngredientEntity>>

    @Query("SELECT * FROM permanent_excluded_ingredients ORDER BY name COLLATE NOCASE ASC")
    suspend fun getPermanentExcludedIngredientsOnce(): List<PermanentExcludedIngredientEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPermanentExcludedIngredient(item: PermanentExcludedIngredientEntity)

    @Query("DELETE FROM permanent_excluded_ingredients WHERE id = :id")
    suspend fun deletePermanentExcludedIngredient(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: RecipeHistoryEntity)

    @Query("SELECT * FROM recipe_history ORDER BY viewedAt DESC")
    fun observeHistory(): Flow<List<RecipeHistoryEntity>>

    @Query("DELETE FROM recipe_history WHERE recipeId = :recipeId")
    suspend fun deleteHistory(recipeId: String)

    @Query("DELETE FROM recipe_history")
    suspend fun clearHistory()
}

@Database(
    entities = [
        ShoppingItemEntity::class,
        PantryIngredientEntity::class,
        PermanentExcludedIngredientEntity::class,
        RecipeHistoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(StringListConverters::class)
abstract class KucharzDatabase : RoomDatabase() {
    abstract fun dao(): KucharzDao
}
