package com.securebank.app.sensor

import com.securebank.app.data.model.KeystrokeData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ============================================
 * KEYSTROKE DATA COLLECTOR
 * ============================================
 * Collects and processes keystroke timing data.
 * Measures dwell time (key press duration) and flight time (inter-key interval).
 */
@Singleton
class KeystrokeCollector @Inject constructor() {
    
    private val _keystrokeEvents = MutableSharedFlow<KeystrokeData>(replay = 0, extraBufferCapacity = 100)
    val keystrokeEvents: SharedFlow<KeystrokeData> = _keystrokeEvents.asSharedFlow()
    
    private var currentSessionId: String = ""
    private var isCollecting = false
    private var isBaselineMode = false
    
    // Keystroke timing tracking
    private var keyDownTime: Long = 0
    private var lastKeyUpTime: Long = 0
    private var lastKeyCode: Int = -1
    
    // Baseline keystroke data (captured during login)
    private val baselineKeystrokes = mutableListOf<KeystrokeData>()
    
    /**
     * Starts keystroke collection for a session.
     * @param sessionId The current session identifier
     * @param isBaseline True if capturing baseline data (during login)
     */
    fun startCollection(sessionId: String, isBaseline: Boolean = false) {
        currentSessionId = sessionId
        isCollecting = true
        isBaselineMode = isBaseline
        
        if (isBaseline) {
            baselineKeystrokes.clear()
        }
        
        resetState()
    }
    
    /**
     * Stops keystroke collection.
     */
    fun stopCollection() {
        isCollecting = false
        resetState()
    }
    
    /**
     * Called when a key is pressed down.
     * Records the start time for dwell time calculation.
     */
    fun onKeyDown(keyCode: Int) {
        if (!isCollecting) return
        keyDownTime = System.currentTimeMillis()
    }
    
    /**
     * Called when a key is released.
     * Calculates dwell time and flight time, then emits keystroke data.
     */
    suspend fun onKeyUp(keyCode: Int) {
        if (!isCollecting) return
        
        val keyUpTime = System.currentTimeMillis()
        
        // Calculate dwell time (how long key was pressed)
        val dwellTime = if (keyDownTime > 0) {
            keyUpTime - keyDownTime
        } else {
            0L
        }
        
        // Calculate flight time (time since last key was released)
        val flightTime = if (lastKeyUpTime > 0) {
            keyDownTime - lastKeyUpTime
        } else {
            0L
        }
        
        // Create keystroke data
        val keystrokeData = KeystrokeData(
            sessionId = currentSessionId,
            timestamp = keyDownTime,
            keyCode = keyCode,
            dwellTime = dwellTime.coerceAtLeast(0),
            flightTime = flightTime.coerceAtLeast(0),
            isLoginBaseline = isBaselineMode
        )
        
        // Store baseline if in baseline mode
        if (isBaselineMode) {
            baselineKeystrokes.add(keystrokeData)
        }
        
        _keystrokeEvents.emit(keystrokeData)
        
        // Update state for next keystroke
        lastKeyUpTime = keyUpTime
        lastKeyCode = keyCode
        keyDownTime = 0
    }
    
    /**
     * Handles character input for software keyboard.
     * Since Android software keyboards don't provide key events in Compose,
     * we simulate keystroke timing based on character input.
     */
    suspend fun onCharacterInput(char: Char, inputTime: Long = System.currentTimeMillis()) {
        if (!isCollecting) return
        
        // Estimate dwell time for software keyboard (average tap duration)
        val estimatedDwellTime = 80L  // Average key press duration
        
        // Calculate flight time
        val flightTime = if (lastKeyUpTime > 0) {
            inputTime - lastKeyUpTime - estimatedDwellTime
        } else {
            0L
        }
        
        val keystrokeData = KeystrokeData(
            sessionId = currentSessionId,
            timestamp = inputTime - estimatedDwellTime,
            keyCode = char.code,
            dwellTime = estimatedDwellTime,
            flightTime = flightTime.coerceAtLeast(0),
            isLoginBaseline = isBaselineMode
        )
        
        if (isBaselineMode) {
            baselineKeystrokes.add(keystrokeData)
        }
        
        _keystrokeEvents.emit(keystrokeData)
        
        lastKeyUpTime = inputTime
        lastKeyCode = char.code
    }
    
    /**
     * Processes text change to extract keystroke timing.
     * Called when text field value changes.
     */
    suspend fun onTextChanged(
        oldText: String,
        newText: String,
        changeTime: Long = System.currentTimeMillis()
    ) {
        if (!isCollecting) return
        
        // Detect the type of change
        when {
            newText.length > oldText.length -> {
                // Character(s) added
                val addedChars = newText.removePrefix(oldText.commonPrefixWith(newText))
                    .removeSuffix(newText.commonSuffixWith(oldText))
                
                for (char in addedChars) {
                    onCharacterInput(char, changeTime)
                }
            }
            newText.length < oldText.length -> {
                // Character(s) deleted (backspace)
                val deletedCount = oldText.length - newText.length
                repeat(deletedCount) {
                    onCharacterInput('\b', changeTime)  // Backspace character
                }
            }
            // Same length but different content (replacement) - treat as delete + add
        }
    }
    
    /**
     * Gets the baseline keystroke data captured during login.
     */
    fun getBaselineKeystrokes(): List<KeystrokeData> {
        return baselineKeystrokes.toList()
    }
    
    /**
     * Calculates average dwell time from baseline data.
     */
    fun getBaselineAverageDwellTime(): Float {
        if (baselineKeystrokes.isEmpty()) return 0f
        return baselineKeystrokes.map { it.dwellTime }.average().toFloat()
    }
    
    /**
     * Calculates average flight time from baseline data.
     */
    fun getBaselineAverageFlightTime(): Float {
        if (baselineKeystrokes.isEmpty()) return 0f
        return baselineKeystrokes.map { it.flightTime }.average().toFloat()
    }
    
    /**
     * Calculates typing speed (characters per minute) from baseline.
     */
    fun getBaselineTypingSpeed(): Float {
        if (baselineKeystrokes.size < 2) return 0f
        
        val firstTimestamp = baselineKeystrokes.first().timestamp
        val lastTimestamp = baselineKeystrokes.last().timestamp
        val durationMinutes = (lastTimestamp - firstTimestamp) / 60000f
        
        return if (durationMinutes > 0) {
            baselineKeystrokes.size / durationMinutes
        } else 0f
    }
    
    /**
     * Resets the internal state.
     */
    private fun resetState() {
        keyDownTime = 0
        lastKeyUpTime = 0
        lastKeyCode = -1
    }
    
    /**
     * Clears baseline data (call when logging out or starting new session).
     */
    fun clearBaseline() {
        baselineKeystrokes.clear()
    }
}

