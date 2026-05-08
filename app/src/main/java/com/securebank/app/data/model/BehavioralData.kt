package com.securebank.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * ============================================
 * KEYSTROKE DYNAMICS DATA
 * ============================================
 * Captures timing information for each keystroke.
 * 
 * Dwell Time: Duration a key is held down (ACTION_DOWN to ACTION_UP)
 * Flight Time: Time between releasing one key and pressing the next
 */
@Entity(tableName = "keystroke_data")
data class KeystrokeData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val keyCode: Int,                    // The key that was pressed
    val dwellTime: Long,                 // Time key was held (ms)
    val flightTime: Long,                // Time since last key release (ms)
    val isLoginBaseline: Boolean = false // True if captured during login (baseline)
)

/**
 * ============================================
 * TOUCH DYNAMICS DATA
 * ============================================
 * Captures touch interaction patterns including pressure, area, and velocity.
 */
@Entity(tableName = "touch_data")
data class TouchData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val touchType: TouchType,            // TAP, SWIPE, SCROLL, LONG_PRESS
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val pressure: Float,                 // Touch pressure (0.0 - 1.0)
    val touchSize: Float,                // Touch area size from sensor
    val duration: Long,                  // Touch duration (ms)
    val velocity: Float,                 // Calculated velocity for swipes
    val acceleration: Float = 0f,        // Touch acceleration
    val holdDuration: Long = 0L,         // Sustained stationary contact time (ms)
    val touchArea: Float = 0f            // Normalized contact patch area
)

enum class TouchType {
    TAP,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SCROLL,
    LONG_PRESS,
    MULTI_TOUCH
}

/**
 * ============================================
 * MOTION/SENSOR DATA
 * ============================================
 * Captures device orientation and movement patterns.
 * Uses accelerometer and gyroscope readings.
 */
@Entity(tableName = "motion_data")
data class MotionData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Accelerometer readings (m/s²) - includes gravity
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    
    // Gyroscope readings (rad/s) - rotation rate
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    
    // Calculated device orientation angles
    val pitch: Float,                    // Rotation around X-axis
    val roll: Float,                     // Rotation around Y-axis
    val azimuth: Float,                  // Rotation around Z-axis
    
    // Filtered values (after noise reduction) - stored as JSON string
    val filteredAccelX: Float = 0f,
    val filteredAccelY: Float = 0f,
    val filteredAccelZ: Float = 0f,
    
    // Device state inference
    val deviceState: String = DeviceState.UNKNOWN.name
) {
    // Helper property to get filtered accel as array
    val filteredAccel: FloatArray
        get() = floatArrayOf(filteredAccelX, filteredAccelY, filteredAccelZ)
    
    // Helper property to get device state as enum
    val deviceStateEnum: DeviceState
        get() = try { DeviceState.valueOf(deviceState) } catch (e: Exception) { DeviceState.UNKNOWN }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MotionData
        return id == other.id && sessionId == other.sessionId
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sessionId.hashCode()
        return result
    }
    
    companion object {
        fun create(
            sessionId: String,
            timestamp: Long = System.currentTimeMillis(),
            accelX: Float,
            accelY: Float,
            accelZ: Float,
            gyroX: Float,
            gyroY: Float,
            gyroZ: Float,
            pitch: Float,
            roll: Float,
            azimuth: Float,
            filteredAccel: FloatArray = floatArrayOf(0f, 0f, 0f),
            deviceState: DeviceState = DeviceState.UNKNOWN
        ) = MotionData(
            sessionId = sessionId,
            timestamp = timestamp,
            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,
            pitch = pitch,
            roll = roll,
            azimuth = azimuth,
            filteredAccelX = filteredAccel.getOrElse(0) { 0f },
            filteredAccelY = filteredAccel.getOrElse(1) { 0f },
            filteredAccelZ = filteredAccel.getOrElse(2) { 0f },
            deviceState = deviceState.name
        )
    }
}

enum class DeviceState {
    HELD_IN_HAND,       // User is holding the device
    ON_TABLE,           // Device is laying flat
    IN_POCKET,          // Device might be in pocket
    WALKING,            // User is walking while using
    STATIONARY,         // Device is not moving
    UNKNOWN
}

/**
 * ============================================
 * BEHAVIORAL SESSION
 * ============================================
 * Aggregates all behavioral data for a user session.
 */
