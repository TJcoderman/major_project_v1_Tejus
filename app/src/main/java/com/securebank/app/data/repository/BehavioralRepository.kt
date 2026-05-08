package com.securebank.app.data.repository

import com.securebank.app.data.local.dao.*
import com.securebank.app.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ============================================
 * BEHAVIORAL DATA REPOSITORY
 * ============================================
 * Single source of truth for all behavioral data operations.
 * Handles keystroke, touch, and motion data storage/retrieval.
 */
@Singleton
class BehavioralRepository @Inject constructor(
    private val keystrokeDao: KeystrokeDao,
    private val touchDao: TouchDao,
    private val motionDao: MotionDao,
    private val sessionDao: BehavioralSessionDao
) {
    // ========================
    // KEYSTROKE OPERATIONS
    // ========================
    
    suspend fun saveKeystroke(keystrokeData: KeystrokeData) {
        keystrokeDao.insert(keystrokeData)
    }
    
    suspend fun saveKeystrokes(keystrokes: List<KeystrokeData>) {
        keystrokeDao.insertAll(keystrokes)
    }
    
    fun getSessionKeystrokes(sessionId: String): Flow<List<KeystrokeData>> {
        return keystrokeDao.getBySession(sessionId)
    }
    
    suspend fun getBaselineKeystrokes(sessionId: String): List<KeystrokeData> {
        return keystrokeDao.getBaselineKeystrokes(sessionId)
    }
    
    suspend fun getAverageKeystrokeMetrics(sessionId: String, isBaseline: Boolean): Pair<Float, Float> {
        val avgDwell = keystrokeDao.getAvgDwellTime(sessionId, isBaseline) ?: 0f
        val avgFlight = keystrokeDao.getAvgFlightTime(sessionId, isBaseline) ?: 0f
        return Pair(avgDwell, avgFlight)
    }

    suspend fun getRecentKeystrokeData(sessionId: String, limit: Int): List<com.securebank.app.data.model.KeystrokeData> {
        return keystrokeDao.getRecentSessionKeystrokes(sessionId, limit)
    }
    
    // ========================
    // TOUCH OPERATIONS
    // ========================
    
    suspend fun saveTouch(touchData: TouchData) {
        touchDao.insert(touchData)
    }
    
    suspend fun saveTouches(touches: List<TouchData>) {
        touchDao.insertAll(touches)
    }
    
    fun getSessionTouches(sessionId: String): Flow<List<TouchData>> {
        return touchDao.getBySession(sessionId)
    }
    
    suspend fun getRecentTouches(sessionId: String, limit: Int = 100): List<TouchData> {
        return touchDao.getRecentTouches(sessionId, limit)
    }
    
    suspend fun getAverageTouchMetrics(sessionId: String): Pair<Float, Float> {
        val avgPressure = touchDao.getAvgPressure(sessionId) ?: 0f
        val avgSwipeVelocity = touchDao.getAvgSwipeVelocity(sessionId) ?: 0f
        return Pair(avgPressure, avgSwipeVelocity)
    }
    
    // ========================
    // MOTION OPERATIONS
    // ========================
    
    suspend fun saveMotion(motionData: MotionData) {
        motionDao.insert(motionData)
    }
    
    suspend fun saveMotionBatch(motionDataList: List<MotionData>) {
        motionDao.insertAll(motionDataList)
    }
    
    fun getSessionMotion(sessionId: String): Flow<List<MotionData>> {
        return motionDao.getBySession(sessionId)
    }
    
    suspend fun getRecentMotion(sessionId: String, limit: Int = 100): List<MotionData> {
        return motionDao.getRecentMotion(sessionId, limit)
    }
    
    suspend fun getAverageMotionMetrics(sessionId: String): Triple<Float, Float, DeviceState?> {
        val avgPitch = motionDao.getAvgPitch(sessionId) ?: 0f
        val avgRoll = motionDao.getAvgRoll(sessionId) ?: 0f
        val commonStateString = motionDao.getMostCommonDeviceState(sessionId)
        val commonState = commonStateString?.let { 
            try { DeviceState.valueOf(it) } catch (e: Exception) { null }
        }
        return Triple(avgPitch, avgRoll, commonState)
    }
    
    // ========================
    // SESSION OPERATIONS
    // ========================
    
    suspend fun createSession(session: BehavioralSession) {
        sessionDao.insert(session)
    }
    
    suspend fun updateSession(session: BehavioralSession) {
        sessionDao.update(session)
    }
    
    suspend fun getSession(sessionId: String): BehavioralSession? {
        return sessionDao.getBySessionId(sessionId)
    }
    
    fun observeSession(sessionId: String): Flow<BehavioralSession?> {
        return sessionDao.observeSession(sessionId)
    }
    
    suspend fun getBaselineSession(userId: String): BehavioralSession? {
        return sessionDao.getLatestBaselineSession(userId)
    }
    
    fun getUserSessions(userId: String): Flow<List<BehavioralSession>> {
        return sessionDao.getUserSessions(userId)
    }
    
    // ========================
    // WINDOWED / RECENT OPERATIONS
    // ========================

    /**
     * Returns recent keystroke metrics (dwell, flight) from the latest [windowSize] non-baseline
     * samples. Returns null if there are fewer than [minSamples] to avoid noisy comparisons.
     */
    suspend fun getRecentKeystrokeMetrics(
        sessionId: String,
        windowSize: Int,
        minSamples: Int
    ): Pair<Float, Float>? {
        val count = keystrokeDao.getRecentKeystrokeCount(sessionId)
        if (count < minSamples) return null
        val avgDwell = keystrokeDao.getRecentAvgDwellTime(sessionId, windowSize) ?: return null
        val avgFlight = keystrokeDao.getRecentAvgFlightTime(sessionId, windowSize) ?: return null
        return Pair(avgDwell, avgFlight)
    }

    /**
     * Returns recent touch metrics (pressure, swipe velocity) from the latest [windowSize]
     * samples. Returns null if there are fewer than [minSamples].
     */
    suspend fun getRecentTouchMetrics(
        sessionId: String,
        windowSize: Int,
        minSamples: Int
    ): Pair<Float, Float>? {
        val count = touchDao.getRecentTouchCount(sessionId)
        if (count < minSamples) return null
        val avgPressure = touchDao.getRecentAvgPressure(sessionId, windowSize) ?: return null
        val avgVelocity = touchDao.getRecentAvgSwipeVelocity(sessionId, windowSize) ?: 0f
        return Pair(avgPressure, avgVelocity)
    }

    /**
     * Returns recent motion metrics (pitch, roll, state) from the latest [windowSize]
     * samples. Returns null if there are fewer than [minSamples].
     */
    suspend fun getRecentMotionMetrics(
        sessionId: String,
        windowSize: Int,
        minSamples: Int
    ): Triple<Float, Float, DeviceState?>? {
        val count = motionDao.getRecentMotionCount(sessionId)
        if (count < minSamples) return null
        val avgPitch = motionDao.getRecentAvgPitch(sessionId, windowSize) ?: return null
        val avgRoll = motionDao.getRecentAvgRoll(sessionId, windowSize) ?: return null
        val commonStateString = motionDao.getMostCommonDeviceState(sessionId)
        val commonState = commonStateString?.let {
            try { DeviceState.valueOf(it) } catch (e: Exception) { null }
        }
        return Triple(avgPitch, avgRoll, commonState)
    }

    // ========================
    // CLEANUP OPERATIONS
    // ========================
    
    suspend fun clearSessionData(sessionId: String) {
        keystrokeDao.deleteBySession(sessionId)
        touchDao.deleteBySession(sessionId)
        motionDao.deleteBySession(sessionId)
        sessionDao.deleteSession(sessionId)
    }
    
    suspend fun cleanupOldData(olderThanTimestamp: Long) {
        keystrokeDao.deleteOlderThan(olderThanTimestamp)
        touchDao.deleteOlderThan(olderThanTimestamp)
        motionDao.deleteOlderThan(olderThanTimestamp)
        sessionDao.deleteOldSessions(olderThanTimestamp)
    }
}

