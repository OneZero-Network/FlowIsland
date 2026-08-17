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

@Entity(tableName = "expense_trips")
data class ExpenseTripEntity(
    @PrimaryKey val id: String,
    val name: String,
    val currencyCode: String,
    val createdAt: Long,
    val archived: Boolean = false,
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val amount: Double,
    val category: String,
    val note: String?,
    val date: Long,
)

data class ExpenseTripWithExpenses(
    @Embedded val trip: ExpenseTripEntity,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val expenses: List<ExpenseEntity>,
)

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertTrip(trip: ExpenseTripEntity)

    @Insert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Transaction
    @Query("SELECT * FROM expense_trips ORDER BY createdAt DESC")
    fun observeTrips(): Flow<List<ExpenseTripWithExpenses>>

    @Transaction
    @Query("SELECT * FROM expense_trips WHERE id = :tripId LIMIT 1")
    fun observeTrip(tripId: String): Flow<ExpenseTripWithExpenses?>

    @Query("UPDATE expense_trips SET archived = 1 WHERE id = :tripId")
    suspend fun archiveTrip(tripId: String)

    @Query("DELETE FROM expense_trips")
    suspend fun deleteAllTrips()

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