@Entity(tableName = "behavioral_sessions")
data class BehavioralSession(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val isBaseline: Boolean = false,     // True if this is the baseline session (login)
    
    // Aggregated metrics (updated periodically)
    val avgKeystrokeDwellTime: Float = 0f,
    val avgKeystrokeFlightTime: Float = 0f,
    val avgTouchPressure: Float = 0f,
    val avgSwipeVelocity: Float = 0f,
    val avgDevicePitch: Float = 0f,
    val avgDeviceRoll: Float = 0f,
    
    // Standard deviations for comparison
    val stdKeystrokeDwellTime: Float = 0f,
    val stdKeystrokeFlightTime: Float = 0f,
    val stdTouchPressure: Float = 0f,
    
    // Risk tracking
    val currentRiskScore: Float = 0f,
    val riskAlertCount: Int = 0
)

/**
 * ============================================
 * RISK ASSESSMENT RESULT
 * ============================================
 * Result of behavioral analysis at a point in time.
 */
data class RiskAssessment(
    val timestamp: Long = System.currentTimeMillis(),
    val overallRiskScore: Float,         // 0.0 (safe) to 1.0 (high risk)
    val riskLevel: RiskLevel,
    val keystrokeDeviation: Float,       // Deviation from baseline
    val touchDeviation: Float,
    val motionDeviation: Float,
    val anomalies: List<BehavioralAnomaly>,
    val recommendation: SecurityRecommendation
)

enum class RiskLevel {
    LOW,      // Score 0.0 - 0.3: Normal behavior
    MEDIUM,   // Score 0.3 - 0.6: Some deviation detected
    HIGH,     // Score 0.6 - 0.8: Significant deviation
    CRITICAL  // Score 0.8 - 1.0: Likely session hijacking
}

enum class SecurityRecommendation {
    CONTINUE,           // No action needed
    SHOW_WARNING,       // Display a toast/warning
    REQUEST_REAUTHENTICATION,  // Ask user to re-authenticate
    FORCE_LOGOUT        // Immediately terminate session
}

/**
 * Models the severity of a security alert dialog.
 * Determines which actions the user is allowed to take.
 */
enum class AlertSeverity {
    CRITICAL,  // Non-dismissible, logout only
    HIGH,      // Must verify identity (re-auth)
    MEDIUM     // Warning, user may continue
}

data class BehavioralAnomaly(
    val type: AnomalyType,
    val severity: Float,
    val description: String
)

enum class AnomalyType {
    KEYSTROKE_SPEED_CHANGE,
    TYPING_RHYTHM_CHANGE,
    TOUCH_PRESSURE_CHANGE,
    SWIPE_PATTERN_CHANGE,
    DEVICE_ORIENTATION_CHANGE,
    DEVICE_STATE_CHANGE,
    UNUSUAL_INTERACTION_PATTERN
}

data class DebugRiskContribution(
    val signal: String,
    val deviation: Float,
    val weight: Float,
    val contribution: Float
)

data class DebugMetricComparison(
    val label: String,
    val baseline: String,
    val current: String,
    val deviation: String
)

data class DebugExplainabilityState(
    val sessionState: String = "Waiting for login",
    val decision: String = "No risk decision yet",
    val reason: String = "Behavioral collection has not produced an assessment.",
    val riskScore: Float = 0f,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val recommendation: SecurityRecommendation = SecurityRecommendation.CONTINUE,
    val zScoreRisk: Float = 0f,
    val mlRisk: Float? = null,
    val mlPrediction: String = "Not evaluated",
    val mlConfidence: Float? = null,
    val modelLoaded: Boolean = false,
    val enrollmentReady: Boolean = false,
    val expectedFeatureCount: Int = 0,
    val extractedFeatureCount: Int = 0,
    val contributions: List<DebugRiskContribution> = emptyList(),
    val comparisons: List<DebugMetricComparison> = emptyList(),
    val anomalies: List<BehavioralAnomaly> = emptyList(),
    val lastAssessmentTimestamp: Long? = null
)

data class DebugCollectionCounters(
    val keystrokes: Int = 0,
    val touches: Int = 0,
    val motionSamples: Int = 0,
    val databaseWrites: Int = 0,
    val lastEventTimestamp: Long? = null
)

/**
 * Type converter for TouchType enum
 */
class TouchTypeConverter {
    @TypeConverter
    fun fromTouchType(type: TouchType): String = type.name
    
    @TypeConverter
    fun toTouchType(value: String): TouchType = TouchType.valueOf(value)
}

