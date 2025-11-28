package com.securebank.app.data.local.dao;

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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.securebank.app.data.model.BehavioralSession;
import java.lang.Class;
import java.lang.Exception;
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
public final class BehavioralSessionDao_Impl implements BehavioralSessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BehavioralSession> __insertionAdapterOfBehavioralSession;

  private final EntityDeletionOrUpdateAdapter<BehavioralSession> __updateAdapterOfBehavioralSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldSessions;

  public BehavioralSessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBehavioralSession = new EntityInsertionAdapter<BehavioralSession>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `behavioral_sessions` (`sessionId`,`userId`,`startTime`,`endTime`,`isBaseline`,`avgKeystrokeDwellTime`,`avgKeystrokeFlightTime`,`avgTouchPressure`,`avgSwipeVelocity`,`avgDevicePitch`,`avgDeviceRoll`,`stdKeystrokeDwellTime`,`stdKeystrokeFlightTime`,`stdTouchPressure`,`currentRiskScore`,`riskAlertCount`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BehavioralSession entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getUserId());
        statement.bindLong(3, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getEndTime());
        }
        final int _tmp = entity.isBaseline() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindDouble(6, entity.getAvgKeystrokeDwellTime());
        statement.bindDouble(7, entity.getAvgKeystrokeFlightTime());
        statement.bindDouble(8, entity.getAvgTouchPressure());
        statement.bindDouble(9, entity.getAvgSwipeVelocity());
        statement.bindDouble(10, entity.getAvgDevicePitch());
        statement.bindDouble(11, entity.getAvgDeviceRoll());
        statement.bindDouble(12, entity.getStdKeystrokeDwellTime());
        statement.bindDouble(13, entity.getStdKeystrokeFlightTime());
        statement.bindDouble(14, entity.getStdTouchPressure());
        statement.bindDouble(15, entity.getCurrentRiskScore());
        statement.bindLong(16, entity.getRiskAlertCount());
      }
    };
    this.__updateAdapterOfBehavioralSession = new EntityDeletionOrUpdateAdapter<BehavioralSession>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `behavioral_sessions` SET `sessionId` = ?,`userId` = ?,`startTime` = ?,`endTime` = ?,`isBaseline` = ?,`avgKeystrokeDwellTime` = ?,`avgKeystrokeFlightTime` = ?,`avgTouchPressure` = ?,`avgSwipeVelocity` = ?,`avgDevicePitch` = ?,`avgDeviceRoll` = ?,`stdKeystrokeDwellTime` = ?,`stdKeystrokeFlightTime` = ?,`stdTouchPressure` = ?,`currentRiskScore` = ?,`riskAlertCount` = ? WHERE `sessionId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BehavioralSession entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getUserId());
        statement.bindLong(3, entity.getStartTime());
        if (entity.getEndTime() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getEndTime());
        }
        final int _tmp = entity.isBaseline() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindDouble(6, entity.getAvgKeystrokeDwellTime());
        statement.bindDouble(7, entity.getAvgKeystrokeFlightTime());
        statement.bindDouble(8, entity.getAvgTouchPressure());
        statement.bindDouble(9, entity.getAvgSwipeVelocity());
        statement.bindDouble(10, entity.getAvgDevicePitch());
        statement.bindDouble(11, entity.getAvgDeviceRoll());
        statement.bindDouble(12, entity.getStdKeystrokeDwellTime());
        statement.bindDouble(13, entity.getStdKeystrokeFlightTime());
        statement.bindDouble(14, entity.getStdTouchPressure());
        statement.bindDouble(15, entity.getCurrentRiskScore());
        statement.bindLong(16, entity.getRiskAlertCount());
        statement.bindString(17, entity.getSessionId());
      }
    };
    this.__preparedStmtOfDeleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM behavioral_sessions WHERE sessionId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldSessions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM behavioral_sessions WHERE endTime IS NOT NULL AND endTime < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final BehavioralSession session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBehavioralSession.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final BehavioralSession session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBehavioralSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final String sessionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSession.acquire();
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
          __preparedStmtOfDeleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldSessions(final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldSessions.acquire();
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
          __preparedStmtOfDeleteOldSessions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getBySessionId(final String sessionId,
      final Continuation<? super BehavioralSession> $completion) {
    final String _sql = "SELECT * FROM behavioral_sessions WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BehavioralSession>() {
      @Override
      @Nullable
      public BehavioralSession call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfIsBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isBaseline");
          final int _cursorIndexOfAvgKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeDwellTime");
          final int _cursorIndexOfAvgKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeFlightTime");
          final int _cursorIndexOfAvgTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgTouchPressure");
          final int _cursorIndexOfAvgSwipeVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSwipeVelocity");
          final int _cursorIndexOfAvgDevicePitch = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDevicePitch");
          final int _cursorIndexOfAvgDeviceRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDeviceRoll");
          final int _cursorIndexOfStdKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeDwellTime");
          final int _cursorIndexOfStdKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeFlightTime");
          final int _cursorIndexOfStdTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "stdTouchPressure");
          final int _cursorIndexOfCurrentRiskScore = CursorUtil.getColumnIndexOrThrow(_cursor, "currentRiskScore");
          final int _cursorIndexOfRiskAlertCount = CursorUtil.getColumnIndexOrThrow(_cursor, "riskAlertCount");
          final BehavioralSession _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBaseline);
            _tmpIsBaseline = _tmp != 0;
            final float _tmpAvgKeystrokeDwellTime;
            _tmpAvgKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeDwellTime);
            final float _tmpAvgKeystrokeFlightTime;
            _tmpAvgKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeFlightTime);
            final float _tmpAvgTouchPressure;
            _tmpAvgTouchPressure = _cursor.getFloat(_cursorIndexOfAvgTouchPressure);
            final float _tmpAvgSwipeVelocity;
            _tmpAvgSwipeVelocity = _cursor.getFloat(_cursorIndexOfAvgSwipeVelocity);
            final float _tmpAvgDevicePitch;
            _tmpAvgDevicePitch = _cursor.getFloat(_cursorIndexOfAvgDevicePitch);
            final float _tmpAvgDeviceRoll;
            _tmpAvgDeviceRoll = _cursor.getFloat(_cursorIndexOfAvgDeviceRoll);
            final float _tmpStdKeystrokeDwellTime;
            _tmpStdKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeDwellTime);
            final float _tmpStdKeystrokeFlightTime;
            _tmpStdKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeFlightTime);
            final float _tmpStdTouchPressure;
            _tmpStdTouchPressure = _cursor.getFloat(_cursorIndexOfStdTouchPressure);
            final float _tmpCurrentRiskScore;
            _tmpCurrentRiskScore = _cursor.getFloat(_cursorIndexOfCurrentRiskScore);
            final int _tmpRiskAlertCount;
            _tmpRiskAlertCount = _cursor.getInt(_cursorIndexOfRiskAlertCount);
            _result = new BehavioralSession(_tmpSessionId,_tmpUserId,_tmpStartTime,_tmpEndTime,_tmpIsBaseline,_tmpAvgKeystrokeDwellTime,_tmpAvgKeystrokeFlightTime,_tmpAvgTouchPressure,_tmpAvgSwipeVelocity,_tmpAvgDevicePitch,_tmpAvgDeviceRoll,_tmpStdKeystrokeDwellTime,_tmpStdKeystrokeFlightTime,_tmpStdTouchPressure,_tmpCurrentRiskScore,_tmpRiskAlertCount);
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
  public Flow<BehavioralSession> observeSession(final String sessionId) {
    final String _sql = "SELECT * FROM behavioral_sessions WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavioral_sessions"}, new Callable<BehavioralSession>() {
      @Override
      @Nullable
      public BehavioralSession call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfIsBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isBaseline");
          final int _cursorIndexOfAvgKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeDwellTime");
          final int _cursorIndexOfAvgKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeFlightTime");
          final int _cursorIndexOfAvgTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgTouchPressure");
          final int _cursorIndexOfAvgSwipeVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSwipeVelocity");
          final int _cursorIndexOfAvgDevicePitch = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDevicePitch");
          final int _cursorIndexOfAvgDeviceRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDeviceRoll");
          final int _cursorIndexOfStdKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeDwellTime");
          final int _cursorIndexOfStdKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeFlightTime");
          final int _cursorIndexOfStdTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "stdTouchPressure");
          final int _cursorIndexOfCurrentRiskScore = CursorUtil.getColumnIndexOrThrow(_cursor, "currentRiskScore");
          final int _cursorIndexOfRiskAlertCount = CursorUtil.getColumnIndexOrThrow(_cursor, "riskAlertCount");
          final BehavioralSession _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBaseline);
            _tmpIsBaseline = _tmp != 0;
            final float _tmpAvgKeystrokeDwellTime;
            _tmpAvgKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeDwellTime);
            final float _tmpAvgKeystrokeFlightTime;
            _tmpAvgKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeFlightTime);
            final float _tmpAvgTouchPressure;
            _tmpAvgTouchPressure = _cursor.getFloat(_cursorIndexOfAvgTouchPressure);
            final float _tmpAvgSwipeVelocity;
            _tmpAvgSwipeVelocity = _cursor.getFloat(_cursorIndexOfAvgSwipeVelocity);
            final float _tmpAvgDevicePitch;
            _tmpAvgDevicePitch = _cursor.getFloat(_cursorIndexOfAvgDevicePitch);
            final float _tmpAvgDeviceRoll;
            _tmpAvgDeviceRoll = _cursor.getFloat(_cursorIndexOfAvgDeviceRoll);
            final float _tmpStdKeystrokeDwellTime;
            _tmpStdKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeDwellTime);
            final float _tmpStdKeystrokeFlightTime;
            _tmpStdKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeFlightTime);
            final float _tmpStdTouchPressure;
            _tmpStdTouchPressure = _cursor.getFloat(_cursorIndexOfStdTouchPressure);
            final float _tmpCurrentRiskScore;
            _tmpCurrentRiskScore = _cursor.getFloat(_cursorIndexOfCurrentRiskScore);
            final int _tmpRiskAlertCount;
            _tmpRiskAlertCount = _cursor.getInt(_cursorIndexOfRiskAlertCount);
            _result = new BehavioralSession(_tmpSessionId,_tmpUserId,_tmpStartTime,_tmpEndTime,_tmpIsBaseline,_tmpAvgKeystrokeDwellTime,_tmpAvgKeystrokeFlightTime,_tmpAvgTouchPressure,_tmpAvgSwipeVelocity,_tmpAvgDevicePitch,_tmpAvgDeviceRoll,_tmpStdKeystrokeDwellTime,_tmpStdKeystrokeFlightTime,_tmpStdTouchPressure,_tmpCurrentRiskScore,_tmpRiskAlertCount);
          } else {
            _result = null;
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
  public Object getLatestBaselineSession(final String userId,
      final Continuation<? super BehavioralSession> $completion) {
    final String _sql = "SELECT * FROM behavioral_sessions WHERE userId = ? AND isBaseline = 1 ORDER BY startTime DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BehavioralSession>() {
      @Override
      @Nullable
      public BehavioralSession call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfIsBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isBaseline");
          final int _cursorIndexOfAvgKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeDwellTime");
          final int _cursorIndexOfAvgKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeFlightTime");
          final int _cursorIndexOfAvgTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgTouchPressure");
          final int _cursorIndexOfAvgSwipeVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSwipeVelocity");
          final int _cursorIndexOfAvgDevicePitch = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDevicePitch");
          final int _cursorIndexOfAvgDeviceRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDeviceRoll");
          final int _cursorIndexOfStdKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeDwellTime");
          final int _cursorIndexOfStdKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeFlightTime");
          final int _cursorIndexOfStdTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "stdTouchPressure");
          final int _cursorIndexOfCurrentRiskScore = CursorUtil.getColumnIndexOrThrow(_cursor, "currentRiskScore");
          final int _cursorIndexOfRiskAlertCount = CursorUtil.getColumnIndexOrThrow(_cursor, "riskAlertCount");
          final BehavioralSession _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBaseline);
            _tmpIsBaseline = _tmp != 0;
            final float _tmpAvgKeystrokeDwellTime;
            _tmpAvgKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeDwellTime);
            final float _tmpAvgKeystrokeFlightTime;
            _tmpAvgKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeFlightTime);
            final float _tmpAvgTouchPressure;
            _tmpAvgTouchPressure = _cursor.getFloat(_cursorIndexOfAvgTouchPressure);
            final float _tmpAvgSwipeVelocity;
            _tmpAvgSwipeVelocity = _cursor.getFloat(_cursorIndexOfAvgSwipeVelocity);
            final float _tmpAvgDevicePitch;
            _tmpAvgDevicePitch = _cursor.getFloat(_cursorIndexOfAvgDevicePitch);
            final float _tmpAvgDeviceRoll;
            _tmpAvgDeviceRoll = _cursor.getFloat(_cursorIndexOfAvgDeviceRoll);
            final float _tmpStdKeystrokeDwellTime;
            _tmpStdKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeDwellTime);
            final float _tmpStdKeystrokeFlightTime;
            _tmpStdKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeFlightTime);
            final float _tmpStdTouchPressure;
            _tmpStdTouchPressure = _cursor.getFloat(_cursorIndexOfStdTouchPressure);
            final float _tmpCurrentRiskScore;
            _tmpCurrentRiskScore = _cursor.getFloat(_cursorIndexOfCurrentRiskScore);
            final int _tmpRiskAlertCount;
            _tmpRiskAlertCount = _cursor.getInt(_cursorIndexOfRiskAlertCount);
            _result = new BehavioralSession(_tmpSessionId,_tmpUserId,_tmpStartTime,_tmpEndTime,_tmpIsBaseline,_tmpAvgKeystrokeDwellTime,_tmpAvgKeystrokeFlightTime,_tmpAvgTouchPressure,_tmpAvgSwipeVelocity,_tmpAvgDevicePitch,_tmpAvgDeviceRoll,_tmpStdKeystrokeDwellTime,_tmpStdKeystrokeFlightTime,_tmpStdTouchPressure,_tmpCurrentRiskScore,_tmpRiskAlertCount);
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
  public Flow<List<BehavioralSession>> getUserSessions(final String userId) {
    final String _sql = "SELECT * FROM behavioral_sessions WHERE userId = ? ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavioral_sessions"}, new Callable<List<BehavioralSession>>() {
      @Override
      @NonNull
      public List<BehavioralSession> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfEndTime = CursorUtil.getColumnIndexOrThrow(_cursor, "endTime");
          final int _cursorIndexOfIsBaseline = CursorUtil.getColumnIndexOrThrow(_cursor, "isBaseline");
          final int _cursorIndexOfAvgKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeDwellTime");
          final int _cursorIndexOfAvgKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "avgKeystrokeFlightTime");
          final int _cursorIndexOfAvgTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "avgTouchPressure");
          final int _cursorIndexOfAvgSwipeVelocity = CursorUtil.getColumnIndexOrThrow(_cursor, "avgSwipeVelocity");
          final int _cursorIndexOfAvgDevicePitch = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDevicePitch");
          final int _cursorIndexOfAvgDeviceRoll = CursorUtil.getColumnIndexOrThrow(_cursor, "avgDeviceRoll");
          final int _cursorIndexOfStdKeystrokeDwellTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeDwellTime");
          final int _cursorIndexOfStdKeystrokeFlightTime = CursorUtil.getColumnIndexOrThrow(_cursor, "stdKeystrokeFlightTime");
          final int _cursorIndexOfStdTouchPressure = CursorUtil.getColumnIndexOrThrow(_cursor, "stdTouchPressure");
          final int _cursorIndexOfCurrentRiskScore = CursorUtil.getColumnIndexOrThrow(_cursor, "currentRiskScore");
          final int _cursorIndexOfRiskAlertCount = CursorUtil.getColumnIndexOrThrow(_cursor, "riskAlertCount");
          final List<BehavioralSession> _result = new ArrayList<BehavioralSession>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BehavioralSession _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final Long _tmpEndTime;
            if (_cursor.isNull(_cursorIndexOfEndTime)) {
              _tmpEndTime = null;
            } else {
              _tmpEndTime = _cursor.getLong(_cursorIndexOfEndTime);
            }
            final boolean _tmpIsBaseline;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsBaseline);
            _tmpIsBaseline = _tmp != 0;
            final float _tmpAvgKeystrokeDwellTime;
            _tmpAvgKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeDwellTime);
            final float _tmpAvgKeystrokeFlightTime;
            _tmpAvgKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfAvgKeystrokeFlightTime);
            final float _tmpAvgTouchPressure;
            _tmpAvgTouchPressure = _cursor.getFloat(_cursorIndexOfAvgTouchPressure);
            final float _tmpAvgSwipeVelocity;
            _tmpAvgSwipeVelocity = _cursor.getFloat(_cursorIndexOfAvgSwipeVelocity);
            final float _tmpAvgDevicePitch;
            _tmpAvgDevicePitch = _cursor.getFloat(_cursorIndexOfAvgDevicePitch);
            final float _tmpAvgDeviceRoll;
            _tmpAvgDeviceRoll = _cursor.getFloat(_cursorIndexOfAvgDeviceRoll);
            final float _tmpStdKeystrokeDwellTime;
            _tmpStdKeystrokeDwellTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeDwellTime);
            final float _tmpStdKeystrokeFlightTime;
            _tmpStdKeystrokeFlightTime = _cursor.getFloat(_cursorIndexOfStdKeystrokeFlightTime);
            final float _tmpStdTouchPressure;
            _tmpStdTouchPressure = _cursor.getFloat(_cursorIndexOfStdTouchPressure);
            final float _tmpCurrentRiskScore;
            _tmpCurrentRiskScore = _cursor.getFloat(_cursorIndexOfCurrentRiskScore);
            final int _tmpRiskAlertCount;
            _tmpRiskAlertCount = _cursor.getInt(_cursorIndexOfRiskAlertCount);
            _item = new BehavioralSession(_tmpSessionId,_tmpUserId,_tmpStartTime,_tmpEndTime,_tmpIsBaseline,_tmpAvgKeystrokeDwellTime,_tmpAvgKeystrokeFlightTime,_tmpAvgTouchPressure,_tmpAvgSwipeVelocity,_tmpAvgDevicePitch,_tmpAvgDeviceRoll,_tmpStdKeystrokeDwellTime,_tmpStdKeystrokeFlightTime,_tmpStdTouchPressure,_tmpCurrentRiskScore,_tmpRiskAlertCount);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
