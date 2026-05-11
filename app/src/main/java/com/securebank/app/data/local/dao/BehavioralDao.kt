package com.securebank.app.data.local.dao

import androidx.room.*
import com.securebank.app.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * ============================================
 * KEYSTROKE DATA DAO
 * ============================================
 */
@Dao
interface KeystrokeDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(keystrokeData: KeystrokeData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keystrokeDataList: List<KeystrokeData>)
    
    @Query("SELECT * FROM keystroke_data WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: String): Flow<List<KeystrokeData>>
    
    @Query("SELECT * FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 1")
    suspend fun getBaselineKeystrokes(sessionId: String): List<KeystrokeData>
    
    @Query("SELECT * FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 0")
    suspend fun getSessionKeystrokes(sessionId: String): List<KeystrokeData>

    @Query("SELECT * FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSessionKeystrokes(sessionId: String, limit: Int): List<KeystrokeData>
    
    @Query("SELECT AVG(dwellTime) FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = :isBaseline")
    suspend fun getAvgDwellTime(sessionId: String, isBaseline: Boolean): Float?
    
    @Query("SELECT AVG(flightTime) FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = :isBaseline")
    suspend fun getAvgFlightTime(sessionId: String, isBaseline: Boolean): Float?

    @Query("SELECT COUNT(*) FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 0")
    suspend fun getRecentKeystrokeCount(sessionId: String): Int

    @Query("SELECT AVG(dwellTime) FROM (SELECT dwellTime FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 0 ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgDwellTime(sessionId: String, limit: Int): Float?

    @Query("SELECT AVG(flightTime) FROM (SELECT flightTime FROM keystroke_data WHERE sessionId = :sessionId AND isLoginBaseline = 0 ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgFlightTime(sessionId: String, limit: Int): Float?
    
    @Query("DELETE FROM keystroke_data WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
    
    @Query("DELETE FROM keystroke_data WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * ============================================
 * TOUCH DATA DAO
 * ============================================
 */
@Dao
interface TouchDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(touchData: TouchData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(touchDataList: List<TouchData>)
    
    @Query("SELECT * FROM touch_data WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: String): Flow<List<TouchData>>
    
    @Query("SELECT * FROM touch_data WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTouches(sessionId: String, limit: Int): List<TouchData>
    
    @Query("SELECT AVG(pressure) FROM touch_data WHERE sessionId = :sessionId")
    suspend fun getAvgPressure(sessionId: String): Float?
    
    @Query("SELECT AVG(velocity) FROM touch_data WHERE sessionId = :sessionId AND touchType IN ('SWIPE_UP', 'SWIPE_DOWN', 'SWIPE_LEFT', 'SWIPE_RIGHT')")
    suspend fun getAvgSwipeVelocity(sessionId: String): Float?
    
    @Query("SELECT COUNT(*) FROM touch_data WHERE sessionId = :sessionId AND touchType = :touchType")
    suspend fun countByType(sessionId: String, touchType: TouchType): Int

    @Query("SELECT COUNT(*) FROM touch_data WHERE sessionId = :sessionId")
    suspend fun getRecentTouchCount(sessionId: String): Int

    @Query("SELECT AVG(pressure) FROM (SELECT pressure FROM touch_data WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgPressure(sessionId: String, limit: Int): Float?

    @Query("SELECT AVG(velocity) FROM (SELECT velocity FROM touch_data WHERE sessionId = :sessionId AND touchType IN ('SWIPE_UP', 'SWIPE_DOWN', 'SWIPE_LEFT', 'SWIPE_RIGHT') ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgSwipeVelocity(sessionId: String, limit: Int): Float?
    
    @Query("DELETE FROM touch_data WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
    
    @Query("DELETE FROM touch_data WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * ============================================
 * MOTION DATA DAO
 * ============================================
 */
@Dao
interface MotionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(motionData: MotionData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(motionDataList: List<MotionData>)
    
    @Query("SELECT * FROM motion_data WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getBySession(sessionId: String): Flow<List<MotionData>>
    
    @Query("SELECT * FROM motion_data WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMotion(sessionId: String, limit: Int): List<MotionData>
    
    @Query("SELECT AVG(pitch) FROM motion_data WHERE sessionId = :sessionId")
    suspend fun getAvgPitch(sessionId: String): Float?
    
    @Query("SELECT AVG(roll) FROM motion_data WHERE sessionId = :sessionId")
    suspend fun getAvgRoll(sessionId: String): Float?
    
    @Query("""
        SELECT deviceState 
        FROM motion_data 
        WHERE sessionId = :sessionId 
        GROUP BY deviceState 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostCommonDeviceState(sessionId: String): String?

    @Query("""
        SELECT deviceState
        FROM (
            SELECT deviceState
            FROM motion_data
            WHERE sessionId = :sessionId
            ORDER BY timestamp DESC
            LIMIT :limit
        )
        GROUP BY deviceState
        ORDER BY COUNT(*) DESC
        LIMIT 1
    """)
    suspend fun getRecentMostCommonDeviceState(sessionId: String, limit: Int): String?

    @Query("SELECT COUNT(*) FROM motion_data WHERE sessionId = :sessionId")
    suspend fun getRecentMotionCount(sessionId: String): Int

    @Query("SELECT AVG(pitch) FROM (SELECT pitch FROM motion_data WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgPitch(sessionId: String, limit: Int): Float?

    @Query("SELECT AVG(roll) FROM (SELECT roll FROM motion_data WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun getRecentAvgRoll(sessionId: String, limit: Int): Float?
    
    @Query("DELETE FROM motion_data WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)
    
    @Query("DELETE FROM motion_data WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}

/**
 * ============================================
 * BEHAVIORAL SESSION DAO
 * ============================================
 */
@Dao
interface BehavioralSessionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: BehavioralSession)
    
    @Update
    suspend fun update(session: BehavioralSession)
    
    @Query("SELECT * FROM behavioral_sessions WHERE sessionId = :sessionId")
    suspend fun getBySessionId(sessionId: String): BehavioralSession?
    
    @Query("SELECT * FROM behavioral_sessions WHERE sessionId = :sessionId")
    fun observeSession(sessionId: String): Flow<BehavioralSession?>
    
    @Query("SELECT * FROM behavioral_sessions WHERE userId = :userId AND isBaseline = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestBaselineSession(userId: String): BehavioralSession?
    
    @Query("SELECT * FROM behavioral_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getUserSessions(userId: String): Flow<List<BehavioralSession>>
    
    @Query("DELETE FROM behavioral_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
    
    @Query("DELETE FROM behavioral_sessions WHERE endTime IS NOT NULL AND endTime < :timestamp")
    suspend fun deleteOldSessions(timestamp: Long)
}
