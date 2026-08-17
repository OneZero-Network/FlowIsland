package com.flowisland.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Schema version 2 adds the durable active-activity snapshot used to recover
 * ongoing timers and activities after process death/reboot. Every future schema
 * change MUST ship as an explicit Migration(oldVersion, newVersion) added to
 * DatabaseModule. Destructive migration is intentionally never used.
 */
@Database(
    entities = [
        ActivityHistoryEntity::class,
        ActiveActivityEntity::class,
        StudySessionEntity::class,
        CookingRecipeEntity::class,
        CookingStepEntity::class,
        FitnessSessionEntity::class,
        TripEntity::class,
        ExpenseTripEntity::class,
        ExpenseEntity::class,
        ReminderEntity::class,
        DeliveryEntity::class,
        FlightEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityHistoryDao(): ActivityHistoryDao
    abstract fun activeActivityDao(): ActiveActivityDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun cookingDao(): CookingDao
    abstract fun fitnessSessionDao(): FitnessSessionDao
    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderDao(): ReminderDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun flightDao(): FlightDao

    companion object {
        const val DATABASE_NAME = "flowisland.db"
    }
}
