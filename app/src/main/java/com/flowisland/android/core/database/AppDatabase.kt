package com.flowisland.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Migration policy: this is schema version 1 (initial release), so there is
 * nothing to migrate from yet. Every future schema change MUST ship as an
 * explicit Migration(oldVersion, newVersion) added to the `.addMigrations(...)`
 * call in DatabaseModule -- fallbackToDestructiveMigration() is intentionally
 * never used in the release build, per the "do not destroy user data during
 * upgrades" requirement. It is used only for local debug iteration, gated by
 * BuildConfig-less `if (isDebuggable)` check in DatabaseModule.
 */
@Database(
    entities = [
        ActivityHistoryEntity::class,
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
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityHistoryDao(): ActivityHistoryDao
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
