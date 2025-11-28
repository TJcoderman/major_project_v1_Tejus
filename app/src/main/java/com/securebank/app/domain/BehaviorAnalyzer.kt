package com.securebank.app.domain

import com.securebank.app.data.model.*
import com.securebank.app.data.repository.BehavioralRepository
import com.securebank.app.sensor.KeystrokeCollector
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * ============================================
 * BEHAVIOR ANALYZER - ANOMALY DETECTION ENGINE
 * ============================================
 * Compares current user behavior against baseline to detect anomalies.
 * Implements risk scoring algorithm for session hijacking detection.
 */
@Singleton
class BehaviorAnalyzer @Inject constructor(
    private val behavioralRepository: BehavioralRepository,
    private val keystrokeCollector: KeystrokeCollector
) {
    // Risk thresholds
    companion object {
        // Deviation thresholds (as percentage)
        const val LOW_THRESHOLD = 0.2f       // 20% deviation
        const val MEDIUM_THRESHOLD = 0.4f    // 40% deviation
        const val HIGH_THRESHOLD = 0.6f      // 60% deviation
        const val CRITICAL_THRESHOLD = 0.8f  // 80% deviation
        
        // Weight factors for different behavioral dimensions
        const val KEYSTROKE_WEIGHT = 0.35f
        const val TOUCH_WEIGHT = 0.30f
        const val MOTION_WEIGHT = 0.35f
        
        // Minimum samples required for reliable analysis
        const val MIN_KEYSTROKE_SAMPLES = 10
        const val MIN_TOUCH_SAMPLES = 5
        const val MIN_MOTION_SAMPLES = 20
        
        // Analysis window (analyze last N data points)
        const val ANALYSIS_WINDOW = 50
    }
    
    // Current risk state
    private val _currentRiskScore = MutableStateFlow(0f)
    val currentRiskScore: StateFlow<Float> = _currentRiskScore.asStateFlow()
    
    private val _currentRiskLevel = MutableStateFlow(RiskLevel.LOW)
    val currentRiskLevel: StateFlow<RiskLevel> = _currentRiskLevel.asStateFlow()
    
    private val _riskAssessment = MutableSharedFlow<RiskAssessment>(replay = 1)
    val riskAssessment: SharedFlow<RiskAssessment> = _riskAssessment.asSharedFlow()
    
    // Baseline metrics (from login)
    private var baselineKeystrokeDwell: Float = 0f
    private var baselineKeystrokeDwellStdDev: Float = 1f // Default non-zero to prevent division
    private var baselineKeystrokeFlight: Float = 0f
    private var baselineKeystrokeFlightStdDev: Float = 1f
    private var baselineTouchPressure: Float = 0f
    private var baselineSwipeVelocity: Float = 0f
    private var baselineDevicePitch: Float = 0f
    private var baselineDeviceRoll: Float = 0f
    private var baselineDeviceState: DeviceState = DeviceState.UNKNOWN
    
    // Session tracking
    private var currentSessionId: String = ""
    private var analysisCount = 0
    
    /**
     * Initializes the analyzer with baseline data from login.
     */
    suspend fun initializeBaseline(sessionId: String) {
        currentSessionId = sessionId
        analysisCount = 0
        
        // Get baseline keystroke metrics directly from collector memory
        val baselineKeystrokes = keystrokeCollector.getBaselineKeystrokes()
        
        if (baselineKeystrokes.isNotEmpty()) {
            // Calculate Mean
            baselineKeystrokeDwell = baselineKeystrokes.map { it.dwellTime }.average().toFloat()
            baselineKeystrokeFlight = baselineKeystrokes.map { it.flightTime }.average().toFloat()
            
            // Calculate Standard Deviation
            baselineKeystrokeDwellStdDev = calculateStdDev(baselineKeystrokes.map { it.dwellTime.toFloat() }, baselineKeystrokeDwell)
            baselineKeystrokeFlightStdDev = calculateStdDev(baselineKeystrokes.map { it.flightTime.toFloat() }, baselineKeystrokeFlight)
            
            // Enforce minimum StdDev to avoid division by zero or overly sensitive triggers
            baselineKeystrokeDwellStdDev = baselineKeystrokeDwellStdDev.coerceAtLeast(5f) 
            baselineKeystrokeFlightStdDev = baselineKeystrokeFlightStdDev.coerceAtLeast(5f)
        }
        
        // Set default baseline for touch and motion (will be updated as data comes in)
        baselineTouchPressure = 0.5f  // Default middle value
        baselineSwipeVelocity = 500f  // Default reasonable velocity
        baselineDevicePitch = 45f     // Default holding angle
        baselineDeviceRoll = 0f
        baselineDeviceState = DeviceState.HELD_IN_HAND
        
        _currentRiskScore.value = 0f
        _currentRiskLevel.value = RiskLevel.LOW
    }

    private fun calculateStdDev(values: List<Float>, mean: Float): Float {
        if (values.size < 2) return 0f
        val variance = values.map { 
            val diff = it - mean
            diff * diff 
        }.average()
        return sqrt(variance).toFloat()
    }
    
    /**
     * Updates baseline with initial touch/motion data after login.
     */
    suspend fun updateInitialBaseline(sessionId: String) {
        val (avgPressure, avgVelocity) = behavioralRepository.getAverageTouchMetrics(sessionId)
        val (avgPitch, avgRoll, commonState) = behavioralRepository.getAverageMotionMetrics(sessionId)
        
        if (avgPressure > 0) baselineTouchPressure = avgPressure
        if (avgVelocity > 0) baselineSwipeVelocity = avgVelocity
        if (avgPitch != 0f) baselineDevicePitch = avgPitch
        if (avgRoll != 0f) baselineDeviceRoll = avgRoll
        if (commonState != null) baselineDeviceState = commonState
    }
    
    /**
     * Performs a comprehensive risk assessment.
     * Should be called periodically (e.g., every 10-30 seconds).
     */
    suspend fun performRiskAssessment(sessionId: String): RiskAssessment {
        analysisCount++
        
        val anomalies = mutableListOf<BehavioralAnomaly>()
        
        // Analyze keystroke patterns
        val keystrokeDeviation = analyzeKeystrokePatterns(sessionId, anomalies)
        
        // Analyze touch patterns
        val touchDeviation = analyzeTouchPatterns(sessionId, anomalies)
        
        // Analyze motion patterns
        val motionDeviation = analyzeMotionPatterns(sessionId, anomalies)
        
        // Calculate weighted risk score
        val overallRiskScore = calculateWeightedRiskScore(
            keystrokeDeviation,
            touchDeviation,
            motionDeviation
        )
        
        // Determine risk level
        val riskLevel = determineRiskLevel(overallRiskScore)
        
        // Determine recommendation
        val recommendation = determineRecommendation(riskLevel, anomalies)
        
        // Create assessment
        val assessment = RiskAssessment(
            timestamp = System.currentTimeMillis(),
            overallRiskScore = overallRiskScore,
            riskLevel = riskLevel,
            keystrokeDeviation = keystrokeDeviation,
            touchDeviation = touchDeviation,
            motionDeviation = motionDeviation,
            anomalies = anomalies,
            recommendation = recommendation
        )
        
        // Update state
        _currentRiskScore.value = overallRiskScore
        _currentRiskLevel.value = riskLevel
        _riskAssessment.emit(assessment)
        
        return assessment
    }
    
    /**
     * Analyzes keystroke patterns and returns deviation score.
     * IMPROVED: Uses Z-Score (Standard Deviation) and emphasizes Flight Time.
     */
    private suspend fun analyzeKeystrokePatterns(
        sessionId: String,
        anomalies: MutableList<BehavioralAnomaly>
    ): Float {
        val (avgDwell, avgFlight) = behavioralRepository.getAverageKeystrokeMetrics(sessionId, false)
        
        // Skip if not enough baseline data
        if (baselineKeystrokeDwell <= 0 || avgDwell <= 0) {
            return 0f
        }
        
        // Calculate Z-Scores (Standard Deviations from mean)
        val dwellZScore = abs(avgDwell - baselineKeystrokeDwell) / baselineKeystrokeDwellStdDev
        val flightZScore = if (baselineKeystrokeFlight > 0) {
            abs(avgFlight - baselineKeystrokeFlight) / baselineKeystrokeFlightStdDev
        } else 0f
        
        // Weighted deviation: Flight Time is 3x more important than Dwell Time (due to software keyboard limitations)
        // Normalize Z-Score: 3.0 sigma = 1.0 (100%) deviation
        val normalizedDwellDev = (dwellZScore / 3.0f).coerceIn(0f, 1f)
        val normalizedFlightDev = (flightZScore / 3.0f).coerceIn(0f, 1f)
        
        val keystrokeDeviation = (normalizedDwellDev * 0.25f) + (normalizedFlightDev * 0.75f)
        
        // Record anomalies based on Z-Score
        if (dwellZScore > 3.0f) { // > 3 Standard Deviations
            anomalies.add(
                BehavioralAnomaly(
                    type = AnomalyType.KEYSTROKE_SPEED_CHANGE,
                    severity = normalizedDwellDev,
                    description = "Abnormal Dwell Time (Z-Score: %.1f)".format(dwellZScore)
                )
            )
        }
        
        if (flightZScore > 3.0f) { // > 3 Standard Deviations
            anomalies.add(
                BehavioralAnomaly(
                    type = AnomalyType.TYPING_RHYTHM_CHANGE,
                    severity = normalizedFlightDev,
                    description = "Abnormal Typing Rhythm (Z-Score: %.1f)".format(flightZScore)
                )
            )
        }
        
        return keystrokeDeviation
    }
    
    /**
     * Analyzes touch patterns and returns deviation score.
     */
    private suspend fun analyzeTouchPatterns(
        sessionId: String,
        anomalies: MutableList<BehavioralAnomaly>
    ): Float {
        val (avgPressure, avgVelocity) = behavioralRepository.getAverageTouchMetrics(sessionId)
        
        // Skip if no data
        if (avgPressure <= 0 && avgVelocity <= 0) {
            return 0f
        }
        
        var totalDeviation = 0f
        var deviationCount = 0
        
        // Pressure deviation
        if (avgPressure > 0 && baselineTouchPressure > 0) {
            val pressureDeviation = abs(avgPressure - baselineTouchPressure) / baselineTouchPressure
            totalDeviation += pressureDeviation
            deviationCount++
            
            if (pressureDeviation > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.TOUCH_PRESSURE_CHANGE,
                        severity = pressureDeviation.coerceAtMost(1f),
                        description = "Touch pressure deviation: ${(pressureDeviation * 100).toInt()}%"
                    )
                )
            }
        }
        
        // Swipe velocity deviation
        if (avgVelocity > 0 && baselineSwipeVelocity > 0) {
            val velocityDeviation = abs(avgVelocity - baselineSwipeVelocity) / baselineSwipeVelocity
            totalDeviation += velocityDeviation
            deviationCount++
            
            if (velocityDeviation > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.SWIPE_PATTERN_CHANGE,
                        severity = velocityDeviation.coerceAtMost(1f),
                        description = "Swipe velocity deviation: ${(velocityDeviation * 100).toInt()}%"
                    )
                )
            }
        }
        
        return if (deviationCount > 0) {
            (totalDeviation / deviationCount).coerceIn(0f, 1f)
        } else 0f
    }
    
    /**
     * Analyzes motion/sensor patterns and returns deviation score.
     */
    private suspend fun analyzeMotionPatterns(
        sessionId: String,
        anomalies: MutableList<BehavioralAnomaly>
    ): Float {
        val (avgPitch, avgRoll, currentState) = behavioralRepository.getAverageMotionMetrics(sessionId)
        
        var totalDeviation = 0f
        var deviationCount = 0
        
        // Orientation deviation (pitch and roll)
        if (avgPitch != 0f || avgRoll != 0f) {
            // Use absolute angle difference (max 180 degrees deviation possible)
            val pitchDeviation = abs(avgPitch - baselineDevicePitch) / 90f
            val rollDeviation = abs(avgRoll - baselineDeviceRoll) / 90f
            val orientationDeviation = (pitchDeviation + rollDeviation) / 2
            
            totalDeviation += orientationDeviation
            deviationCount++
            
            if (orientationDeviation > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.DEVICE_ORIENTATION_CHANGE,
                        severity = orientationDeviation.coerceAtMost(1f),
                        description = "Device orientation changed significantly"
                    )
                )
            }
        }
        
        // Device state change detection
        if (currentState != null && currentState != baselineDeviceState) {
            val stateChangeSeverity = when {
                baselineDeviceState == DeviceState.HELD_IN_HAND && currentState == DeviceState.ON_TABLE -> 0.1f
                baselineDeviceState == DeviceState.ON_TABLE && currentState == DeviceState.HELD_IN_HAND -> 0.1f
                currentState == DeviceState.WALKING && baselineDeviceState != DeviceState.WALKING -> 0.3f
                else -> 0.15f
            }
            
            totalDeviation += stateChangeSeverity
            deviationCount++
            
            if (stateChangeSeverity > 0.2f) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.DEVICE_STATE_CHANGE,
                        severity = stateChangeSeverity,
                        description = "Device state changed from ${baselineDeviceState.name} to ${currentState.name}"
                    )
                )
            }
        }
        
        return if (deviationCount > 0) {
            (totalDeviation / deviationCount).coerceIn(0f, 1f)
        } else 0f
    }
    
    /**
     * Calculates weighted risk score from individual deviations.
     */
    private fun calculateWeightedRiskScore(
        keystrokeDeviation: Float,
        touchDeviation: Float,
        motionDeviation: Float
    ): Float {
        // Apply exponential smoothing to prevent sudden jumps
        val rawScore = (keystrokeDeviation * KEYSTROKE_WEIGHT) +
                      (touchDeviation * TOUCH_WEIGHT) +
                      (motionDeviation * MOTION_WEIGHT)
        
        // Apply smoothing with previous score
        val smoothingFactor = 0.3f
        val smoothedScore = (smoothingFactor * rawScore) + 
                           ((1 - smoothingFactor) * _currentRiskScore.value)
        
        return smoothedScore.coerceIn(0f, 1f)
    }
    
    /**
     * Determines risk level from score.
     */
    private fun determineRiskLevel(score: Float): RiskLevel {
        return when {
            score >= CRITICAL_THRESHOLD -> RiskLevel.CRITICAL
            score >= HIGH_THRESHOLD -> RiskLevel.HIGH
            score >= MEDIUM_THRESHOLD -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    /**
     * Determines recommended action based on risk level and anomalies.
     */
    private fun determineRecommendation(
        riskLevel: RiskLevel,
        anomalies: List<BehavioralAnomaly>
    ): SecurityRecommendation {
        // Consider both risk level and anomaly count/severity
        val highSeverityAnomalies = anomalies.count { it.severity > HIGH_THRESHOLD }
        
        return when {
            riskLevel == RiskLevel.CRITICAL -> SecurityRecommendation.FORCE_LOGOUT
            riskLevel == RiskLevel.HIGH && highSeverityAnomalies >= 2 -> SecurityRecommendation.FORCE_LOGOUT
            riskLevel == RiskLevel.HIGH -> SecurityRecommendation.REQUEST_REAUTHENTICATION
            riskLevel == RiskLevel.MEDIUM -> SecurityRecommendation.SHOW_WARNING
            else -> SecurityRecommendation.CONTINUE
        }
    }
    
    /**
     * Quick risk check for immediate feedback.
     * Less comprehensive than full assessment, but faster.
     * IMPROVED: Triggers High Risk immediately if any single metric is massively off.
     */
    fun quickRiskCheck(
        recentKeystrokeDwell: Float,
        recentTouchPressure: Float,
        currentPitch: Float,
        currentRoll: Float
    ): RiskLevel {
        var maxZScore = 0f
        
        // Check Keystroke Z-Score
        if (baselineKeystrokeDwell > 0 && recentKeystrokeDwell > 0 && baselineKeystrokeDwellStdDev > 0) {
            val zScore = abs(recentKeystrokeDwell - baselineKeystrokeDwell) / baselineKeystrokeDwellStdDev
            if (zScore > maxZScore) maxZScore = zScore
        }
        
        // Check Touch Pressure (approximate Z-score assuming 20% variance as "normal" stddev)
        if (baselineTouchPressure > 0 && recentTouchPressure > 0) {
            val estimatedStdDev = baselineTouchPressure * 0.2f // Assume 20% natural variance
            val zScore = abs(recentTouchPressure - baselineTouchPressure) / estimatedStdDev
            if (zScore > maxZScore) maxZScore = zScore
        }
        
        // Check Device Orientation (Pitch)
        if (baselineDevicePitch != 0f) {
             val zScore = abs(currentPitch - baselineDevicePitch) / 15f // Assume 15 degrees std dev
             if (zScore > maxZScore) maxZScore = zScore
        }

        // Check Device Orientation (Roll)
        if (baselineDeviceRoll != 0f) {
             val zScore = abs(currentRoll - baselineDeviceRoll) / 15f // Assume 15 degrees std dev
             if (zScore > maxZScore) maxZScore = zScore
        }
        
        // Critical Logic: If any single metric is > 5 Sigma away, it's HIGH/CRITICAL
        return when {
            maxZScore > 5.0f -> RiskLevel.HIGH
            maxZScore > 3.0f -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }
    
    /**
     * Resets the analyzer state (call on logout).
     */
    fun reset() {
        currentSessionId = ""
        analysisCount = 0
        baselineKeystrokeDwell = 0f
        baselineKeystrokeFlight = 0f
        baselineTouchPressure = 0.5f
        baselineSwipeVelocity = 500f
        baselineDevicePitch = 45f
        baselineDeviceRoll = 0f
        baselineDeviceState = DeviceState.UNKNOWN
        _currentRiskScore.value = 0f
        _currentRiskLevel.value = RiskLevel.LOW
    }
}

