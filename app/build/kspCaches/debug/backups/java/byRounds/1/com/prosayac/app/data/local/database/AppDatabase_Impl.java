package com.prosayac.app.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.prosayac.app.data.local.dao.MeterDao;
import com.prosayac.app.data.local.dao.MeterDao_Impl;
import com.prosayac.app.data.local.dao.ReadingDao;
import com.prosayac.app.data.local.dao.ReadingDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile MeterDao _meterDao;

  private volatile ReadingDao _readingDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `meters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serial_number` TEXT NOT NULL, `flat_number` TEXT NOT NULL, `meter_type` TEXT NOT NULL, `owner_name` TEXT NOT NULL, `address` TEXT NOT NULL, `building_name` TEXT NOT NULL, `status` TEXT NOT NULL, `last_reading` TEXT, `last_reading_date` INTEGER, `is_synced` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `readings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `meter_id` INTEGER NOT NULL, `reading_value` TEXT NOT NULL, `reading_date` INTEGER NOT NULL, `is_synced` INTEGER NOT NULL, `reading_type` TEXT NOT NULL, `notes` TEXT, FOREIGN KEY(`meter_id`) REFERENCES `meters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_readings_meter_id` ON `readings` (`meter_id`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f42e9af31ca28ded9aea9533668fab65')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `meters`");
        db.execSQL("DROP TABLE IF EXISTS `readings`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsMeters = new HashMap<String, TableInfo.Column>(12);
        _columnsMeters.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("serial_number", new TableInfo.Column("serial_number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("flat_number", new TableInfo.Column("flat_number", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("meter_type", new TableInfo.Column("meter_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("owner_name", new TableInfo.Column("owner_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("building_name", new TableInfo.Column("building_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("last_reading", new TableInfo.Column("last_reading", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("last_reading_date", new TableInfo.Column("last_reading_date", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("is_synced", new TableInfo.Column("is_synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMeters.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMeters = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMeters = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMeters = new TableInfo("meters", _columnsMeters, _foreignKeysMeters, _indicesMeters);
        final TableInfo _existingMeters = TableInfo.read(db, "meters");
        if (!_infoMeters.equals(_existingMeters)) {
          return new RoomOpenHelper.ValidationResult(false, "meters(com.prosayac.app.data.local.entity.MeterEntity).\n"
                  + " Expected:\n" + _infoMeters + "\n"
                  + " Found:\n" + _existingMeters);
        }
        final HashMap<String, TableInfo.Column> _columnsReadings = new HashMap<String, TableInfo.Column>(7);
        _columnsReadings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("meter_id", new TableInfo.Column("meter_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("reading_value", new TableInfo.Column("reading_value", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("reading_date", new TableInfo.Column("reading_date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("is_synced", new TableInfo.Column("is_synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("reading_type", new TableInfo.Column("reading_type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadings.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReadings = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysReadings.add(new TableInfo.ForeignKey("meters", "CASCADE", "NO ACTION", Arrays.asList("meter_id"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesReadings = new HashSet<TableInfo.Index>(1);
        _indicesReadings.add(new TableInfo.Index("index_readings_meter_id", false, Arrays.asList("meter_id"), Arrays.asList("ASC")));
        final TableInfo _infoReadings = new TableInfo("readings", _columnsReadings, _foreignKeysReadings, _indicesReadings);
        final TableInfo _existingReadings = TableInfo.read(db, "readings");
        if (!_infoReadings.equals(_existingReadings)) {
          return new RoomOpenHelper.ValidationResult(false, "readings(com.prosayac.app.data.local.entity.ReadingEntity).\n"
                  + " Expected:\n" + _infoReadings + "\n"
                  + " Found:\n" + _existingReadings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f42e9af31ca28ded9aea9533668fab65", "b091e769f33641340979cb8ee61d02be");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "meters","readings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `meters`");
      _db.execSQL("DELETE FROM `readings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(MeterDao.class, MeterDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ReadingDao.class, ReadingDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public MeterDao meterDao() {
    if (_meterDao != null) {
      return _meterDao;
    } else {
      synchronized(this) {
        if(_meterDao == null) {
          _meterDao = new MeterDao_Impl(this);
        }
        return _meterDao;
      }
    }
  }

  @Override
  public ReadingDao readingDao() {
    if (_readingDao != null) {
      return _readingDao;
    } else {
      synchronized(this) {
        if(_readingDao == null) {
          _readingDao = new ReadingDao_Impl(this);
        }
        return _readingDao;
      }
    }
  }
}
