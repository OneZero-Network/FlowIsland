package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey val id: String,
    val flightNumber: String,
    val airline: String?,
    val fromLabel: String,
    val toLabel: String,
    val boardingAtMillis: Long,
    val departureAtMillis: Long,
    val status: String, // UPCOMING, CHECKIN, BOARDING, DEPARTING, COMPLETED
    val createdAt: Long,
)

@Dao
interface FlightDao {
    @Insert
    suspend fun insert(entity: FlightEntity)

    @Query("UPDATE flights SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("SELECT * FROM flights WHERE status != 'COMPLETED' ORDER BY departureAtMillis ASC")
    fun observeActive(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights ORDER BY departureAtMillis DESC")
    fun observeAll(): Flow<List<FlightEntity>>

    @Query("DELETE FROM flights")
    suspend fun deleteAll()
}
