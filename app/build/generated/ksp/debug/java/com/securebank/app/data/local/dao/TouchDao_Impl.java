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
import com.securebank.app.data.model.TouchData;
import com.securebank.app.data.model.TouchType;
import com.securebank.app.data.model.TouchTypeConverter;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Integer;
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
public final class TouchDao_Impl implements TouchDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<TouchData> __insertionAdapterOfTouchData;

  private final TouchTypeConverter __touchTypeConverter = new TouchTypeConverter();

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public TouchDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTouchData = new EntityInsertionAdapter<TouchData>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `touch_data` (`id`,`sessionId`,`timestamp`,`touchType`,`startX`,`startY`,`endX`,`endY`,`pressure`,`touchSize`,`duration`,`velocity`,`acceleration`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final TouchData entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindLong(3, entity.getTimestamp());
        final String _tmp = __touchTypeConverter.fromTouchType(entity.getTouchType());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getStartX());
        statement.bindDouble(6, entity.getStartY());
        statement.bindDouble(7, entity.getEndX());
        statement.bindDouble(8, entity.getEndY());
        statement.bindDouble(9, entity.getPressure());
        statement.bindDouble(10, entity.getTouchSize());
        statement.bindLong(11, entity.getDuration());
        statement.bindDouble(12, entity.getVelocity());
        statement.bindDouble(13, entity.getAcceleration());
      }
    };
    this.__preparedStmtOfDeleteBySession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM touch_data WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM touch_data WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final TouchData touchData, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTouchData.insertAndReturnId(touchData);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<TouchData> touchDataList,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTouchData.insert(touchDataList);
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
  public Flow<List<TouchData>> getBySession(final String sessionId) {
    final String _sql = "SELECT * FROM touch_data WHERE sessionId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"touch_data"}, new Callable<List<TouchData>>() {
      @Override
      @NonNull
      public List<TouchData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTouchType = CursorUtil.getColumnIndexOrThrow(_cursor, "touchType");
          final int _cursorIndexOfStartX = CursorUtil.getColumnIndexOrThrow(_cursor, "startX");
          final int _cursorIndexOfStartY = CursorUtil.getColumnIndexOrThrow(_cursor, "startY");
          final int _cursorIndexOfEndX = CursorUtil.getColumnIndexOrThrow(_cursor, "endX");
          final int _cursorIndexOfEndY = CursorUtil.getColumnIndexOrThrow(_cursor, "endY");
          final int _cursorIndexOfPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "pressure");
          final int _cursorIndexOfTouchSize = CursorUtil.getColumnIndexOrThrow(_cursor, "touchSize");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "velocity");
          final int _cursorIndexOfAcceleration = CursorUtil.getColumnIndexOrThrow(_cursor, "acceleration");
          final List<TouchData> _result = new ArrayList<TouchData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TouchData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final TouchType _tmpTouchType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTouchType);
            _tmpTouchType = __touchTypeConverter.toTouchType(_tmp);
            final float _tmpStartX;
            _tmpStartX = _cursor.getFloat(_cursorIndexOfStartX);
            final float _tmpStartY;
            _tmpStartY = _cursor.getFloat(_cursorIndexOfStartY);
            final float _tmpEndX;
            _tmpEndX = _cursor.getFloat(_cursorIndexOfEndX);
            final float _tmpEndY;
            _tmpEndY = _cursor.getFloat(_cursorIndexOfEndY);
            final float _tmpPressure;
            _tmpPressure = _cursor.getFloat(_cursorIndexOfPressure);
            final float _tmpTouchSize;
            _tmpTouchSize = _cursor.getFloat(_cursorIndexOfTouchSize);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final float _tmpVelocity;
            _tmpVelocity = _cursor.getFloat(_cursorIndexOfVelocity);
            final float _tmpAcceleration;
            _tmpAcceleration = _cursor.getFloat(_cursorIndexOfAcceleration);
            _item = new TouchData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpTouchType,_tmpStartX,_tmpStartY,_tmpEndX,_tmpEndY,_tmpPressure,_tmpTouchSize,_tmpDuration,_tmpVelocity,_tmpAcceleration);
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
  public Object getRecentTouches(final String sessionId, final int limit,
      final Continuation<? super List<TouchData>> $completion) {
    final String _sql = "SELECT * FROM touch_data WHERE sessionId = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TouchData>>() {
      @Override
      @NonNull
      public List<TouchData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfTouchType = CursorUtil.getColumnIndexOrThrow(_cursor, "touchType");
          final int _cursorIndexOfStartX = CursorUtil.getColumnIndexOrThrow(_cursor, "startX");
          final int _cursorIndexOfStartY = CursorUtil.getColumnIndexOrThrow(_cursor, "startY");
          final int _cursorIndexOfEndX = CursorUtil.getColumnIndexOrThrow(_cursor, "endX");
          final int _cursorIndexOfEndY = CursorUtil.getColumnIndexOrThrow(_cursor, "endY");
          final int _cursorIndexOfPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "pressure");
          final int _cursorIndexOfTouchSize = CursorUtil.getColumnIndexOrThrow(_cursor, "touchSize");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "velocity");
          final int _cursorIndexOfAcceleration = CursorUtil.getColumnIndexOrThrow(_cursor, "acceleration");
          final List<TouchData> _result = new ArrayList<TouchData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TouchData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final TouchType _tmpTouchType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTouchType);
            _tmpTouchType = __touchTypeConverter.toTouchType(_tmp);
            final float _tmpStartX;
            _tmpStartX = _cursor.getFloat(_cursorIndexOfStartX);
            final float _tmpStartY;
            _tmpStartY = _cursor.getFloat(_cursorIndexOfStartY);
            final float _tmpEndX;
            _tmpEndX = _cursor.getFloat(_cursorIndexOfEndX);
            final float _tmpEndY;
            _tmpEndY = _cursor.getFloat(_cursorIndexOfEndY);
            final float _tmpPressure;
            _tmpPressure = _cursor.getFloat(_cursorIndexOfPressure);
            final float _tmpTouchSize;
            _tmpTouchSize = _cursor.getFloat(_cursorIndexOfTouchSize);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final float _tmpVelocity;
            _tmpVelocity = _cursor.getFloat(_cursorIndexOfVelocity);
            final float _tmpAcceleration;
            _tmpAcceleration = _cursor.getFloat(_cursorIndexOfAcceleration);
            _item = new TouchData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpTouchType,_tmpStartX,_tmpStartY,_tmpEndX,_tmpEndY,_tmpPressure,_tmpTouchSize,_tmpDuration,_tmpVelocity,_tmpAcceleration);
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
  public Object getAvgPressure(final String sessionId,
      final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(pressure) FROM touch_data WHERE sessionId = ?";
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
  public Object getAvgSwipeVelocity(final String sessionId,
      final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(velocity) FROM touch_data WHERE sessionId = ? AND touchType IN ('SWIPE_UP', 'SWIPE_DOWN', 'SWIPE_LEFT', 'SWIPE_RIGHT')";
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
  public Object countByType(final String sessionId, final TouchType touchType,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM touch_data WHERE sessionId = ? AND touchType = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    final String _tmp = __touchTypeConverter.fromTouchType(touchType);
    _statement.bindString(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(0);
            _result = _tmp_1;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
