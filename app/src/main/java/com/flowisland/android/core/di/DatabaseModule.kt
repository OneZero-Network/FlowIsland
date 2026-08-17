package com.flowisland.android.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowisland.android.core.database.ActivityHistoryDao
import com.flowisland.android.core.database.AppDatabase
import com.flowisland.android.core.database.CookingDao
import com.flowisland.android.core.database.DeliveryDao
import com.flowisland.android.core.database.ExpenseDao
import com.flowisland.android.core.database.FitnessSessionDao
import com.flowisland.android.core.database.FlightDao
import com.flowisland.android.core.database.ReminderDao
import com.flowisland.android.core.database.StudySessionDao
import com.flowisland.android.core.database.TripDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS active_activities (" +
                "id TEXT NOT NULL PRIMARY KEY," +
                "type TEXT NOT NULL," +
                "title TEXT NOT NULL," +
                "subtitle TEXT," +
                "icon TEXT NOT NULL," +
                "state TEXT NOT NULL," +
                "timerDurationMillis INTEGER," +
                "timerStartedAtElapsedRealtime INTEGER," +
                "timerStartedAtWallClockMillis INTEGER," +
                "timerAccumulatedPausedMillis INTEGER," +
                "timerPausedAtElapsedRealtime INTEGER," +
                "timerPausedAtWallClockMillis INTEGER," +
                "timerCountUp INTEGER," +
                "explicitProgress REAL," +
                "pinned INTEGER NOT NULL," +
                "hidden INTEGER NOT NULL," +
                "createdAt INTEGER NOT NULL," +
                "updatedAt INTEGER NOT NULL," +
                "lastInteractedAt INTEGER NOT NULL," +
                "payloadId TEXT," +
                "expirationTime INTEGER," +
                "actionsJson TEXT NOT NULL" +
                ")")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides fun provideActivityHistoryDao(db: AppDatabase): ActivityHistoryDao = db.activityHistoryDao()
    @Provides fun provideActiveActivityDao(db: AppDatabase): ActiveActivityDao = db.activeActivityDao()
    @Provides fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()
    @Provides fun provideCookingDao(db: AppDatabase): CookingDao = db.cookingDao()
    @Provides fun provideFitnessSessionDao(db: AppDatabase): FitnessSessionDao = db.fitnessSessionDao()
    @Provides fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideDeliveryDao(db: AppDatabase): DeliveryDao = db.deliveryDao()
    @Provides fun provideFlightDao(db: AppDatabase): FlightDao = db.flightDao()
}
