package com.securebank.app.sensor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import com.securebank.app.data.model.TouchData
import com.securebank.app.data.model.TouchType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ============================================
 * TOUCH DATA COLLECTOR
 * ============================================
 * Collects and processes touch events from the UI.
 * Calculates touch metrics like pressure, velocity, and patterns.
 */
@Singleton
class TouchDataCollector @Inject constructor() {
    
    private val _touchEvents = MutableSharedFlow<TouchData>(replay = 0, extraBufferCapacity = 100)
    val touchEvents: SharedFlow<TouchData> = _touchEvents.asSharedFlow()
    
    private var currentSessionId: String = ""
    private var isCollecting = false
    
    // Touch tracking state
    private var touchStartTime: Long = 0
    private var touchStartPosition: Offset = Offset.Zero
    private var lastTouchEndTime: Long = 0
    private var touchMovePositions = mutableListOf<Pair<Offset, Long>>()
    private var activePointerId: Long? = null
    
    // Hold duration tracking
    private var stationaryStartTime: Long = 0
    private var totalHoldDuration: Long = 0
    private var lastMovePosition: Offset = Offset.Zero
    private var lastPressure: Float = 0f
    private var lastSize: Float = 0f
    private val pressureSamples = mutableListOf<Float>()
    private val sizeSamples = mutableListOf<Float>()
    
    companion object {
        private const val STATIONARY_THRESHOLD = 5f  // px movement threshold for "stationary"
        private const val TAP_MAX_DURATION = 200L           // Max duration for a tap (ms)
        private const val LONG_PRESS_MIN_DURATION = 500L    // Min duration for long press (ms)
        private const val SWIPE_MIN_DISTANCE = 50f          // Min distance for swipe (px)
        private const val SWIPE_VELOCITY_THRESHOLD = 100f   // Min velocity for swipe (px/s)
    }
    
    /**
     * Starts touch data collection for a session.
     */
    fun startCollection(sessionId: String) {
        currentSessionId = sessionId
        isCollecting = true
        resetTouchState()
    }
    
    /**
     * Stops touch data collection.
     */
    fun stopCollection() {
        isCollecting = false
        resetTouchState()
    }
    
    /**
     * Called when a touch begins (ACTION_DOWN equivalent).
     */
    fun onTouchStart(position: Offset, pressure: Float, size: Float) {
        if (!isCollecting) return
        
        touchStartTime = System.currentTimeMillis()
        touchStartPosition = position
        touchMovePositions.clear()
        touchMovePositions.add(Pair(position, touchStartTime))
        
        // Hold tracking
        stationaryStartTime = touchStartTime
        totalHoldDuration = 0
        lastMovePosition = position
        lastPressure = pressure
        lastSize = size
        pressureSamples.clear()
        sizeSamples.clear()
        addTouchShapeSample(pressure, size)
    }
    
    /**
     * Called during touch movement (ACTION_MOVE equivalent).
     */
    fun onTouchMove(
        position: Offset,
        pressure: Float = lastPressure,
        size: Float = lastSize
    ) {
        if (!isCollecting) return
        
        val now = System.currentTimeMillis()
        touchMovePositions.add(Pair(position, now))
        addTouchShapeSample(pressure, size)
        
        // Track hold duration: accumulate time spent stationary
        val distFromLast = calculateDistance(lastMovePosition, position)
        if (distFromLast > STATIONARY_THRESHOLD) {
            // Finger moved — save any accumulated stationary time
            if (stationaryStartTime > 0) {
                totalHoldDuration += now - stationaryStartTime
            }
            stationaryStartTime = now
            lastMovePosition = position
        }
        lastPressure = pressure
        lastSize = size
        
        // Keep only recent positions for velocity calculation
        if (touchMovePositions.size > 20) {
            touchMovePositions.removeAt(0)
        }
    }
    
    /**
     * Called when a touch ends (ACTION_UP equivalent).
     */
    suspend fun onTouchEnd(
        endPosition: Offset,
        pressure: Float,
        size: Float
    ) {
        if (!isCollecting) return
        
        val touchEndTime = System.currentTimeMillis()
        val duration = touchEndTime - touchStartTime
        val distance = calculateDistance(touchStartPosition, endPosition)
        val velocity = calculateVelocity()
        val acceleration = calculateAcceleration()
        addTouchShapeSample(pressure, size)
        
        // Finalize hold duration
        if (stationaryStartTime > 0) {
            totalHoldDuration += touchEndTime - stationaryStartTime
        }
        
        // Compute normalized touch area (pressure × size gives a proxy)
        val avgPressure = pressureSamples.averageOrFallback((lastPressure + pressure) / 2f)
        val avgSize = sizeSamples.averageOrFallback((lastSize + size) / 2f)
        val normalizedArea = avgPressure * avgSize
        
        // Classify touch type
        val touchType = classifyTouch(duration, distance, velocity, endPosition)
        
        // Create touch data
        val touchData = TouchData(
            sessionId = currentSessionId,
            timestamp = touchStartTime,
            touchType = touchType,
            startX = touchStartPosition.x,
            startY = touchStartPosition.y,
            endX = endPosition.x,
            endY = endPosition.y,
            pressure = avgPressure.coerceIn(0f, 1f),
            touchSize = avgSize,
            duration = duration,
            velocity = velocity,
            acceleration = acceleration,
            holdDuration = totalHoldDuration,
            touchArea = normalizedArea
        )
        
        _touchEvents.emit(touchData)
        
        lastTouchEndTime = touchEndTime
        resetTouchState()
    }
    
