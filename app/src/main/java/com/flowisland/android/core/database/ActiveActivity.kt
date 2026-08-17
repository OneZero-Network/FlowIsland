package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Durable snapshot of an ongoing FlowIsland activity.
 *
 * This is deliberately separate from the history ledger: history is append-only,
 * while this table is the crash/process-death recovery store for activities that
 * are still alive.
 */
@Entity(tableName = "active_activities")
data class ActiveActivityEntity(
    @androidx.room.PrimaryKey val id: String,
    val type: String,
    val title: String,
    val subtitle: String?,
    val icon: String,
    val state: String,
    val timerDurationMillis: Long?,
    val timerStartedAtElapsedRealtime: Long?,
    val timerStartedAtWallClockMillis: Long?,
    val timerAccumulatedPausedMillis: Long?,
    val timerPausedAtElapsedRealtime: Long?,
    val timerPausedAtWallClockMillis: Long?,
    val timerCountUp: Boolean?,
    val explicitProgress: Float?,
    val pinned: Boolean,
    val hidden: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastInteractedAt: Long,
    val payloadId: String?,
    val expirationTime: Long?,
    val actionsJson: String,
)

@Dao
interface ActiveActivityDao {
    @Query("SELECT * FROM active_activities")
    suspend fun getAll(): List<ActiveActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveActivityEntity)

    @Query("DELETE FROM active_activities WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM active_activities")
    suspend fun deleteAll()
}
