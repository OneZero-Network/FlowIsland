package com.flowisland.android.core.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            // No destructive fallback: schema version 1 has no prior version to
            // migrate from. Future versions must add real Migration objects here.
            .build()

    @Provides fun provideActivityHistoryDao(db: AppDatabase): ActivityHistoryDao = db.activityHistoryDao()
    @Provides fun provideStudySessionDao(db: AppDatabase): StudySessionDao = db.studySessionDao()
    @Provides fun provideCookingDao(db: AppDatabase): CookingDao = db.cookingDao()
    @Provides fun provideFitnessSessionDao(db: AppDatabase): FitnessSessionDao = db.fitnessSessionDao()
    @Provides fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideDeliveryDao(db: AppDatabase): DeliveryDao = db.deliveryDao()
    @Provides fun provideFlightDao(db: AppDatabase): FlightDao = db.flightDao()
}
