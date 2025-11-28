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
import com.securebank.app.data.model.KeystrokeData;
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
public final class KeystrokeDao_Impl implements KeystrokeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<KeystrokeData> __insertionAdapterOfKeystrokeData;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBySession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public KeystrokeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfKeystrokeData = new EntityInsertionAdapter<KeystrokeData>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `keystroke_data` (`id`,`sessionId`,`timestamp`,`keyCode`,`dwellTime`,`flightTime`,`isLoginBaseline`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final KeystrokeData entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getSessionId());
        statement.bindLong(3, entity.getTimestamp());
        statement.bindLong(4, entity.getKeyCode());
        statement.bindLong(5, entity.getDwellTime());
        statement.bindLong(6, entity.getFlightTime());
        final int _tmp = entity.isLoginBaseline() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__preparedStmtOfDeleteBySession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM keystroke_data WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM keystroke_data WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final KeystrokeData keystrokeData,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfKeystrokeData.insertAndReturnId(keystrokeData);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<KeystrokeData> keystrokeDataList,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfKeystrokeData.insert(keystrokeDataList);
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
  public Flow<List<KeystrokeData>> getBySession(final String sessionId) {
    final String _sql = "SELECT * FROM keystroke_data WHERE sessionId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"keystroke_data"}, new Callable<List<KeystrokeData>>() {
      @Override
      @NonNull
      public List<KeystrokeData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfKeyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "keyCode");
          final int _cursorIndexOfDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dwellTime");
          final int _cursorIndexOfFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "flightTime");
          final int _cursorIndexOfIsLoginBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isLoginBaseline");
          final List<KeystrokeData> _result = new ArrayList<KeystrokeData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KeystrokeData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpKeyCode;
            _tmpKeyCode = _cursor.getInt(_cursorIndexOfKeyCode);
            final long _tmpDwellTime;
            _tmpDwellTime = _cursor.getLong(_cursorIndexOfDwellTime);
            final long _tmpFlightTime;
            _tmpFlightTime = _cursor.getLong(_cursorIndexOfFlightTime);
            final boolean _tmpIsLoginBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLoginBaseline);
            _tmpIsLoginBaseline = _tmp != 0;
            _item = new KeystrokeData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpKeyCode,_tmpDwellTime,_tmpFlightTime,_tmpIsLoginBaseline);
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
  public Object getBaselineKeystrokes(final String sessionId,
      final Continuation<? super List<KeystrokeData>> $completion) {
    final String _sql = "SELECT * FROM keystroke_data WHERE sessionId = ? AND isLoginBaseline = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<KeystrokeData>>() {
      @Override
      @NonNull
      public List<KeystrokeData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfKeyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "keyCode");
          final int _cursorIndexOfDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dwellTime");
          final int _cursorIndexOfFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "flightTime");
          final int _cursorIndexOfIsLoginBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isLoginBaseline");
          final List<KeystrokeData> _result = new ArrayList<KeystrokeData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KeystrokeData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpKeyCode;
            _tmpKeyCode = _cursor.getInt(_cursorIndexOfKeyCode);
            final long _tmpDwellTime;
            _tmpDwellTime = _cursor.getLong(_cursorIndexOfDwellTime);
            final long _tmpFlightTime;
            _tmpFlightTime = _cursor.getLong(_cursorIndexOfFlightTime);
            final boolean _tmpIsLoginBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLoginBaseline);
            _tmpIsLoginBaseline = _tmp != 0;
            _item = new KeystrokeData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpKeyCode,_tmpDwellTime,_tmpFlightTime,_tmpIsLoginBaseline);
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
  public Object getSessionKeystrokes(final String sessionId,
      final Continuation<? super List<KeystrokeData>> $completion) {
    final String _sql = "SELECT * FROM keystroke_data WHERE sessionId = ? AND isLoginBaseline = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<KeystrokeData>>() {
      @Override
      @NonNull
      public List<KeystrokeData> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfKeyCode = CursorUtil.getColumnIndexOrThrow(_cursor, "keyCode");
          final int _cursorIndexOfDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "dwellTime");
          final int _cursorIndexOfFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "flightTime");
          final int _cursorIndexOfIsLoginBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isLoginBaseline");
          final List<KeystrokeData> _result = new ArrayList<KeystrokeData>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final KeystrokeData _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final int _tmpKeyCode;
            _tmpKeyCode = _cursor.getInt(_cursorIndexOfKeyCode);
            final long _tmpDwellTime;
            _tmpDwellTime = _cursor.getLong(_cursorIndexOfDwellTime);
            final long _tmpFlightTime;
            _tmpFlightTime = _cursor.getLong(_cursorIndexOfFlightTime);
            final boolean _tmpIsLoginBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsLoginBaseline);
            _tmpIsLoginBaseline = _tmp != 0;
            _item = new KeystrokeData(_tmpId,_tmpSessionId,_tmpTimestamp,_tmpKeyCode,_tmpDwellTime,_tmpFlightTime,_tmpIsLoginBaseline);
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
  public Object getAvgDwellTime(final String sessionId, final boolean isBaseline,
      final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(dwellTime) FROM keystroke_data WHERE sessionId = ? AND isLoginBaseline = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    final int _tmp = isBaseline ? 1 : 0;
    _statement.bindLong(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp_1;
            if (_cursor.isNull(0)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getFloat(0);
            }
            _result = _tmp_1;
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
  public Object getAvgFlightTime(final String sessionId, final boolean isBaseline,
      final Continuation<? super Float> $completion) {
    final String _sql = "SELECT AVG(flightTime) FROM keystroke_data WHERE sessionId = ? AND isLoginBaseline = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    _argIndex = 2;
    final int _tmp = isBaseline ? 1 : 0;
    _statement.bindLong(_argIndex, _tmp);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Float>() {
      @Override
      @Nullable
      public Float call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Float _result;
          if (_cursor.moveToFirst()) {
            final Float _tmp_1;
            if (_cursor.isNull(0)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getFloat(0);
            }
            _result = _tmp_1;
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
