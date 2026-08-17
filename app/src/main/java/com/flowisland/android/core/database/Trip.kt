package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val destinationLabel: String?,
    val distanceMeters: Double,
    val durationMillis: Long,
    val averageSpeedKmh: Double,
    val startedAt: Long,
    val endedAt: Long,
)

@Dao
interface TripDao {
    @Insert
    suspend fun insert(entity: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
