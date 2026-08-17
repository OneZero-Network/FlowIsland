package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val subject: String,
    val plannedDurationMillis: Long,
    val actualDurationMillis: Long,
    val startedAt: Long,
    val endedAt: Long,
    val completed: Boolean,
)

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(entity: StudySessionEntity)

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(actualDurationMillis), 0) FROM study_sessions WHERE startedAt >= :sinceMillis AND completed = 1")
    fun observeTotalFocusMillisSince(sinceMillis: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE startedAt >= :sinceMillis")
    fun observeSessionCountSince(sinceMillis: Long): Flow<Int>

    @Query("DELETE FROM study_sessions")
    suspend fun deleteAll()
}
