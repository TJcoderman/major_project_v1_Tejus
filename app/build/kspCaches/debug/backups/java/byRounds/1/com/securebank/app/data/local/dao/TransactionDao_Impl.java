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
import com.securebank.app.data.model.Transaction;
import com.securebank.app.data.model.TransactionStatus;
import com.securebank.app.data.model.TransactionType;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
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
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Transaction> __insertionAdapterOfTransaction;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByUser;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransaction = new EntityInsertionAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`userId`,`type`,`amount`,`recipientAccount`,`recipientName`,`description`,`timestamp`,`status`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, __TransactionType_enumToString(entity.getType()));
        statement.bindDouble(4, entity.getAmount());
        if (entity.getRecipientAccount() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getRecipientAccount());
        }
        if (entity.getRecipientName() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getRecipientName());
        }
        statement.bindString(7, entity.getDescription());
        statement.bindLong(8, entity.getTimestamp());
        statement.bindString(9, __TransactionStatus_enumToString(entity.getStatus()));
      }
    };
    this.__preparedStmtOfDeleteByUser = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE userId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Transaction transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTransaction.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<Transaction> transactions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfTransaction.insert(transactions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByUser(final String userId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByUser.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, userId);
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
          __preparedStmtOfDeleteByUser.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Transaction>> getByUser(final String userId) {
    final String _sql = "SELECT * FROM transactions WHERE userId = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfRecipientAccount = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientAccount");
          final int _cursorIndexOfRecipientName = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientName");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final TransactionType _tmpType;
            _tmpType = __TransactionType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpRecipientAccount;
            if (_cursor.isNull(_cursorIndexOfRecipientAccount)) {
              _tmpRecipientAccount = null;
            } else {
              _tmpRecipientAccount = _cursor.getString(_cursorIndexOfRecipientAccount);
            }
            final String _tmpRecipientName;
            if (_cursor.isNull(_cursorIndexOfRecipientName)) {
              _tmpRecipientName = null;
            } else {
              _tmpRecipientName = _cursor.getString(_cursorIndexOfRecipientName);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final TransactionStatus _tmpStatus;
            _tmpStatus = __TransactionStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            _item = new Transaction(_tmpId,_tmpUserId,_tmpType,_tmpAmount,_tmpRecipientAccount,_tmpRecipientName,_tmpDescription,_tmpTimestamp,_tmpStatus);
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
  public Object getRecentTransactions(final String userId, final int limit,
      final Continuation<? super List<Transaction>> $completion) {
    final String _sql = "SELECT * FROM transactions WHERE userId = ? ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfRecipientAccount = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientAccount");
          final int _cursorIndexOfRecipientName = CursorUtil.getColumnIndexOrThrow(_cursor, "recipientName");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final TransactionType _tmpType;
            _tmpType = __TransactionType_stringToEnum(_cursor.getString(_cursorIndexOfType));
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final String _tmpRecipientAccount;
            if (_cursor.isNull(_cursorIndexOfRecipientAccount)) {
              _tmpRecipientAccount = null;
            } else {
              _tmpRecipientAccount = _cursor.getString(_cursorIndexOfRecipientAccount);
            }
            final String _tmpRecipientName;
            if (_cursor.isNull(_cursorIndexOfRecipientName)) {
              _tmpRecipientName = null;
            } else {
              _tmpRecipientName = _cursor.getString(_cursorIndexOfRecipientName);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final TransactionStatus _tmpStatus;
            _tmpStatus = __TransactionStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            _item = new Transaction(_tmpId,_tmpUserId,_tmpType,_tmpAmount,_tmpRecipientAccount,_tmpRecipientName,_tmpDescription,_tmpTimestamp,_tmpStatus);
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
  public Object getTotalCredits(final String userId,
      final Continuation<? super Double> $completion) {
    final String _sql = "SELECT SUM(amount) FROM transactions WHERE userId = ? AND type = 'CREDIT'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
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
  public Object getTotalDebits(final String userId,
      final Continuation<? super Double> $completion) {
    final String _sql = "SELECT SUM(amount) FROM transactions WHERE userId = ? AND type = 'DEBIT'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __TransactionType_enumToString(@NonNull final TransactionType _value) {
    switch (_value) {
      case CREDIT: return "CREDIT";
      case DEBIT: return "DEBIT";
      case TRANSFER_IN: return "TRANSFER_IN";
      case TRANSFER_OUT: return "TRANSFER_OUT";
      case BILL_PAYMENT: return "BILL_PAYMENT";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __TransactionStatus_enumToString(@NonNull final TransactionStatus _value) {
    switch (_value) {
      case PENDING: return "PENDING";
      case COMPLETED: return "COMPLETED";
      case FAILED: return "FAILED";
      case CANCELLED: return "CANCELLED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private TransactionType __TransactionType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "CREDIT": return TransactionType.CREDIT;
      case "DEBIT": return TransactionType.DEBIT;
      case "TRANSFER_IN": return TransactionType.TRANSFER_IN;
      case "TRANSFER_OUT": return TransactionType.TRANSFER_OUT;
      case "BILL_PAYMENT": return TransactionType.BILL_PAYMENT;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private TransactionStatus __TransactionStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "PENDING": return TransactionStatus.PENDING;
      case "COMPLETED": return TransactionStatus.COMPLETED;
      case "FAILED": return TransactionStatus.FAILED;
      case "CANCELLED": return TransactionStatus.CANCELLED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
