package com.prosayac.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.prosayac.app.data.local.entity.ReadingEntity;
import com.prosayac.app.domain.model.DailyStats;
import com.prosayac.app.domain.model.MonthlyStats;
import com.prosayac.app.domain.model.ReadingWithMeter;
import com.prosayac.app.domain.model.TypeStats;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ReadingDao_Impl implements ReadingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ReadingEntity> __insertionAdapterOfReadingEntity;

  private final EntityDeletionOrUpdateAdapter<ReadingEntity> __deletionAdapterOfReadingEntity;

  private final EntityDeletionOrUpdateAdapter<ReadingEntity> __updateAdapterOfReadingEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ReadingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfReadingEntity = new EntityInsertionAdapter<ReadingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `readings` (`id`,`meter_id`,`reading_value`,`reading_date`,`is_synced`,`reading_type`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMeterId());
        statement.bindString(3, entity.getReadingValue());
        statement.bindLong(4, entity.getReadingDate());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getReadingType());
        if (entity.getNotes() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfReadingEntity = new EntityDeletionOrUpdateAdapter<ReadingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `readings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfReadingEntity = new EntityDeletionOrUpdateAdapter<ReadingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `readings` SET `id` = ?,`meter_id` = ?,`reading_value` = ?,`reading_date` = ?,`is_synced` = ?,`reading_type` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getMeterId());
        statement.bindString(3, entity.getReadingValue());
        statement.bindLong(4, entity.getReadingDate());
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindString(6, entity.getReadingType());
        if (entity.getNotes() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getNotes());
        }
        statement.bindLong(8, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM readings";
        return _query;
      }
    };
  }

  @Override
  public Object insertReading(final ReadingEntity reading,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfReadingEntity.insertAndReturnId(reading);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertReadings(final List<ReadingEntity> readings,
      final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfReadingEntity.insertAndReturnIdsList(readings);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteReading(final ReadingEntity reading,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfReadingEntity.handle(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateReading(final ReadingEntity reading,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfReadingEntity.handle(reading);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ReadingEntity>> getAllReadings() {
    final String _sql = "SELECT * FROM readings ORDER BY reading_date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<List<ReadingEntity>>() {
      @Override
      @NonNull
      public List<ReadingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMeterId = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_id");
          final int _cursorIndexOfReadingValue = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_value");
          final int _cursorIndexOfReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfReadingType = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_type");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<ReadingEntity> _result = new ArrayList<ReadingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMeterId;
            _tmpMeterId = _cursor.getLong(_cursorIndexOfMeterId);
            final String _tmpReadingValue;
            _tmpReadingValue = _cursor.getString(_cursorIndexOfReadingValue);
            final long _tmpReadingDate;
            _tmpReadingDate = _cursor.getLong(_cursorIndexOfReadingDate);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpReadingType;
            _tmpReadingType = _cursor.getString(_cursorIndexOfReadingType);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new ReadingEntity(_tmpId,_tmpMeterId,_tmpReadingValue,_tmpReadingDate,_tmpIsSynced,_tmpReadingType,_tmpNotes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ReadingEntity>> getReadingsByMeterId(final long meterId) {
    final String _sql = "SELECT * FROM readings WHERE meter_id = ? ORDER BY reading_date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, meterId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<List<ReadingEntity>>() {
      @Override
      @NonNull
      public List<ReadingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMeterId = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_id");
          final int _cursorIndexOfReadingValue = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_value");
          final int _cursorIndexOfReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfReadingType = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_type");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<ReadingEntity> _result = new ArrayList<ReadingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMeterId;
            _tmpMeterId = _cursor.getLong(_cursorIndexOfMeterId);
            final String _tmpReadingValue;
            _tmpReadingValue = _cursor.getString(_cursorIndexOfReadingValue);
            final long _tmpReadingDate;
            _tmpReadingDate = _cursor.getLong(_cursorIndexOfReadingDate);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpReadingType;
            _tmpReadingType = _cursor.getString(_cursorIndexOfReadingType);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new ReadingEntity(_tmpId,_tmpMeterId,_tmpReadingValue,_tmpReadingDate,_tmpIsSynced,_tmpReadingType,_tmpNotes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ReadingEntity>> getReadingsByDateRange(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM readings WHERE reading_date BETWEEN ? AND ? ORDER BY reading_date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<List<ReadingEntity>>() {
      @Override
      @NonNull
      public List<ReadingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfMeterId = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_id");
          final int _cursorIndexOfReadingValue = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_value");
          final int _cursorIndexOfReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfReadingType = CursorUtil.getColumnIndexOrThrow(_cursor, "reading_type");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<ReadingEntity> _result = new ArrayList<ReadingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMeterId;
            _tmpMeterId = _cursor.getLong(_cursorIndexOfMeterId);
            final String _tmpReadingValue;
            _tmpReadingValue = _cursor.getString(_cursorIndexOfReadingValue);
            final long _tmpReadingDate;
            _tmpReadingDate = _cursor.getLong(_cursorIndexOfReadingDate);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpReadingType;
            _tmpReadingType = _cursor.getString(_cursorIndexOfReadingType);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new ReadingEntity(_tmpId,_tmpMeterId,_tmpReadingValue,_tmpReadingDate,_tmpIsSynced,_tmpReadingType,_tmpNotes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ReadingWithMeter>> getAllReadingsWithMeter() {
    final String _sql = "\n"
            + "        SELECT r.id, r.meter_id AS meterId, m.serial_number AS serialNumber,\n"
            + "               m.flat_number AS flatNumber, m.meter_type AS meterType,\n"
            + "               m.building_name AS buildingName,\n"
            + "               r.reading_value AS readingValue, r.reading_date AS readingDate,\n"
            + "               r.is_synced AS isSynced, r.reading_type AS readingType,\n"
            + "               r.notes, m.status AS meterStatus\n"
            + "        FROM readings r\n"
            + "        INNER JOIN meters m ON r.meter_id = m.id\n"
            + "        ORDER BY r.reading_date DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings",
        "meters"}, new Callable<List<ReadingWithMeter>>() {
      @Override
      @NonNull
      public List<ReadingWithMeter> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfMeterId = 1;
          final int _cursorIndexOfSerialNumber = 2;
          final int _cursorIndexOfFlatNumber = 3;
          final int _cursorIndexOfMeterType = 4;
          final int _cursorIndexOfBuildingName = 5;
          final int _cursorIndexOfReadingValue = 6;
          final int _cursorIndexOfReadingDate = 7;
          final int _cursorIndexOfIsSynced = 8;
          final int _cursorIndexOfReadingType = 9;
          final int _cursorIndexOfNotes = 10;
          final int _cursorIndexOfMeterStatus = 11;
          final List<ReadingWithMeter> _result = new ArrayList<ReadingWithMeter>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingWithMeter _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpMeterId;
            _tmpMeterId = _cursor.getLong(_cursorIndexOfMeterId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpReadingValue;
            _tmpReadingValue = _cursor.getString(_cursorIndexOfReadingValue);
            final long _tmpReadingDate;
            _tmpReadingDate = _cursor.getLong(_cursorIndexOfReadingDate);
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final String _tmpReadingType;
            _tmpReadingType = _cursor.getString(_cursorIndexOfReadingType);
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            final String _tmpMeterStatus;
            _tmpMeterStatus = _cursor.getString(_cursorIndexOfMeterStatus);
            _item = new ReadingWithMeter(_tmpId,_tmpMeterId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpBuildingName,_tmpReadingValue,_tmpReadingDate,_tmpIsSynced,_tmpReadingType,_tmpNotes,_tmpMeterStatus);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getTotalReadingCount() {
    final String _sql = "SELECT COUNT(*) FROM readings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getUnsyncedReadingCount() {
    final String _sql = "SELECT COUNT(*) FROM readings WHERE is_synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"readings"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getReadingCountSince(final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM readings WHERE reading_date >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDailyReadingStats(final long since,
      final Continuation<? super List<DailyStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT strftime('%Y-%m-%d', reading_date / 1000, 'unixepoch') as day, \n"
            + "               COUNT(*) as count \n"
            + "        FROM readings \n"
            + "        WHERE reading_date >= ? \n"
            + "        GROUP BY day \n"
            + "        ORDER BY day ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyStats>>() {
      @Override
      @NonNull
      public List<DailyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDay = 0;
          final int _cursorIndexOfCount = 1;
          final List<DailyStats> _result = new ArrayList<DailyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyStats _item;
            final String _tmpDay;
            _tmpDay = _cursor.getString(_cursorIndexOfDay);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new DailyStats(_tmpDay,_tmpCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getReadingTypeDistribution(
      final Continuation<? super List<TypeStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT reading_type, COUNT(*) as count \n"
            + "        FROM readings \n"
            + "        GROUP BY reading_type\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TypeStats>>() {
      @Override
      @NonNull
      public List<TypeStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfReadingType = 0;
          final int _cursorIndexOfCount = 1;
          final List<TypeStats> _result = new ArrayList<TypeStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TypeStats _item;
            final String _tmpReading_type;
            _tmpReading_type = _cursor.getString(_cursorIndexOfReadingType);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new TypeStats(_tmpReading_type,_tmpCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getMonthlyReadingTrend(final Continuation<? super List<MonthlyStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT strftime('%Y-%m', reading_date / 1000, 'unixepoch') as month, \n"
            + "               COUNT(*) as count \n"
            + "        FROM readings \n"
            + "        GROUP BY month \n"
            + "        ORDER BY month ASC \n"
            + "        LIMIT 12\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MonthlyStats>>() {
      @Override
      @NonNull
      public List<MonthlyStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMonth = 0;
          final int _cursorIndexOfCount = 1;
          final List<MonthlyStats> _result = new ArrayList<MonthlyStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MonthlyStats _item;
            final String _tmpMonth;
            _tmpMonth = _cursor.getString(_cursorIndexOfMonth);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new MonthlyStats(_tmpMonth,_tmpCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsSynced(final List<Long> ids, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE readings SET is_synced = 1 WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
