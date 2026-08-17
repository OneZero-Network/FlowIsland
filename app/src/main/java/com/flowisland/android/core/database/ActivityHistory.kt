package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * A durable record written once an activity reaches a terminal state. This is
 * intentionally separate from the live in-memory ActivityEngine state -- the
 * engine is a StateFlow of "what's happening right now"; this table is the
 * append-only ledger of "what has happened", which is what the History screen
 * and weekly/monthly stats read from.
 */
@Entity(tableName = "activity_history")
data class ActivityHistoryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val completed: Boolean,
)

@Dao
interface ActivityHistoryDao {
    @Insert
    suspend fun insert(entity: ActivityHistoryEntity)

    @Query("SELECT * FROM activity_history ORDER BY endedAt DESC LIMIT 200")
    fun observeRecent(): Flow<List<ActivityHistoryEntity>>

    @Query("SELECT * FROM activity_history WHERE endedAt >= :sinceMillis ORDER BY endedAt DESC")
    fun observeSince(sinceMillis: Long): Flow<List<ActivityHistoryEntity>>

    @Query("SELECT * FROM activity_history WHERE type = :type ORDER BY endedAt DESC")
    fun observeByType(type: String): Flow<List<ActivityHistoryEntity>>

    @Query("DELETE FROM activity_history")
    suspend fun deleteAll()
}
