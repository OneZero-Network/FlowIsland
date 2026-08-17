package com.flowisland.android.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val orderNumber: String?,
    val etaMillis: Long?,
    val status: String, // ORDERED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface DeliveryDao {
    @Insert
    suspend fun insert(entity: DeliveryEntity)

    @Query("UPDATE deliveries SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("SELECT * FROM deliveries WHERE status != 'DELIVERED' ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DeliveryEntity>>

    @Query("DELETE FROM deliveries")
    suspend fun deleteAll()
}
