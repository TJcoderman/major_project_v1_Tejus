package com.securebank.app.data.local;

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
import com.securebank.app.data.local.dao.BehavioralSessionDao;
import com.securebank.app.data.local.dao.BehavioralSessionDao_Impl;
import com.securebank.app.data.local.dao.KeystrokeDao;
import com.securebank.app.data.local.dao.KeystrokeDao_Impl;
import com.securebank.app.data.local.dao.MotionDao;
import com.securebank.app.data.local.dao.MotionDao_Impl;
import com.securebank.app.data.local.dao.TouchDao;
import com.securebank.app.data.local.dao.TouchDao_Impl;
import com.securebank.app.data.local.dao.TransactionDao;
import com.securebank.app.data.local.dao.TransactionDao_Impl;
import com.securebank.app.data.local.dao.UserDao;
import com.securebank.app.data.local.dao.UserDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SecureBankDatabase_Impl extends SecureBankDatabase {
  private volatile UserDao _userDao;

  private volatile TransactionDao _transactionDao;

  private volatile KeystrokeDao _keystrokeDao;

  private volatile TouchDao _touchDao;

  private volatile MotionDao _motionDao;

  private volatile BehavioralSessionDao _behavioralSessionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`username` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `fullName` TEXT NOT NULL, `accountNumber` TEXT NOT NULL, `balance` REAL NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`username`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `recipientAccount` TEXT, `recipientName` TEXT, `description` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `status` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `keystroke_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `keyCode` INTEGER NOT NULL, `dwellTime` INTEGER NOT NULL, `flightTime` INTEGER NOT NULL, `isLoginBaseline` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `touch_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `touchType` TEXT NOT NULL, `startX` REAL NOT NULL, `startY` REAL NOT NULL, `endX` REAL NOT NULL, `endY` REAL NOT NULL, `pressure` REAL NOT NULL, `touchSize` REAL NOT NULL, `duration` INTEGER NOT NULL, `velocity` REAL NOT NULL, `acceleration` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `motion_data` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `accelX` REAL NOT NULL, `accelY` REAL NOT NULL, `accelZ` REAL NOT NULL, `gyroX` REAL NOT NULL, `gyroY` REAL NOT NULL, `gyroZ` REAL NOT NULL, `pitch` REAL NOT NULL, `roll` REAL NOT NULL, `azimuth` REAL NOT NULL, `filteredAccelX` REAL NOT NULL, `filteredAccelY` REAL NOT NULL, `filteredAccelZ` REAL NOT NULL, `deviceState` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `behavioral_sessions` (`sessionId` TEXT NOT NULL, `userId` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, `isBaseline` INTEGER NOT NULL, `avgKeystrokeDwellTime` REAL NOT NULL, `avgKeystrokeFlightTime` REAL NOT NULL, `avgTouchPressure` REAL NOT NULL, `avgSwipeVelocity` REAL NOT NULL, `avgDevicePitch` REAL NOT NULL, `avgDeviceRoll` REAL NOT NULL, `stdKeystrokeDwellTime` REAL NOT NULL, `stdKeystrokeFlightTime` REAL NOT NULL, `stdTouchPressure` REAL NOT NULL, `currentRiskScore` REAL NOT NULL, `riskAlertCount` INTEGER NOT NULL, PRIMARY KEY(`sessionId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1eb2aeb3bef52cab1a45ea0749db8e2f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `users`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `keystroke_data`");
        db.execSQL("DROP TABLE IF EXISTS `touch_data`");
        db.execSQL("DROP TABLE IF EXISTS `motion_data`");
        db.execSQL("DROP TABLE IF EXISTS `behavioral_sessions`");
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
        final HashMap<String, TableInfo.Column> _columnsUsers = new HashMap<String, TableInfo.Column>(6);
        _columnsUsers.put("username", new TableInfo.Column("username", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("passwordHash", new TableInfo.Column("passwordHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("fullName", new TableInfo.Column("fullName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("accountNumber", new TableInfo.Column("accountNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("balance", new TableInfo.Column("balance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUsers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUsers = new TableInfo("users", _columnsUsers, _foreignKeysUsers, _indicesUsers);
        final TableInfo _existingUsers = TableInfo.read(db, "users");
        if (!_infoUsers.equals(_existingUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "users(com.securebank.app.data.model.User).\n"
                  + " Expected:\n" + _infoUsers + "\n"
                  + " Found:\n" + _existingUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(9);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recipientAccount", new TableInfo.Column("recipientAccount", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("recipientName", new TableInfo.Column("recipientName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.securebank.app.data.model.Transaction).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsKeystrokeData = new HashMap<String, TableInfo.Column>(7);
        _columnsKeystrokeData.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("keyCode", new TableInfo.Column("keyCode", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("dwellTime", new TableInfo.Column("dwellTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("flightTime", new TableInfo.Column("flightTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeystrokeData.put("isLoginBaseline", new TableInfo.Column("isLoginBaseline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKeystrokeData = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKeystrokeData = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKeystrokeData = new TableInfo("keystroke_data", _columnsKeystrokeData, _foreignKeysKeystrokeData, _indicesKeystrokeData);
        final TableInfo _existingKeystrokeData = TableInfo.read(db, "keystroke_data");
        if (!_infoKeystrokeData.equals(_existingKeystrokeData)) {
          return new RoomOpenHelper.ValidationResult(false, "keystroke_data(com.securebank.app.data.model.KeystrokeData).\n"
                  + " Expected:\n" + _infoKeystrokeData + "\n"
                  + " Found:\n" + _existingKeystrokeData);
        }
        final HashMap<String, TableInfo.Column> _columnsTouchData = new HashMap<String, TableInfo.Column>(13);
        _columnsTouchData.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("touchType", new TableInfo.Column("touchType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("startX", new TableInfo.Column("startX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("startY", new TableInfo.Column("startY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("endX", new TableInfo.Column("endX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("endY", new TableInfo.Column("endY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("pressure", new TableInfo.Column("pressure", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("touchSize", new TableInfo.Column("touchSize", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("velocity", new TableInfo.Column("velocity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTouchData.put("acceleration", new TableInfo.Column("acceleration", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTouchData = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTouchData = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTouchData = new TableInfo("touch_data", _columnsTouchData, _foreignKeysTouchData, _indicesTouchData);
        final TableInfo _existingTouchData = TableInfo.read(db, "touch_data");
        if (!_infoTouchData.equals(_existingTouchData)) {
          return new RoomOpenHelper.ValidationResult(false, "touch_data(com.securebank.app.data.model.TouchData).\n"
                  + " Expected:\n" + _infoTouchData + "\n"
                  + " Found:\n" + _existingTouchData);
        }
        final HashMap<String, TableInfo.Column> _columnsMotionData = new HashMap<String, TableInfo.Column>(16);
        _columnsMotionData.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("accelX", new TableInfo.Column("accelX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("accelY", new TableInfo.Column("accelY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("accelZ", new TableInfo.Column("accelZ", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("gyroX", new TableInfo.Column("gyroX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("gyroY", new TableInfo.Column("gyroY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("gyroZ", new TableInfo.Column("gyroZ", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("pitch", new TableInfo.Column("pitch", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("roll", new TableInfo.Column("roll", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("azimuth", new TableInfo.Column("azimuth", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("filteredAccelX", new TableInfo.Column("filteredAccelX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("filteredAccelY", new TableInfo.Column("filteredAccelY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("filteredAccelZ", new TableInfo.Column("filteredAccelZ", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMotionData.put("deviceState", new TableInfo.Column("deviceState", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMotionData = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMotionData = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMotionData = new TableInfo("motion_data", _columnsMotionData, _foreignKeysMotionData, _indicesMotionData);
        final TableInfo _existingMotionData = TableInfo.read(db, "motion_data");
        if (!_infoMotionData.equals(_existingMotionData)) {
          return new RoomOpenHelper.ValidationResult(false, "motion_data(com.securebank.app.data.model.MotionData).\n"
                  + " Expected:\n" + _infoMotionData + "\n"
                  + " Found:\n" + _existingMotionData);
        }
        final HashMap<String, TableInfo.Column> _columnsBehavioralSessions = new HashMap<String, TableInfo.Column>(16);
        _columnsBehavioralSessions.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("endTime", new TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("isBaseline", new TableInfo.Column("isBaseline", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgKeystrokeDwellTime", new TableInfo.Column("avgKeystrokeDwellTime", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgKeystrokeFlightTime", new TableInfo.Column("avgKeystrokeFlightTime", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgTouchPressure", new TableInfo.Column("avgTouchPressure", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgSwipeVelocity", new TableInfo.Column("avgSwipeVelocity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgDevicePitch", new TableInfo.Column("avgDevicePitch", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("avgDeviceRoll", new TableInfo.Column("avgDeviceRoll", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("stdKeystrokeDwellTime", new TableInfo.Column("stdKeystrokeDwellTime", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("stdKeystrokeFlightTime", new TableInfo.Column("stdKeystrokeFlightTime", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("stdTouchPressure", new TableInfo.Column("stdTouchPressure", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("currentRiskScore", new TableInfo.Column("currentRiskScore", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBehavioralSessions.put("riskAlertCount", new TableInfo.Column("riskAlertCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBehavioralSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBehavioralSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBehavioralSessions = new TableInfo("behavioral_sessions", _columnsBehavioralSessions, _foreignKeysBehavioralSessions, _indicesBehavioralSessions);
        final TableInfo _existingBehavioralSessions = TableInfo.read(db, "behavioral_sessions");
        if (!_infoBehavioralSessions.equals(_existingBehavioralSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "behavioral_sessions(com.securebank.app.data.model.BehavioralSession).\n"
                  + " Expected:\n" + _infoBehavioralSessions + "\n"
                  + " Found:\n" + _existingBehavioralSessions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "1eb2aeb3bef52cab1a45ea0749db8e2f", "848212d7e61b51dd337a92b328e82f62");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "users","transactions","keystroke_data","touch_data","motion_data","behavioral_sessions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `users`");
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `keystroke_data`");
      _db.execSQL("DELETE FROM `touch_data`");
      _db.execSQL("DELETE FROM `motion_data`");
      _db.execSQL("DELETE FROM `behavioral_sessions`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(UserDao.class, UserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(KeystrokeDao.class, KeystrokeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TouchDao.class, TouchDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MotionDao.class, MotionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BehavioralSessionDao.class, BehavioralSessionDao_Impl.getRequiredConverters());
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
  public UserDao userDao() {
    if (_userDao != null) {
      return _userDao;
    } else {
      synchronized(this) {
        if(_userDao == null) {
          _userDao = new UserDao_Impl(this);
        }
        return _userDao;
      }
    }
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public KeystrokeDao keystrokeDao() {
    if (_keystrokeDao != null) {
      return _keystrokeDao;
    } else {
      synchronized(this) {
        if(_keystrokeDao == null) {
          _keystrokeDao = new KeystrokeDao_Impl(this);
        }
        return _keystrokeDao;
      }
    }
  }

  @Override
  public TouchDao touchDao() {
    if (_touchDao != null) {
      return _touchDao;
    } else {
      synchronized(this) {
        if(_touchDao == null) {
          _touchDao = new TouchDao_Impl(this);
        }
        return _touchDao;
      }
    }
  }

  @Override
  public MotionDao motionDao() {
    if (_motionDao != null) {
      return _motionDao;
    } else {
      synchronized(this) {
        if(_motionDao == null) {
          _motionDao = new MotionDao_Impl(this);
        }
        return _motionDao;
      }
    }
  }

  @Override
  public BehavioralSessionDao behavioralSessionDao() {
    if (_behavioralSessionDao != null) {
      return _behavioralSessionDao;
    } else {
      synchronized(this) {
        if(_behavioralSessionDao == null) {
          _behavioralSessionDao = new BehavioralSessionDao_Impl(this);
        }
        return _behavioralSessionDao;
      }
    }
  }
}
