package com.prosayac.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prosayac.app.data.local.database.AppDatabase
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.data.local.dao.SiteDao
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

    // Migration from version 1 to 4 (comprehensive schema recreation)
    private val MIGRATION_1_4 = object : Migration(1, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Recreate meters table with complete schema
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS meters_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    serial_number TEXT NOT NULL,
                    flat_number TEXT NOT NULL DEFAULT '',
                    meter_type TEXT NOT NULL,
                    owner_name TEXT NOT NULL DEFAULT '',
                    address TEXT NOT NULL DEFAULT '',
                    building_name TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'Unread',
                    last_reading TEXT,
                    last_reading_date INTEGER,
                    is_synced INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Copy data if old table exists
            database.execSQL("""
                INSERT INTO meters_new (id, serial_number, flat_number, meter_type, owner_name, address, building_name, status, last_reading, last_reading_date, is_synced, created_at)
                SELECT id, serial_number,
                       COALESCE(flat_number, ''),
                       meter_type,
                       COALESCE(owner_name, ''),
                       COALESCE(address, ''),
                       COALESCE(building_name, ''),
                       COALESCE(status, 'Unread'),
                       last_reading,
                       last_reading_date,
                       COALESCE(is_synced, 0),
                       COALESCE(created_at, 0)
                FROM meters
            """.trimIndent())

            database.execSQL("DROP TABLE meters")
            database.execSQL("ALTER TABLE meters_new RENAME TO meters")

            // Recreate readings table with foreign key
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS readings_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    meter_id INTEGER NOT NULL,
                    reading_value TEXT NOT NULL,
                    reading_date INTEGER NOT NULL,
                    is_synced INTEGER NOT NULL DEFAULT 0,
                    reading_type TEXT NOT NULL DEFAULT 'manual',
                    notes TEXT,
                    FOREIGN KEY (meter_id) REFERENCES meters(id) ON DELETE CASCADE
                )
            """.trimIndent())

            database.execSQL("CREATE INDEX IF NOT EXISTS index_readings_meter_id ON readings_new(meter_id)")

            // Copy data if old table exists
            database.execSQL("""
                INSERT INTO readings_new (id, meter_id, reading_value, reading_date, is_synced, reading_type, notes)
                SELECT id, meter_id, reading_value, reading_date,
                       COALESCE(is_synced, 0),
                       COALESCE(reading_type, 'manual'),
                       notes
                FROM readings
            """.trimIndent())

            database.execSQL("DROP TABLE readings")
            database.execSQL("ALTER TABLE readings_new RENAME TO readings")
        }
    }

    // Migration from version 2 to 4 (no schema changes, empty migration)
    private val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema changes between v2 and v4
        }
    }

    // Migration from version 3 to 4 (no schema changes, empty migration)
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No schema changes between v3 and v4
        }
    }

    // Migration from version 4 to 5 (add sites table)
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS sites (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    address TEXT
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "prosayac_database"
        )
            .addMigrations(MIGRATION_1_4, MIGRATION_2_4, MIGRATION_3_4, MIGRATION_4_5)
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
    fun provideSiteDao(database: AppDatabase): SiteDao {
        return database.siteDao()
    }

    @Provides
    @Singleton
    fun provideSerialManager(@ApplicationContext context: Context): MBusSerialManager {
        return MBusSerialManager(context)
    }
}
