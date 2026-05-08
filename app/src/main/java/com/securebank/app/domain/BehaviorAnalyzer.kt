package com.securebank.app.domain

import android.util.Log
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
 * Implements a HYBRID approach:
 *   1. Z-score based statistical deviation (original system)
 *   2. ML model inference (trained MLP neural network)
 * Both signals are combined for more accurate detection.
 */
@Singleton
class BehaviorAnalyzer @Inject constructor(
    private val behavioralRepository: BehavioralRepository,
    private val keystrokeCollector: KeystrokeCollector,
    private val mlModelInference: MLModelInference,
    private val mlFeatureExtractor: MLFeatureExtractor
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

        // ML model blending weight (0.0 = Z-score only, 1.0 = ML only)
        // 0.6 gives ML majority since it was trained on richer feature set
        const val ML_WEIGHT = 0.6f

        private const val TAG = "BehaviorAnalyzer"
    }
    
    // Current risk state
    private val _currentRiskScore = MutableStateFlow(0f)
    val currentRiskScore: StateFlow<Float> = _currentRiskScore.asStateFlow()
    
    private val _currentRiskLevel = MutableStateFlow(RiskLevel.LOW)
    val currentRiskLevel: StateFlow<RiskLevel> = _currentRiskLevel.asStateFlow()
    
    private val _riskAssessment = MutableSharedFlow<RiskAssessment>(replay = 1)
    val riskAssessment: SharedFlow<RiskAssessment> = _riskAssessment.asSharedFlow()

    private val _mlReadyState = MutableStateFlow(false)
    val mlReadyState: StateFlow<Boolean> = _mlReadyState.asStateFlow()

    private val _debugExplainabilityState = MutableStateFlow(DebugExplainabilityState())
    val debugExplainabilityState: StateFlow<DebugExplainabilityState> = _debugExplainabilityState.asStateFlow()
    
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
    private var latestKeystrokeDwell: Float = 0f
    private var latestKeystrokeFlight: Float = 0f
    private var latestTouchPressure: Float = 0f
    private var latestSwipeVelocity: Float = 0f
    private var latestDevicePitch: Float = 0f
    private var latestDeviceRoll: Float = 0f
    private var latestDeviceState: DeviceState? = null
    
    // Session tracking
    private var currentSessionId: String = ""
    private var analysisCount = 0

    // ML model state
    private var mlModelLoaded = false
    private var mlEnrollmentReady = false
    private var lastMLPrediction = "Not evaluated"
    private var lastMLConfidence: Float? = null
    private var lastMLRisk: Float? = null
    private var lastExtractedFeatureCount = 0
    
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

        // Initialize ML model
        initializeMLModel()
        publishDebugState(
            sessionState = "Baseline initialized",
            decision = "Monitoring started",
            reason = "Login keystroke baseline is ready. Touch and motion baselines will update after live data arrives.",
            riskScore = 0f,
            riskLevel = RiskLevel.LOW,
            recommendation = SecurityRecommendation.CONTINUE
        )
    }

    /**
     * Loads the ML model and prepares it for inference.
     * Called automatically during baseline initialization.
     */
    private fun initializeMLModel() {
        if (!mlModelLoaded) {
            mlModelLoaded = mlModelInference.loadModel()
            if (mlModelLoaded) {
                Log.d(TAG, "ML model loaded successfully (${mlModelInference.getExpectedFeatureCount()} features)")
            } else {
                Log.w(TAG, "ML model failed to load - falling back to Z-score only")
            }
        }
        updateMLReadyState()
    }

    /**
     * Sets the ML enrollment baseline from PIN entry data collected during login.
     * Call this after the user successfully enters their PIN.
     *
     * @param pinKeystrokes PIN keystroke events from the login attempt
     * @param touches Touch data collected during login
     * @param motionData Motion sensor data collected during login
     */
    fun setMLEnrollmentBaseline(
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>
    ) {
        if (!mlModelLoaded) {
            updateMLReadyState()
            return
        }

        mlFeatureExtractor.setEnrollmentBaseline(pinKeystrokes, touches, motionData)
        mlEnrollmentReady = mlFeatureExtractor.hasEnrollmentBaseline()

        if (mlEnrollmentReady) {
            Log.d(TAG, "ML enrollment baseline set (${pinKeystrokes.size} keystrokes, ${touches.size} touches, ${motionData.size} motion samples)")
        } else {
            Log.w(TAG, "ML enrollment baseline could not be established - insufficient data")
        }
        updateMLReadyState()
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

        publishDebugState(
            sessionState = "Monitoring",
            decision = "Touch and motion baseline updated",
            reason = "Initial live touch and motion samples have been folded into the session baseline.",
            riskScore = _currentRiskScore.value,
            riskLevel = _currentRiskLevel.value,
            recommendation = SecurityRecommendation.CONTINUE
        )
    }
    
    /**
     * Performs a comprehensive risk assessment.
     * Should be called periodically (e.g., every 10-30 seconds).
     *
     * HYBRID approach:
     *   1. Z-score deviation analysis (original system)
     *   2. ML model inference (if model + enrollment are ready)
     *   3. Weighted combination: (1-ML_WEIGHT)*Z-score + ML_WEIGHT*ML
     */
    suspend fun performRiskAssessment(sessionId: String): RiskAssessment {
        analysisCount++
        lastMLPrediction = "Not evaluated"
        lastMLConfidence = null
        lastMLRisk = null
        lastExtractedFeatureCount = 0

        val anomalies = mutableListOf<BehavioralAnomaly>()

        // ── Z-Score Analysis ──
        val keystrokeDeviation = analyzeKeystrokePatterns(sessionId, anomalies)
        val touchDeviation = analyzeTouchPatterns(sessionId, anomalies)
        val motionDeviation = analyzeMotionPatterns(sessionId, anomalies)

        val zScoreRisk = calculateWeightedRiskScore(
            keystrokeDeviation,
            touchDeviation,
            motionDeviation
        )

        // ── ML Model Analysis ──
        val mlRisk = performMLAssessment(sessionId, anomalies)

        // ── Blend Scores ──
        val overallRiskScore = if (mlRisk != null) {
            // Hybrid: blend Z-score and ML predictions
            val blended = ((1f - ML_WEIGHT) * zScoreRisk) + (ML_WEIGHT * mlRisk)
            blended.coerceIn(0f, 1f)
        } else {
            // Fallback: Z-score only
            zScoreRisk
        }

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
        publishDebugState(
            sessionState = "Risk assessment #$analysisCount",
            decision = recommendation.name.replace('_', ' '),
            reason = buildDecisionReason(riskLevel, recommendation, anomalies),
            riskScore = overallRiskScore,
            riskLevel = riskLevel,
            recommendation = recommendation,
            keystrokeDeviation = keystrokeDeviation,
            touchDeviation = touchDeviation,
            motionDeviation = motionDeviation,
            zScoreRisk = zScoreRisk,
            mlRisk = mlRisk,
            anomalies = anomalies,
            timestamp = assessment.timestamp
        )
        _riskAssessment.emit(assessment)

        return assessment
    }

    /**
     * Runs the ML model on current session data vs enrollment baseline.
     * Returns an impostor risk score (0.0 = genuine, 1.0 = impostor),
     * or null if ML is not available.
     *
     * NOTE: The banking demo does not use CustomPinPad for login, so real PIN
     * keystroke features are unavailable. ML enrollment requires the experiment
     * module. When mlEnrollmentReady is false, this returns null and the system
     * falls back to statistical Z-score detection only.
     */
    private suspend fun performMLAssessment(
        sessionId: String,
        anomalies: MutableList<BehavioralAnomaly>
    ): Float? {
        if (!mlModelLoaded || !mlEnrollmentReady) return null

        // Gather current session data for ML feature extraction
        val recentTouches = behavioralRepository.getRecentTouches(sessionId, 200)
        val recentMotion = behavioralRepository.getRecentMotion(sessionId, 500)

        // Use recent non-baseline keystrokes from the DB as "current" session data.
        // Previously this incorrectly used getBaselineKeystrokes() which compared
        // enrollment data against itself.
        val recentKeystrokes = behavioralRepository.getRecentKeystrokeMetrics(
            sessionId, ANALYSIS_WINDOW, MIN_KEYSTROKE_SAMPLES
        )
        // If no recent keystrokes, we cannot compute deviation features for ML
        if (recentKeystrokes == null || recentMotion.size < 50) return null

        // Build synthetic PinKeystrokeEvent from recent non-baseline keystrokes
        // for feature extraction compatibility. These are typed during banking use.
        val recentKeystrokeData = behavioralRepository.getRecentKeystrokeData(
            sessionId, ANALYSIS_WINDOW
        )
        val pinKeystrokes = recentKeystrokeData.mapIndexed { index, ks ->
            PinKeystrokeEvent(
                sessionId = sessionId,
                timestamp = ks.timestamp,
                digit = index % 10,
                keyDownTime = ks.timestamp,
                keyUpTime = ks.timestamp + ks.dwellTime,
                dwellTime = ks.dwellTime,
                flightTime = ks.flightTime,
                touchX = 0f,
                touchY = 0f,
                touchSize = 0f,
                pinAttemptNumber = 1
            )
        }

        if (pinKeystrokes.size < 6) return null

        val featureOrder = mlModelInference.getFeatureNames()
        val features = mlFeatureExtractor.computeDeviationFeatures(
            pinKeystrokes, recentTouches, recentMotion, featureOrder
        ) ?: return null
        lastExtractedFeatureCount = features.size

        // classify() now returns null on model failure instead of fail-open
        val classifyResult = mlModelInference.classify(features) ?: run {
            lastMLPrediction = "Unavailable"
            lastMLConfidence = null
            return null
        }
        val (isGenuine, confidence) = classifyResult
        lastMLPrediction = if (isGenuine) "Genuine" else "Impostor"
        lastMLConfidence = confidence

        // Convert to risk score: genuine=low risk, impostor=high risk
        val mlRiskScore = if (isGenuine) {
            (1f - confidence).coerceIn(0f, 0.3f) // Genuine with confidence -> low risk
        } else {
            confidence.coerceIn(0.5f, 1f) // Impostor with confidence -> high risk
        }
        lastMLRisk = mlRiskScore

        // Add anomaly if ML detects impostor
        if (!isGenuine && confidence > 0.7f) {
            anomalies.add(
                BehavioralAnomaly(
                    type = AnomalyType.TYPING_RHYTHM_CHANGE,
                    severity = mlRiskScore,
                    description = "ML model: impostor detected (confidence: %.0f%%)".format(confidence * 100)
                )
            )
        }

        Log.d(TAG, "ML assessment: genuine=$isGenuine, confidence=${"%.2f".format(confidence)}, risk=${"%.3f".format(mlRiskScore)}")
        return mlRiskScore
    }
    
    /**
     * Analyzes keystroke patterns and returns deviation score.
     * IMPROVED: Uses Z-Score (Standard Deviation) and emphasizes Flight Time.
     */
    private suspend fun analyzeKeystrokePatterns(
        sessionId: String,
        anomalies: MutableList<BehavioralAnomaly>
    ): Float {
        // Use recent windowed metrics with minimum sample gate
        val recentMetrics = behavioralRepository.getRecentKeystrokeMetrics(
            sessionId, ANALYSIS_WINDOW, MIN_KEYSTROKE_SAMPLES
        )
        if (recentMetrics == null) {
            // Insufficient data — mark as unavailable, do not contribute deviation
            return 0f
        }
        val (avgDwell, avgFlight) = recentMetrics
        latestKeystrokeDwell = avgDwell
        latestKeystrokeFlight = avgFlight
        
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
        // Use recent windowed metrics with minimum sample gate
        val recentMetrics = behavioralRepository.getRecentTouchMetrics(
            sessionId, ANALYSIS_WINDOW, MIN_TOUCH_SAMPLES
        )
        if (recentMetrics == null) {
            // Insufficient data — mark as unavailable, do not contribute deviation
            return 0f
        }
        val (avgPressure, avgVelocity) = recentMetrics
        latestTouchPressure = avgPressure
        latestSwipeVelocity = avgVelocity
        
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
        // Use recent windowed metrics with minimum sample gate
        val recentMetrics = behavioralRepository.getRecentMotionMetrics(
            sessionId, ANALYSIS_WINDOW, MIN_MOTION_SAMPLES
        )
        if (recentMetrics == null) {
            // Insufficient data — mark as unavailable, do not contribute deviation
            return 0f
        }
        val (avgPitch, avgRoll, currentState) = recentMetrics
        latestDevicePitch = avgPitch
        latestDeviceRoll = avgRoll
        latestDeviceState = currentState
        
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
     * Processes real-time motion data to update risk score immediately.
     */
    fun processRealTimeMotion(motionData: MotionData): SecurityRecommendation {
        // Calculate immediate deviation (Z-Score based)
        var maxZScore = 0f
        
        if (baselineDevicePitch != 0f) {
             val zScore = abs(motionData.pitch - baselineDevicePitch) / 15f
             if (zScore > maxZScore) maxZScore = zScore
        }

        if (baselineDeviceRoll != 0f) {
             val zScore = abs(motionData.roll - baselineDeviceRoll) / 15f
             if (zScore > maxZScore) maxZScore = zScore
        }
        
        // Convert Z-Score to Risk Score (0.0 - 1.0)
        // 3 Sigma = 1.0 (High Risk)
        val instantaneousRisk = (maxZScore / 3.0f).coerceIn(0f, 1f)
        
        // Smooth update of current score
        // Use a faster smoothing factor (0.1) for responsiveness
        val smoothedScore = (0.1f * instantaneousRisk) + (0.9f * _currentRiskScore.value)
        
        _currentRiskScore.value = smoothedScore
        _currentRiskLevel.value = determineRiskLevel(smoothedScore)
        
        return if (_currentRiskLevel.value == RiskLevel.CRITICAL || _currentRiskLevel.value == RiskLevel.HIGH) {
            // Trigger immediate recommendation
             val anomalies = listOf(
                 BehavioralAnomaly(
                     type = AnomalyType.DEVICE_ORIENTATION_CHANGE,
                     severity = instantaneousRisk,
                     description = "Real-time orientation anomaly (Z: %.1f)".format(maxZScore)
                 )
             )
            determineRecommendation(_currentRiskLevel.value, anomalies)
        } else {
            SecurityRecommendation.CONTINUE
        }
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

    fun applyDebugScenario(label: String, riskScore: Float) {
        val safeScore = riskScore.coerceIn(0f, 1f)
        val riskLevel = determineRiskLevel(safeScore)
        val anomaly = BehavioralAnomaly(
            type = AnomalyType.UNUSUAL_INTERACTION_PATTERN,
            severity = safeScore,
            description = "Demo scenario: $label"
        )
        val anomalies = if (safeScore > LOW_THRESHOLD) listOf(anomaly) else emptyList()
        val recommendation = determineRecommendation(riskLevel, anomalies)

        _currentRiskScore.value = safeScore
        _currentRiskLevel.value = riskLevel
        publishDebugState(
            sessionState = "Demo simulation",
            decision = recommendation.name.replace('_', ' '),
            reason = "Reviewer demo override applied: $label.",
            riskScore = safeScore,
            riskLevel = riskLevel,
            recommendation = recommendation,
            keystrokeDeviation = if ("typing" in label.lowercase() || "critical" in label.lowercase()) safeScore else 0f,
            touchDeviation = if ("touch" in label.lowercase() || "critical" in label.lowercase()) safeScore else 0f,
            motionDeviation = if ("motion" in label.lowercase() || "critical" in label.lowercase()) safeScore else 0f,
            zScoreRisk = safeScore,
            mlRisk = null,
            anomalies = anomalies,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun publishDebugState(
        sessionState: String,
        decision: String,
        reason: String,
        riskScore: Float,
        riskLevel: RiskLevel,
        recommendation: SecurityRecommendation,
        keystrokeDeviation: Float = 0f,
        touchDeviation: Float = 0f,
        motionDeviation: Float = 0f,
        zScoreRisk: Float = 0f,
        mlRisk: Float? = lastMLRisk,
        anomalies: List<BehavioralAnomaly> = emptyList(),
        timestamp: Long? = null
    ) {
        _debugExplainabilityState.value = DebugExplainabilityState(
            sessionState = sessionState,
            decision = decision,
            reason = reason,
            riskScore = riskScore,
            riskLevel = riskLevel,
            recommendation = recommendation,
            zScoreRisk = zScoreRisk,
            mlRisk = mlRisk,
            mlPrediction = lastMLPrediction,
            mlConfidence = lastMLConfidence,
            modelLoaded = mlModelLoaded,
            enrollmentReady = mlEnrollmentReady,
            expectedFeatureCount = mlModelInference.getExpectedFeatureCount(),
            extractedFeatureCount = lastExtractedFeatureCount,
            contributions = listOf(
                DebugRiskContribution("Keystroke", keystrokeDeviation, KEYSTROKE_WEIGHT, keystrokeDeviation * KEYSTROKE_WEIGHT),
                DebugRiskContribution("Touch", touchDeviation, TOUCH_WEIGHT, touchDeviation * TOUCH_WEIGHT),
                DebugRiskContribution("Motion", motionDeviation, MOTION_WEIGHT, motionDeviation * MOTION_WEIGHT)
            ),
            comparisons = buildMetricComparisons(),
            anomalies = anomalies,
            lastAssessmentTimestamp = timestamp
        )
    }

    private fun buildMetricComparisons(): List<DebugMetricComparison> {
        return listOf(
            DebugMetricComparison(
                label = "Keystroke dwell",
                baseline = formatMs(baselineKeystrokeDwell),
                current = formatMs(latestKeystrokeDwell),
                deviation = formatRelativeDeviation(latestKeystrokeDwell, baselineKeystrokeDwell)
            ),
            DebugMetricComparison(
                label = "Keystroke flight",
                baseline = formatMs(baselineKeystrokeFlight),
                current = formatMs(latestKeystrokeFlight),
                deviation = formatRelativeDeviation(latestKeystrokeFlight, baselineKeystrokeFlight)
            ),
            DebugMetricComparison(
                label = "Touch pressure",
                baseline = "%.2f".format(baselineTouchPressure),
                current = "%.2f".format(latestTouchPressure),
                deviation = formatRelativeDeviation(latestTouchPressure, baselineTouchPressure)
            ),
            DebugMetricComparison(
                label = "Swipe velocity",
                baseline = "%.0f px/s".format(baselineSwipeVelocity),
                current = "%.0f px/s".format(latestSwipeVelocity),
                deviation = formatRelativeDeviation(latestSwipeVelocity, baselineSwipeVelocity)
            ),
            DebugMetricComparison(
                label = "Device pitch",
                baseline = "%.1f deg".format(baselineDevicePitch),
                current = "%.1f deg".format(latestDevicePitch),
                deviation = "%+.1f deg".format(latestDevicePitch - baselineDevicePitch)
            ),
            DebugMetricComparison(
                label = "Device roll",
                baseline = "%.1f deg".format(baselineDeviceRoll),
                current = "%.1f deg".format(latestDeviceRoll),
                deviation = "%+.1f deg".format(latestDeviceRoll - baselineDeviceRoll)
            ),
            DebugMetricComparison(
                label = "Device state",
                baseline = baselineDeviceState.name,
                current = latestDeviceState?.name ?: "UNKNOWN",
                deviation = if (latestDeviceState == baselineDeviceState) "same" else "changed"
            )
        )
    }

    private fun buildDecisionReason(
        riskLevel: RiskLevel,
        recommendation: SecurityRecommendation,
        anomalies: List<BehavioralAnomaly>
    ): String {
        if (anomalies.isEmpty()) {
            return "All monitored signals are close enough to the enrolled baseline. Recommended action: ${recommendation.name.replace('_', ' ')}."
        }
        val topReasons = anomalies
            .sortedByDescending { it.severity }
            .take(3)
            .joinToString("; ") { it.description }
        return "$riskLevel risk because $topReasons. Recommended action: ${recommendation.name.replace('_', ' ')}."
    }

    private fun formatMs(value: Float): String = if (value > 0f) "%.0f ms".format(value) else "--"

    private fun formatRelativeDeviation(current: Float, baseline: Float): String {
        if (baseline <= 0f || current <= 0f) return "--"
        val relative = ((current - baseline) / baseline) * 100f
        return "%+.0f%%".format(relative)
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
        latestKeystrokeDwell = 0f
        latestKeystrokeFlight = 0f
        latestTouchPressure = 0f
        latestSwipeVelocity = 0f
        latestDevicePitch = 0f
        latestDeviceRoll = 0f
        latestDeviceState = null
        _currentRiskScore.value = 0f
        _currentRiskLevel.value = RiskLevel.LOW

        // Clear ML state
        mlFeatureExtractor.clearBaseline()
        mlEnrollmentReady = false
        lastMLPrediction = "Not evaluated"
        lastMLConfidence = null
        lastMLRisk = null
        lastExtractedFeatureCount = 0
        updateMLReadyState()
        _debugExplainabilityState.value = DebugExplainabilityState(
            modelLoaded = mlModelLoaded,
            expectedFeatureCount = mlModelInference.getExpectedFeatureCount()
        )
    }

    /**
     * Returns whether the ML model is loaded and enrollment baseline is set.
     */
    fun isMLReady(): Boolean = mlModelLoaded && mlEnrollmentReady

    private fun updateMLReadyState() {
        _mlReadyState.value = isMLReady()
    }
}

