package com.prosayac.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.prosayac.app.data.local.entity.MeterEntity;
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
public final class MeterDao_Impl implements MeterDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MeterEntity> __insertionAdapterOfMeterEntity;

  private final EntityDeletionOrUpdateAdapter<MeterEntity> __deletionAdapterOfMeterEntity;

  private final EntityDeletionOrUpdateAdapter<MeterEntity> __updateAdapterOfMeterEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateMeterReading;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public MeterDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMeterEntity = new EntityInsertionAdapter<MeterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `meters` (`id`,`serial_number`,`flat_number`,`meter_type`,`owner_name`,`address`,`building_name`,`status`,`last_reading`,`last_reading_date`,`is_synced`,`created_at`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSerialNumber());
        statement.bindString(3, entity.getFlatNumber());
        statement.bindString(4, entity.getMeterType());
        statement.bindString(5, entity.getOwnerName());
        statement.bindString(6, entity.getAddress());
        statement.bindString(7, entity.getBuildingName());
        statement.bindString(8, entity.getStatus());
        if (entity.getLastReading() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getLastReading());
        }
        if (entity.getLastReadingDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastReadingDate());
        }
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(11, _tmp);
        statement.bindLong(12, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfMeterEntity = new EntityDeletionOrUpdateAdapter<MeterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `meters` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMeterEntity = new EntityDeletionOrUpdateAdapter<MeterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `meters` SET `id` = ?,`serial_number` = ?,`flat_number` = ?,`meter_type` = ?,`owner_name` = ?,`address` = ?,`building_name` = ?,`status` = ?,`last_reading` = ?,`last_reading_date` = ?,`is_synced` = ?,`created_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MeterEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSerialNumber());
        statement.bindString(3, entity.getFlatNumber());
        statement.bindString(4, entity.getMeterType());
        statement.bindString(5, entity.getOwnerName());
        statement.bindString(6, entity.getAddress());
        statement.bindString(7, entity.getBuildingName());
        statement.bindString(8, entity.getStatus());
        if (entity.getLastReading() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getLastReading());
        }
        if (entity.getLastReadingDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getLastReadingDate());
        }
        final int _tmp = entity.isSynced() ? 1 : 0;
        statement.bindLong(11, _tmp);
        statement.bindLong(12, entity.getCreatedAt());
        statement.bindLong(13, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateMeterReading = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE meters SET status = ?, last_reading = ?, last_reading_date = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM meters";
        return _query;
      }
    };
  }

  @Override
  public Object insertMeter(final MeterEntity meter, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMeterEntity.insertAndReturnId(meter);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertMeters(final List<MeterEntity> meters,
      final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfMeterEntity.insertAndReturnIdsList(meters);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteMeter(final MeterEntity meter, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMeterEntity.handle(meter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMeter(final MeterEntity meter, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMeterEntity.handle(meter);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateMeterReading(final long id, final String status, final String reading,
      final long readingDate, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateMeterReading.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, reading);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, readingDate);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfUpdateMeterReading.release(_stmt);
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
  public Flow<List<MeterEntity>> getAllMeters() {
    final String _sql = "SELECT * FROM meters ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<List<MeterEntity>>() {
      @Override
      @NonNull
      public List<MeterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serial_number");
          final int _cursorIndexOfFlatNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "flat_number");
          final int _cursorIndexOfMeterType = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_type");
          final int _cursorIndexOfOwnerName = CursorUtil.getColumnIndexOrThrow(_cursor, "owner_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBuildingName = CursorUtil.getColumnIndexOrThrow(_cursor, "building_name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastReading = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading");
          final int _cursorIndexOfLastReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<MeterEntity> _result = new ArrayList<MeterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpOwnerName;
            _tmpOwnerName = _cursor.getString(_cursorIndexOfOwnerName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastReading;
            if (_cursor.isNull(_cursorIndexOfLastReading)) {
              _tmpLastReading = null;
            } else {
              _tmpLastReading = _cursor.getString(_cursorIndexOfLastReading);
            }
            final Long _tmpLastReadingDate;
            if (_cursor.isNull(_cursorIndexOfLastReadingDate)) {
              _tmpLastReadingDate = null;
            } else {
              _tmpLastReadingDate = _cursor.getLong(_cursorIndexOfLastReadingDate);
            }
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MeterEntity(_tmpId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpOwnerName,_tmpAddress,_tmpBuildingName,_tmpStatus,_tmpLastReading,_tmpLastReadingDate,_tmpIsSynced,_tmpCreatedAt);
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
  public Object getMeterById(final long id, final Continuation<? super MeterEntity> $completion) {
    final String _sql = "SELECT * FROM meters WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MeterEntity>() {
      @Override
      @Nullable
      public MeterEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serial_number");
          final int _cursorIndexOfFlatNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "flat_number");
          final int _cursorIndexOfMeterType = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_type");
          final int _cursorIndexOfOwnerName = CursorUtil.getColumnIndexOrThrow(_cursor, "owner_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBuildingName = CursorUtil.getColumnIndexOrThrow(_cursor, "building_name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastReading = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading");
          final int _cursorIndexOfLastReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final MeterEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpOwnerName;
            _tmpOwnerName = _cursor.getString(_cursorIndexOfOwnerName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastReading;
            if (_cursor.isNull(_cursorIndexOfLastReading)) {
              _tmpLastReading = null;
            } else {
              _tmpLastReading = _cursor.getString(_cursorIndexOfLastReading);
            }
            final Long _tmpLastReadingDate;
            if (_cursor.isNull(_cursorIndexOfLastReadingDate)) {
              _tmpLastReadingDate = null;
            } else {
              _tmpLastReadingDate = _cursor.getLong(_cursorIndexOfLastReadingDate);
            }
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new MeterEntity(_tmpId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpOwnerName,_tmpAddress,_tmpBuildingName,_tmpStatus,_tmpLastReading,_tmpLastReadingDate,_tmpIsSynced,_tmpCreatedAt);
          } else {
            _result = null;
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
  public Flow<List<MeterEntity>> getMetersByStatus(final String status) {
    final String _sql = "SELECT * FROM meters WHERE status = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<List<MeterEntity>>() {
      @Override
      @NonNull
      public List<MeterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serial_number");
          final int _cursorIndexOfFlatNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "flat_number");
          final int _cursorIndexOfMeterType = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_type");
          final int _cursorIndexOfOwnerName = CursorUtil.getColumnIndexOrThrow(_cursor, "owner_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBuildingName = CursorUtil.getColumnIndexOrThrow(_cursor, "building_name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastReading = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading");
          final int _cursorIndexOfLastReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<MeterEntity> _result = new ArrayList<MeterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpOwnerName;
            _tmpOwnerName = _cursor.getString(_cursorIndexOfOwnerName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastReading;
            if (_cursor.isNull(_cursorIndexOfLastReading)) {
              _tmpLastReading = null;
            } else {
              _tmpLastReading = _cursor.getString(_cursorIndexOfLastReading);
            }
            final Long _tmpLastReadingDate;
            if (_cursor.isNull(_cursorIndexOfLastReadingDate)) {
              _tmpLastReadingDate = null;
            } else {
              _tmpLastReadingDate = _cursor.getLong(_cursorIndexOfLastReadingDate);
            }
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MeterEntity(_tmpId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpOwnerName,_tmpAddress,_tmpBuildingName,_tmpStatus,_tmpLastReading,_tmpLastReadingDate,_tmpIsSynced,_tmpCreatedAt);
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
  public Flow<List<MeterEntity>> getMetersByType(final String type) {
    final String _sql = "SELECT * FROM meters WHERE meter_type = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<List<MeterEntity>>() {
      @Override
      @NonNull
      public List<MeterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serial_number");
          final int _cursorIndexOfFlatNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "flat_number");
          final int _cursorIndexOfMeterType = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_type");
          final int _cursorIndexOfOwnerName = CursorUtil.getColumnIndexOrThrow(_cursor, "owner_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBuildingName = CursorUtil.getColumnIndexOrThrow(_cursor, "building_name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastReading = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading");
          final int _cursorIndexOfLastReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<MeterEntity> _result = new ArrayList<MeterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpOwnerName;
            _tmpOwnerName = _cursor.getString(_cursorIndexOfOwnerName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastReading;
            if (_cursor.isNull(_cursorIndexOfLastReading)) {
              _tmpLastReading = null;
            } else {
              _tmpLastReading = _cursor.getString(_cursorIndexOfLastReading);
            }
            final Long _tmpLastReadingDate;
            if (_cursor.isNull(_cursorIndexOfLastReadingDate)) {
              _tmpLastReadingDate = null;
            } else {
              _tmpLastReadingDate = _cursor.getLong(_cursorIndexOfLastReadingDate);
            }
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MeterEntity(_tmpId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpOwnerName,_tmpAddress,_tmpBuildingName,_tmpStatus,_tmpLastReading,_tmpLastReadingDate,_tmpIsSynced,_tmpCreatedAt);
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
  public Flow<List<MeterEntity>> getMetersByTypeAndStatus(final String type, final String status) {
    final String _sql = "SELECT * FROM meters WHERE meter_type = ? AND status = ? ORDER BY created_at DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    _argIndex = 2;
    _statement.bindString(_argIndex, status);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<List<MeterEntity>>() {
      @Override
      @NonNull
      public List<MeterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSerialNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "serial_number");
          final int _cursorIndexOfFlatNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "flat_number");
          final int _cursorIndexOfMeterType = CursorUtil.getColumnIndexOrThrow(_cursor, "meter_type");
          final int _cursorIndexOfOwnerName = CursorUtil.getColumnIndexOrThrow(_cursor, "owner_name");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfBuildingName = CursorUtil.getColumnIndexOrThrow(_cursor, "building_name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfLastReading = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading");
          final int _cursorIndexOfLastReadingDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_reading_date");
          final int _cursorIndexOfIsSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "is_synced");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<MeterEntity> _result = new ArrayList<MeterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MeterEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSerialNumber;
            _tmpSerialNumber = _cursor.getString(_cursorIndexOfSerialNumber);
            final String _tmpFlatNumber;
            _tmpFlatNumber = _cursor.getString(_cursorIndexOfFlatNumber);
            final String _tmpMeterType;
            _tmpMeterType = _cursor.getString(_cursorIndexOfMeterType);
            final String _tmpOwnerName;
            _tmpOwnerName = _cursor.getString(_cursorIndexOfOwnerName);
            final String _tmpAddress;
            _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            final String _tmpBuildingName;
            _tmpBuildingName = _cursor.getString(_cursorIndexOfBuildingName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpLastReading;
            if (_cursor.isNull(_cursorIndexOfLastReading)) {
              _tmpLastReading = null;
            } else {
              _tmpLastReading = _cursor.getString(_cursorIndexOfLastReading);
            }
            final Long _tmpLastReadingDate;
            if (_cursor.isNull(_cursorIndexOfLastReadingDate)) {
              _tmpLastReadingDate = null;
            } else {
              _tmpLastReadingDate = _cursor.getLong(_cursorIndexOfLastReadingDate);
            }
            final boolean _tmpIsSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSynced);
            _tmpIsSynced = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MeterEntity(_tmpId,_tmpSerialNumber,_tmpFlatNumber,_tmpMeterType,_tmpOwnerName,_tmpAddress,_tmpBuildingName,_tmpStatus,_tmpLastReading,_tmpLastReadingDate,_tmpIsSynced,_tmpCreatedAt);
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
  public Flow<Integer> getTotalCount() {
    final String _sql = "SELECT COUNT(*) FROM meters";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<Integer>() {
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
  public Flow<Integer> getReadCount() {
    final String _sql = "SELECT COUNT(*) FROM meters WHERE status = 'Read'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<Integer>() {
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
  public Flow<Integer> getUnreadCount() {
    final String _sql = "SELECT COUNT(*) FROM meters WHERE status = 'Unread'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<Integer>() {
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
  public Flow<Integer> getUnsyncedCount() {
    final String _sql = "SELECT COUNT(*) FROM meters WHERE is_synced = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"meters"}, new Callable<Integer>() {
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
  public Object markAsSynced(final List<Long> ids, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE meters SET is_synced = 1 WHERE id IN (");
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
