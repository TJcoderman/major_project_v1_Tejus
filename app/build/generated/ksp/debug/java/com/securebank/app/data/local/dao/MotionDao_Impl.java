package com.securebank.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.securebank.app.data.model.MotionData;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class MotionDao_Impl implements MotionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MotionData> __insertionAdapterOfMotionData;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public MotionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMotionData = new EntityInsertionAdapter<MotionData>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `motion_data` (`id`,`sessionId`,`timestamp`,`accelX`,`accelY`,`accelZ`,`gyroX`,`gyroY`,`gyroZ`,`pitch`,`roll`,`azimuth`,`filteredAccelX`,`filteredAccelY`,`filteredAccelZ`,`deviceState`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MotionData entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindDouble(4, entity.getAccelX());
        statement.bindDouble(5, entity.getAccelY());
        statement.bindDouble(6, entity.getAccelZ());
        statement.bindDouble(7, entity.getGyroX());
        statement.bindDouble(8, entity.getGyroY());
        statement.bindDouble(9, entity.getGyroZ());
        statement.bindDouble(10, entity.getPitch());
        statement.bindDouble(11, entity.getRoll());
        statement.bindDouble(12, entity.getAzimuth());
        statement.bindDouble(13, entity.getFilteredAccelX());
        statement.bindDouble(14, entity.getFilteredAccelY());
        statement.bindDouble(15, entity.getFilteredAccelZ());
        statement.bindString(16, entity.getDeviceState());
      }
    };
    this.__preparedStmtOfDeleteBySession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM motion_data WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM motion_data WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MotionData motionData, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfMotionData.insertAndReturnId(motionData);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<MotionData> motionDataList,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMotionData.insert(motionDataList);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBySession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBySession.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, sessionId);
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
          __preparedStmtOfDeleteBySession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
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
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MotionData>> getBySession(final String sessionId) {
    final String _sql = "SELECT * FROM motion_data WHERE sessionId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"motion_data"}, new Callable<List<MotionData>>() {
      @Override
      @NonNull
      public List<MotionData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAccelX = CursorUtil.getColumnIndexOrThrow(_cursor, "accelX");
          final int _cursorIndexOfAccelY = CursorUtil.getColumnIndexOrThrow(_cursor, "accelY");
          final int _cursorIndexOfAccelZ = CursorUtil.getColumnIndexOrThrow(_cursor, "accelZ");
          final int _cursorIndexOfGyroX = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroX");
          final int _cursorIndexOfGyroY = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroY");
          final int _cursorIndexOfGyroZ = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroZ");
          final int _cursorIndexOfPitch = CursorUtil.getColumnIndexOrThrow(_cursor, "pitch");
          final int _cursorIndexOfRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "roll");
          final int _cursorIndexOfAzimuth = CursorUtil.getColumnIndexOrThrow(_cursor, "azimuth");
          final int _cursorIndexOfFilteredAccelX = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelX");
          final int _cursorIndexOfFilteredAccelY = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelY");
          final int _cursorIndexOfFilteredAccelZ = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelZ");
          final int _cursorIndexOfDeviceState = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceState");
          final List<MotionData> _result = new ArrayList<MotionData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MotionData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final float _tmpAccelX;
            _tmpAccelX = _cursor.getFloat(_cursorIndexOfAccelX);
            final float _tmpAccelY;
            _tmpAccelY = _cursor.getFloat(_cursorIndexOfAccelY);
            final float _tmpAccelZ;
            _tmpAccelZ = _cursor.getFloat(_cursorIndexOfAccelZ);
            final float _tmpGyroX;
            _tmpGyroX = _cursor.getFloat(_cursorIndexOfGyroX);
            final float _tmpGyroY;
            _tmpGyroY = _cursor.getFloat(_cursorIndexOfGyroY);
            final float _tmpGyroZ;
            _tmpGyroZ = _cursor.getFloat(_cursorIndexOfGyroZ);
            final float _tmpPitch;
            _tmpPitch = _cursor.getFloat(_cursorIndexOfPitch);
            final float _tmpRoll;
            _tmpRoll = _cursor.getFloat(_cursorIndexOfRoll);
            final float _tmpAzimuth;
            _tmpAzimuth = _cursor.getFloat(_cursorIndexOfAzimuth);
            final float _tmpFilteredAccelX;
            _tmpFilteredAccelX = _cursor.getFloat(_cursorIndexOfFilteredAccelX);
            final float _tmpFilteredAccelY;
            _tmpFilteredAccelY = _cursor.getFloat(_cursorIndexOfFilteredAccelY);
            final float _tmpFilteredAccelZ;
            _tmpFilteredAccelZ = _cursor.getFloat(_cursorIndexOfFilteredAccelZ);
            final String _tmpDeviceState;
            _tmpDeviceState = _cursor.getString(_cursorIndexOfDeviceState);
            _item = new MotionData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpAccelX,_tmpAccelY,_tmpAccelZ,_tmpGyroX,_tmpGyroY,_tmpGyroZ,_tmpPitch,_tmpRoll,_tmpAzimuth,_tmpFilteredAccelX,_tmpFilteredAccelY,_tmpFilteredAccelZ,_tmpDeviceState);
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
  public Object getRecentMotion(final String sessionId, final int limit,
      final Continuation<? super List<MotionData>> $completion) {
    final String _sql = "SELECT * FROM motion_data WHERE sessionId = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MotionData>>() {
      @Override
      @NonNull
      public List<MotionData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAccelX = CursorUtil.getColumnIndexOrThrow(_cursor, "accelX");
          final int _cursorIndexOfAccelY = CursorUtil.getColumnIndexOrThrow(_cursor, "accelY");
          final int _cursorIndexOfAccelZ = CursorUtil.getColumnIndexOrThrow(_cursor, "accelZ");
          final int _cursorIndexOfGyroX = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroX");
          final int _cursorIndexOfGyroY = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroY");
          final int _cursorIndexOfGyroZ = CursorUtil.getColumnIndexOrThrow(_cursor, "gyroZ");
          final int _cursorIndexOfPitch = CursorUtil.getColumnIndexOrThrow(_cursor, "pitch");
          final int _cursorIndexOfRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "roll");
          final int _cursorIndexOfAzimuth = CursorUtil.getColumnIndexOrThrow(_cursor, "azimuth");
          final int _cursorIndexOfFilteredAccelX = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelX");
          final int _cursorIndexOfFilteredAccelY = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelY");
          final int _cursorIndexOfFilteredAccelZ = CursorUtil.getColumnIndexOrThrow(_cursor, "filteredAccelZ");
          final int _cursorIndexOfDeviceState = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceState");
          final List<MotionData> _result = new ArrayList<MotionData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MotionData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final float _tmpAccelX;
            _tmpAccelX = _cursor.getFloat(_cursorIndexOfAccelX);
            final float _tmpAccelY;
            _tmpAccelY = _cursor.getFloat(_cursorIndexOfAccelY);
            final float _tmpAccelZ;
            _tmpAccelZ = _cursor.getFloat(_cursorIndexOfAccelZ);
            final float _tmpGyroX;
            _tmpGyroX = _cursor.getFloat(_cursorIndexOfGyroX);
            final float _tmpGyroY;
            _tmpGyroY = _cursor.getFloat(_cursorIndexOfGyroY);
            final float _tmpGyroZ;
            _tmpGyroZ = _cursor.getFloat(_cursorIndexOfGyroZ);
            final float _tmpPitch;
            _tmpPitch = _cursor.getFloat(_cursorIndexOfPitch);
            final float _tmpRoll;
            _tmpRoll = _cursor.getFloat(_cursorIndexOfRoll);
            final float _tmpAzimuth;
            _tmpAzimuth = _cursor.getFloat(_cursorIndexOfAzimuth);
            final float _tmpFilteredAccelX;
            _tmpFilteredAccelX = _cursor.getFloat(_cursorIndexOfFilteredAccelX);
            final float _tmpFilteredAccelY;
            _tmpFilteredAccelY = _cursor.getFloat(_cursorIndexOfFilteredAccelY);
            final float _tmpFilteredAccelZ;
            _tmpFilteredAccelZ = _cursor.getFloat(_cursorIndexOfFilteredAccelZ);
            final String _tmpDeviceState;
            _tmpDeviceState = _cursor.getString(_cursorIndexOfDeviceState);
            _item = new MotionData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpAccelX,_tmpAccelY,_tmpAccelZ,_tmpGyroX,_tmpGyroY,_tmpGyroZ,_tmpPitch,_tmpRoll,_tmpAzimuth,_tmpFilteredAccelX,_tmpFilteredAccelY,_tmpFilteredAccelZ,_tmpDeviceState);
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
  public Object getAvgPitch(final String sessionId, final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(pitch) FROM motion_data WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getFloat(0);
            }
            _result = _tmp;
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
  public Object getAvgRoll(final String sessionId, final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(roll) FROM motion_data WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getFloat(0);
            }
            _result = _tmp;
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
  public Object getMostCommonDeviceState(final String sessionId,
      final Continuation<? super String> $completion) {
    final String _sql = "\n"
            + "        SELECT deviceState \n"
            + "        FROM motion_data \n"
            + "        WHERE sessionId = ? \n"
            + "        GROUP BY deviceState \n"
            + "        ORDER BY COUNT(*) DESC \n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<String>() {
      @Override
      @Nullable
      public String call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final String _result;
          if (_cursor.moveToFirst()) {
            if (_cursor.isNull(0)) {
              _result = null;
            } else {
              _result = _cursor.getString(0);
            }
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
