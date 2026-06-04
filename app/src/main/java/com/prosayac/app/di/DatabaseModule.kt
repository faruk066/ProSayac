package com.prosayac.app.di

import android.content.Context
import androidx.room.Room
import com.prosayac.app.data.local.database.AppDatabase
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.util.serial.MBusSerialManager
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "prosayac_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMeterDao(database: AppDatabase): MeterDao {
        return database.meterDao()
    }

    @Provides
    @Singleton
    fun provideReadingDao(database: AppDatabase): ReadingDao {
        return database.readingDao()
    }

    @Provides
    @Singleton
    fun provideSerialManager(@ApplicationContext context: Context): MBusSerialManager {
        return MBusSerialManager(context)
    }
}
