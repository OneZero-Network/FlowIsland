package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val triggerAtMillis: Long,
    val done: Boolean = false,
    val snoozeCount: Int = 0,
    val createdAt: Long,
)

@Dao
interface ReminderDao {
    @Insert
    suspend fun insert(entity: ReminderEntity)

    @Query("UPDATE reminders SET done = 1 WHERE id = :id")
    suspend fun markDone(id: String)

    @Query("UPDATE reminders SET triggerAtMillis = :newTriggerAtMillis, snoozeCount = snoozeCount + 1 WHERE id = :id")
    suspend fun snooze(id: String, newTriggerAtMillis: Long)

    @Query("SELECT * FROM reminders WHERE done = 0 ORDER BY triggerAtMillis ASC")
    fun observePending(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE done = 0 ORDER BY triggerAtMillis ASC")
    suspend fun getPendingOnce(): List<ReminderEntity>

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()
}
