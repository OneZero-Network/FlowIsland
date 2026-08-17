package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "fitness_sessions")
data class FitnessSessionEntity(
    @PrimaryKey val id: String,
    val activityKind: String, // WALK, RUN, CYCLE, CUSTOM
    val label: String,
    val distanceMeters: Double,
    val durationMillis: Long,
    val startedAt: Long,
    val endedAt: Long,
)

@Dao
interface FitnessSessionDao {
    @Insert
    suspend fun insert(entity: FitnessSessionEntity)

    @Query("SELECT * FROM fitness_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FitnessSessionEntity>>

    @Query("SELECT COALESCE(SUM(durationMillis), 0) FROM fitness_sessions WHERE startedAt >= :sinceMillis")
    fun observeTotalDurationSince(sinceMillis: Long): Flow<Long>

    @Query("DELETE FROM fitness_sessions")
    suspend fun deleteAll()
}
