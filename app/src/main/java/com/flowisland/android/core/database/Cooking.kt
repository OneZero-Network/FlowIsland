package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cooking_recipes")
data class CookingRecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "cooking_steps")
data class CookingStepEntity(
    @PrimaryKey val id: String,
    val recipeId: String,
    val orderIndex: Int,
    val name: String,
    val durationMillis: Long,
)

data class CookingRecipeWithSteps(
    @Embedded val recipe: CookingRecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<CookingStepEntity>,
)

@Dao
interface CookingDao {
    @Insert
    suspend fun insertRecipe(recipe: CookingRecipeEntity)

    @Insert
    suspend fun insertSteps(steps: List<CookingStepEntity>)

    @Transaction
    suspend fun insertRecipeWithSteps(recipe: CookingRecipeEntity, steps: List<CookingStepEntity>) {
        insertRecipe(recipe)
        insertSteps(steps)
    }

    @Transaction
    @Query("SELECT * FROM cooking_recipes ORDER BY createdAt DESC")
    fun observeRecipes(): Flow<List<CookingRecipeWithSteps>>

    @Transaction
    @Query("SELECT * FROM cooking_recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeWithSteps(recipeId: String): CookingRecipeWithSteps?

    @Query("DELETE FROM cooking_recipes WHERE id = :recipeId")
    suspend fun deleteRecipe(recipeId: String)

    @Query("DELETE FROM cooking_steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsForRecipe(recipeId: String)

    @Query("DELETE FROM cooking_recipes")
    suspend fun deleteAllRecipes()

    @Query("DELETE FROM cooking_steps")
    suspend fun deleteAllSteps()
}