    /**
     * Process raw pointer input from Compose.
     * This is the main entry point from the UI layer.
     */
    suspend fun processPointerInput(
        change: PointerInputChange,
        isDown: Boolean,
        isUp: Boolean
    ) {
        if (!isCollecting) return
        
        val currentPointerId = change.id.value
        val position = change.position
        val pressure = change.pressure
        val size = 1f // Compose doesn't directly expose touch size
        
        if (isDown) {
            // Only start tracking if no pointer is currently active
            if (activePointerId == null) {
                activePointerId = currentPointerId
                onTouchStart(position, pressure, size)
            }
        } else if (isUp) {
            // Only end tracking if this is the active pointer
            if (activePointerId == currentPointerId) {
                onTouchEnd(position, pressure, size)
                activePointerId = null
            }
        } else {
            // Only process movement for the active pointer
            if (activePointerId == currentPointerId) {
                onTouchMove(position, pressure, size)
            }
        }
    }

    private fun addTouchShapeSample(pressure: Float, size: Float) {
        pressureSamples.add(pressure.coerceIn(0f, 1f))
        sizeSamples.add(size.coerceAtLeast(0f))
        if (pressureSamples.size > 32) pressureSamples.removeAt(0)
        if (sizeSamples.size > 32) sizeSamples.removeAt(0)
    }

    private fun List<Float>.averageOrFallback(fallback: Float): Float =
        if (isEmpty()) fallback else average().toFloat()
    
    /**
     * Classifies the touch type based on duration, distance, and velocity.
     */
    private fun classifyTouch(
        duration: Long,
        distance: Float,
        velocity: Float,
        endPosition: Offset
    ): TouchType {
        // Long press detection
        if (duration >= LONG_PRESS_MIN_DURATION && distance < SWIPE_MIN_DISTANCE) {
            return TouchType.LONG_PRESS
        }
        
        // Swipe detection
        if (distance >= SWIPE_MIN_DISTANCE && velocity > SWIPE_VELOCITY_THRESHOLD) {
            val deltaX = endPosition.x - touchStartPosition.x
            val deltaY = endPosition.y - touchStartPosition.y
            
            return when {
                abs(deltaX) > abs(deltaY) -> {
                    if (deltaX > 0) TouchType.SWIPE_RIGHT else TouchType.SWIPE_LEFT
                }
                else -> {
                    if (deltaY > 0) TouchType.SWIPE_DOWN else TouchType.SWIPE_UP
                }
            }
        }
        
        // Tap detection
        if (duration <= TAP_MAX_DURATION && distance < SWIPE_MIN_DISTANCE) {
            return TouchType.TAP
        }
        
        // Default to scroll for medium-length touches with some movement
        return if (distance > 10f) TouchType.SCROLL else TouchType.TAP
    }
    
    /**
     * Calculates distance between two points.
     */
    private fun calculateDistance(start: Offset, end: Offset): Float {
        val dx = end.x - start.x
        val dy = end.y - start.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculates average velocity from touch movement history.
     */
    private fun calculateVelocity(): Float {
        if (touchMovePositions.size < 2) return 0f
        
        var totalDistance = 0f
        var totalTime = 0L
        
        for (i in 1 until touchMovePositions.size) {
            val (prevPos, prevTime) = touchMovePositions[i - 1]
            val (currPos, currTime) = touchMovePositions[i]
            
            totalDistance += calculateDistance(prevPos, currPos)
            totalTime += currTime - prevTime
        }
        
        return if (totalTime > 0) {
            (totalDistance / totalTime) * 1000f  // Convert to px/second
        } else 0f
    }
    
    /**
     * Calculates acceleration from touch movement history.
     */
    private fun calculateAcceleration(): Float {
        if (touchMovePositions.size < 3) return 0f
        
        val velocities = mutableListOf<Float>()
        
        for (i in 1 until touchMovePositions.size) {
            val (prevPos, prevTime) = touchMovePositions[i - 1]
            val (currPos, currTime) = touchMovePositions[i]
            
            val dt = (currTime - prevTime).toFloat()
            if (dt > 0) {
                val distance = calculateDistance(prevPos, currPos)
                velocities.add(distance / dt * 1000f)
            }
        }
        
        if (velocities.size < 2) return 0f
        
        // Calculate average acceleration
        var totalAccel = 0f
        for (i in 1 until velocities.size) {
            totalAccel += abs(velocities[i] - velocities[i - 1])
        }
        
        return totalAccel / (velocities.size - 1)
    }
    
    /**
     * Resets touch tracking state.
     */
    private fun resetTouchState() {
        touchStartTime = 0
        touchStartPosition = Offset.Zero
        touchMovePositions.clear()
        activePointerId = null
        stationaryStartTime = 0
        totalHoldDuration = 0
        lastMovePosition = Offset.Zero
        lastPressure = 0f
        lastSize = 0f
        pressureSamples.clear()
        sizeSamples.clear()
    }
    
    /**
     * Gets the time since last touch ended.
     */
    fun getTimeSinceLastTouch(): Long {
        return if (lastTouchEndTime > 0) {
            System.currentTimeMillis() - lastTouchEndTime
        } else 0
    }
}

