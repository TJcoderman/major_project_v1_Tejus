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
        const val MIN_PROFILE_ML_SAMPLES = 18
        const val TOUCH_TRUST_RAMP_SAMPLES = 8
        const val TOUCH_TRUST_RAMP_MS = 6000L
        const val MOTION_CRITICAL_ORIENTATION_DEGREES = 65f
        const val MOTION_CRITICAL_DELTA_DEGREES = 50f
        const val MOTION_CRITICAL_GYRO_RAD_PER_SEC = 3.5f
        const val MOTION_HIGH_ORIENTATION_DEGREES = 35f
        const val MOTION_HIGH_DELTA_DEGREES = 30f
        const val MOTION_HIGH_GYRO_RAD_PER_SEC = 2.0f

        // ML model blending weight (0.0 = Z-score only, 1.0 = ML only)
        // 0.6 gives ML majority since it was trained on richer feature set
        const val ML_WEIGHT = 0.6f

        private const val TAG = "BehaviorAnalyzer"

        internal data class RealTimeMotionEvaluation(
            val riskScore: Float,
            val isHighSpike: Boolean,
            val isCriticalSpike: Boolean
        )

        internal fun evaluateRealTimeMotion(
            motionData: MotionData,
            previousMotionData: MotionData?,
            baselinePitch: Float,
            baselineRoll: Float,
            baselineGyroMagnitudeMean: Float,
            baselineGyroMagnitudeStdDev: Float
        ): RealTimeMotionEvaluation {
            val pitchDeviation = abs(motionData.pitch - baselinePitch)
            val rollDeviation = abs(motionData.roll - baselineRoll)
            val orientationDeviation = maxOf(pitchDeviation, rollDeviation)
            val sampleDelta = previousMotionData?.let {
                maxOf(
                    abs(motionData.pitch - it.pitch),
                    abs(motionData.roll - it.roll)
                )
            } ?: 0f
            val gyroMagnitude = sqrt(
                motionData.gyroX * motionData.gyroX +
                    motionData.gyroY * motionData.gyroY +
                    motionData.gyroZ * motionData.gyroZ
            )

            val highGyroLimit = maxOf(
                MOTION_HIGH_GYRO_RAD_PER_SEC,
                baselineGyroMagnitudeMean + baselineGyroMagnitudeStdDev * 6f
            )
            val criticalGyroLimit = maxOf(
                MOTION_CRITICAL_GYRO_RAD_PER_SEC,
                baselineGyroMagnitudeMean + baselineGyroMagnitudeStdDev * 12f
            )

            val isCriticalSpike = orientationDeviation >= MOTION_CRITICAL_ORIENTATION_DEGREES ||
                sampleDelta >= MOTION_CRITICAL_DELTA_DEGREES ||
                gyroMagnitude >= criticalGyroLimit
            val isHighSpike = isCriticalSpike ||
                orientationDeviation >= MOTION_HIGH_ORIENTATION_DEGREES ||
                sampleDelta >= MOTION_HIGH_DELTA_DEGREES ||
                gyroMagnitude >= highGyroLimit

            val orientationRisk = orientationDeviation / MOTION_CRITICAL_ORIENTATION_DEGREES
            val deltaRisk = sampleDelta / MOTION_CRITICAL_DELTA_DEGREES
            val gyroRisk = gyroMagnitude / criticalGyroLimit
            val riskScore = maxOf(orientationRisk, deltaRisk, gyroRisk).coerceIn(0f, 1f)

            return RealTimeMotionEvaluation(
                riskScore = riskScore,
                isHighSpike = isHighSpike,
                isCriticalSpike = isCriticalSpike
            )
        }
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
    private var baselineTouchPressureStdDev: Float = 0.1f
    private var baselineTouchArea: Float = 0f
    private var baselineTouchAreaStdDev: Float = 0.1f
    private var baselineTouchDuration: Float = 150f
    private var baselineTouchDurationStdDev: Float = 60f
    private var baselineHoldDuration: Float = 0f
    private var baselineHoldDurationStdDev: Float = 80f
    private var baselineSwipeVelocity: Float = 0f
    private var baselineSwipeVelocityStdDev: Float = 150f
    private var baselineSwipeAcceleration: Float = 0f
    private var baselineSwipeAccelerationStdDev: Float = 150f
    private var baselineTapRatio: Float = 0f
    private var baselineSwipeRatio: Float = 0f
    private var baselineLongPressRatio: Float = 0f
    private var baselineDevicePitch: Float = 0f
    private var baselineDeviceRoll: Float = 0f
    private var baselineGyroMagnitudeMean: Float = 0.15f
    private var baselineGyroMagnitudeStdDev: Float = 0.08f
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
    private var isProfileBased = false  // True when baseline comes from enrollment profile
    private var consecutiveAbnormalTouches = 0
    private var consecutiveHighPressureTouches = 0
    private var consecutiveLongPressTouches = 0
    private var sessionStartedAt: Long = 0L
    private var realTimeTouchSamples = 0
    private var saturatedPressureSamples = 0
    private var previousMotionData: MotionData? = null

    // ML model state
    private var mlModelLoaded = false
    private var mlEnrollmentReady = false
    private var lastMLPrediction = "Not evaluated"
    private var lastMLConfidence: Float? = null
    private var lastMLRisk: Float? = null
    private var lastExtractedFeatureCount = 0
    
    /**
     * Initializes the analyzer with baseline data.
     * If a BehavioralProfile is provided, uses stored enrollment values.
     * Otherwise, falls back to login keystroke + default heuristics.
     */
    suspend fun initializeBaseline(sessionId: String, profile: BehavioralProfile? = null) {
        currentSessionId = sessionId
        analysisCount = 0
        sessionStartedAt = System.currentTimeMillis()
        realTimeTouchSamples = 0
        saturatedPressureSamples = 0
        consecutiveAbnormalTouches = 0
        consecutiveHighPressureTouches = 0
        consecutiveLongPressTouches = 0
        previousMotionData = null

        if (profile != null) {
            // ── Profile-based initialization (from enrollment) ──
            loadUserProfile(profile)
            isProfileBased = true
            Log.d(TAG, "Baseline initialized from enrolled profile (${profile.sampleCount} samples)")
        } else {
            // ── Provisional baseline (legacy: from login keystrokes) ──
            isProfileBased = false
            val baselineKeystrokes = keystrokeCollector.getBaselineKeystrokes()

            if (baselineKeystrokes.isNotEmpty()) {
                baselineKeystrokeDwell = baselineKeystrokes.map { it.dwellTime }.average().toFloat()
                baselineKeystrokeFlight = baselineKeystrokes.map { it.flightTime }.average().toFloat()
                baselineKeystrokeDwellStdDev = calculateStdDev(baselineKeystrokes.map { it.dwellTime.toFloat() }, baselineKeystrokeDwell)
                baselineKeystrokeFlightStdDev = calculateStdDev(baselineKeystrokes.map { it.flightTime.toFloat() }, baselineKeystrokeFlight)
                baselineKeystrokeDwellStdDev = baselineKeystrokeDwellStdDev.coerceAtLeast(5f)
                baselineKeystrokeFlightStdDev = baselineKeystrokeFlightStdDev.coerceAtLeast(5f)
            }

            // Set default baseline for touch and motion
            baselineTouchPressure = 0.5f
            baselineSwipeVelocity = 500f
            baselineDevicePitch = 45f
            baselineDeviceRoll = 0f
            baselineGyroMagnitudeMean = 0.15f
            baselineGyroMagnitudeStdDev = 0.08f
            baselineDeviceState = DeviceState.HELD_IN_HAND
        }

        _currentRiskScore.value = 0f
        _currentRiskLevel.value = RiskLevel.LOW

        // Initialize ML model
        initializeMLModel()
        if (profile != null && mlModelLoaded) {
            if (profile.isUsableForML()) {
                mlFeatureExtractor.setEnrollmentBaseline(profile)
                mlEnrollmentReady = mlFeatureExtractor.hasEnrollmentBaseline()
                lastMLPrediction = "Ready"
            } else {
                mlFeatureExtractor.clearBaseline()
                mlEnrollmentReady = false
                lastMLPrediction = "Enrollment profile too sparse"
            }
            updateMLReadyState()
        }

        val baselineType = if (isProfileBased) "Enrolled profile" else "Provisional (login)"
        publishDebugState(
            sessionState = "Baseline initialized ($baselineType)",
            decision = "Monitoring started",
            reason = if (isProfileBased)
                "Using enrolled behavioral profile. Calibrated modalities are active; ML is gated by profile quality."
            else
                "Login keystroke baseline is ready. Touch and motion baselines will update after live data arrives.",
            riskScore = 0f,
            riskLevel = RiskLevel.LOW,
            recommendation = SecurityRecommendation.CONTINUE
        )
    }

    /**
     * Loads baseline values from a stored BehavioralProfile (from enrollment).
     * This provides much better accuracy than the provisional defaults.
     */
    private fun loadUserProfile(profile: BehavioralProfile) {
        // Keystroke
        baselineKeystrokeDwell = if (profile.pinDwellMean > 0f) profile.pinDwellMean else 0f
        baselineKeystrokeFlight = if (profile.pinFlightMean > 0f) profile.pinFlightMean else 0f
        baselineKeystrokeDwellStdDev = profile.pinDwellStd.coerceAtLeast(5f)
        baselineKeystrokeFlightStdDev = profile.pinFlightStd.coerceAtLeast(5f)

        // Touch
        baselineTouchPressure = if (profile.pressureMean > 0f) profile.pressureMean else 0.5f
        baselineTouchPressureStdDev = profile.pressureStd.coerceAtLeast(0.05f)
        baselineTouchArea = if (profile.touchAreaMean > 0f) profile.touchAreaMean else 1f
        baselineTouchAreaStdDev = profile.touchAreaStd.coerceAtLeast(0.05f)
        baselineTouchDuration = if (profile.durationMean > 0f) profile.durationMean else 150f
        baselineTouchDurationStdDev = profile.durationStd.coerceAtLeast(40f)
        baselineHoldDuration = profile.holdDurationMean
        baselineHoldDurationStdDev = profile.holdDurationStd.coerceAtLeast(60f)
        baselineSwipeVelocity = if (profile.velocityMean > 0f) profile.velocityMean else 500f
        baselineSwipeVelocityStdDev = profile.velocityStd.coerceAtLeast(100f)
        baselineSwipeAcceleration = profile.accelerationMean
        baselineSwipeAccelerationStdDev = profile.accelerationStd.coerceAtLeast(100f)
        baselineTapRatio = profile.tapRatio
        baselineSwipeRatio = profile.swipeRatio
        baselineLongPressRatio = profile.longPressRatio

        // Motion
        baselineDevicePitch = if (profile.pitchMean != 0f) profile.pitchMean else 45f
        baselineDeviceRoll = profile.rollMean
        baselineGyroMagnitudeMean = profile.gyroMagnitudeMean.coerceAtLeast(0.05f)
        baselineGyroMagnitudeStdDev = profile.gyroMagnitudeStd.coerceAtLeast(0.05f)
        baselineDeviceState = try {
            DeviceState.valueOf(profile.baselineDeviceState)
        } catch (_: Exception) {
            DeviceState.HELD_IN_HAND
        }
    }

    private fun BehavioralProfile.isUsableForML(): Boolean =
        sampleCount >= MIN_PROFILE_ML_SAMPLES &&
            pinDwellMean > 0f &&
            pressureMean > 0f &&
            durationMean > 0f

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
    suspend fun updateInitialBaseline(sessionId: String): Boolean {
        if (isProfileBased) {
            publishDebugState(
                sessionState = "Monitoring",
                decision = "Enrolled baseline preserved",
                reason = "Using signup enrollment profile. Live protected-session data is not folded into the baseline.",
                riskScore = _currentRiskScore.value,
                riskLevel = _currentRiskLevel.value,
                recommendation = SecurityRecommendation.CONTINUE
            )
            return false
        }

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
        return true
    }

    /**
     * Process a single touch event in real-time against the enrollment baseline.
     * Provides immediate anomaly feedback before the periodic assessment runs.
     *
     * Scores pressure, touch area, duration, hold duration, velocity,
     * acceleration, and gesture mix against the enrolled profile.
     *
     * @return SecurityRecommendation for immediate action.
     */
    fun processRealTimeTouch(touchData: TouchData): SecurityRecommendation {
        if (!isProfileBased) {
            // Without an enrolled profile, real-time touch scoring has no reliable
            // baseline to compare against. Skip and let periodic analysis handle it.
            return SecurityRecommendation.CONTINUE
        }

        realTimeTouchSamples++
        if (touchData.pressure >= 0.95f) {
            saturatedPressureSamples++
        }
        latestTouchPressure = touchData.pressure
        latestSwipeVelocity = if (touchData.velocity > 0f) touchData.velocity else latestSwipeVelocity

        val isTrustRamp = realTimeTouchSamples <= TOUCH_TRUST_RAMP_SAMPLES ||
            System.currentTimeMillis() - sessionStartedAt < TOUCH_TRUST_RAMP_MS
        val isClearlyExtremeTouch = touchData.holdDuration > 1200L ||
            touchData.duration > 1800L ||
            (touchData.velocity > 0f && baselineSwipeVelocity > 0f &&
                touchData.velocity > (baselineSwipeVelocity * 3.2f).coerceAtLeast(2200f))

        if (isTrustRamp && !isClearlyExtremeTouch) {
            _currentRiskScore.value = (_currentRiskScore.value * 0.96f).coerceIn(0f, 1f)
            _currentRiskLevel.value = mapScoreToRiskLevel(_currentRiskScore.value)
            return SecurityRecommendation.CONTINUE
        }

        var weightedRisk = 0f
        var totalWeight = 0f

        fun addRisk(value: Float?, weight: Float) {
            if (value == null) return
            weightedRisk += value.coerceIn(0f, 1f) * weight
            totalWeight += weight
        }

        val touchArea = if (touchData.touchArea > 0f) touchData.touchArea else touchData.touchSize
        val isSwipe = touchData.touchType in listOf(
            TouchType.SWIPE_UP,
            TouchType.SWIPE_DOWN,
            TouchType.SWIPE_LEFT,
            TouchType.SWIPE_RIGHT,
            TouchType.SCROLL
        )

        val pressureRisk = normalizedDeviation(
            value = touchData.pressure,
            baseline = baselineTouchPressure,
            stdDev = baselineTouchPressureStdDev,
            relativeTolerance = 0.55f,
            minScale = 0.18f
        )
        val pressureWeight = if (saturatedPressureSamples >= 3) 0.35f else 0.85f
        addRisk(pressureRisk, pressureWeight)

        addRisk(
            normalizedDeviation(touchArea, baselineTouchArea, baselineTouchAreaStdDev, 0.50f, 0.18f),
            if (saturatedPressureSamples >= 3) 0.35f else 0.60f
        )
        addRisk(
            normalizedDeviation(touchData.duration.toFloat(), baselineTouchDuration, baselineTouchDurationStdDev, 0.35f, 40f),
            0.85f
        )
        addRisk(
            normalizedDeviation(touchData.holdDuration.toFloat(), baselineHoldDuration, baselineHoldDurationStdDev, 0.40f, 60f),
            0.90f
        )
        if (isSwipe) {
            addRisk(
                normalizedDeviation(touchData.velocity, baselineSwipeVelocity, baselineSwipeVelocityStdDev, 0.35f, 75f),
                0.90f
            )
            addRisk(
                normalizedDeviation(touchData.acceleration, baselineSwipeAcceleration, baselineSwipeAccelerationStdDev, 0.40f, 75f),
                0.70f
            )
        }

        val gestureRisk = when {
            touchData.touchType == TouchType.LONG_PRESS && baselineLongPressRatio < 0.15f -> 0.65f
            isSwipe && baselineSwipeRatio in 0f..0.15f -> 0.35f
            touchData.touchType == TouchType.TAP && baselineTapRatio > 0f && baselineTapRatio < 0.30f -> 0.25f
            else -> 0f
        }
        addRisk(gestureRisk, 0.55f)

        if (totalWeight == 0f) return SecurityRecommendation.CONTINUE

        val baseTouchRisk = weightedRisk / totalWeight
        val isHighPressure = saturatedPressureSamples < 3 && (pressureRisk ?: 0f) >= 0.70f
        val isLongHold = touchData.holdDuration >= (baselineHoldDuration + baselineHoldDurationStdDev * 3f).coerceAtLeast(400f)

        consecutiveAbnormalTouches = if (baseTouchRisk >= 0.38f) {
            (consecutiveAbnormalTouches + 1).coerceAtMost(8)
        } else {
            (consecutiveAbnormalTouches - 1).coerceAtLeast(0)
        }
        consecutiveHighPressureTouches = if (isHighPressure) {
            (consecutiveHighPressureTouches + 1).coerceAtMost(6)
        } else {
            (consecutiveHighPressureTouches - 1).coerceAtLeast(0)
        }
        consecutiveLongPressTouches = if (isLongHold || touchData.touchType == TouchType.LONG_PRESS) {
            (consecutiveLongPressTouches + 1).coerceAtMost(6)
        } else {
            (consecutiveLongPressTouches - 1).coerceAtLeast(0)
        }

        val persistenceBoost = (
            consecutiveAbnormalTouches * 0.04f +
                consecutiveHighPressureTouches * 0.05f +
                consecutiveLongPressTouches * 0.05f
            ).coerceAtMost(0.35f)
        val instantaneousRisk = (baseTouchRisk + persistenceBoost).coerceIn(0f, 1f)
        val smoothingFactor = when {
            instantaneousRisk >= 0.70f -> 0.45f
            instantaneousRisk >= 0.45f -> 0.30f
            else -> 0.12f
        }
        val previousScore = _currentRiskScore.value
        val smoothedRisk = if (instantaneousRisk < 0.15f) {
            (previousScore * 0.92f + instantaneousRisk * 0.08f).coerceIn(0f, 1f)
        } else {
            (previousScore * (1f - smoothingFactor) + instantaneousRisk * smoothingFactor).coerceIn(0f, 1f)
        }

        _currentRiskScore.value = smoothedRisk

        val riskLevel = mapScoreToRiskLevel(smoothedRisk)
        _currentRiskLevel.value = riskLevel

        return when (riskLevel) {
            RiskLevel.CRITICAL -> SecurityRecommendation.FORCE_LOGOUT
            RiskLevel.HIGH -> SecurityRecommendation.REQUEST_REAUTHENTICATION
            RiskLevel.MEDIUM -> SecurityRecommendation.SHOW_WARNING
            RiskLevel.LOW -> SecurityRecommendation.CONTINUE
        }
    }

    private fun mapScoreToRiskLevel(score: Float): RiskLevel = when {
        score >= CRITICAL_THRESHOLD -> RiskLevel.CRITICAL
        score >= HIGH_THRESHOLD -> RiskLevel.HIGH
        score >= MEDIUM_THRESHOLD -> RiskLevel.MEDIUM
        else -> RiskLevel.LOW
    }

    private fun normalizedDeviation(
        value: Float,
        baseline: Float,
        stdDev: Float,
        relativeTolerance: Float,
        minScale: Float
    ): Float? {
        if (value <= 0f || baseline <= 0f) return null
        val scale = maxOf(stdDev * 3f, abs(baseline) * relativeTolerance, minScale)
        return (abs(value - baseline) / scale).coerceIn(0f, 1f)
    }

    private fun List<Float>.meanOrZero(): Float =
        if (isEmpty()) 0f else sum() / size

    private fun List<Float>.stdOrZero(): Float {
        if (size < 2) return 0f
        val mean = meanOrZero()
        return sqrt(map { value -> (value - mean) * (value - mean) }.meanOrZero())
    }

    private fun TouchData.isSwipeLike(): Boolean =
        touchType == TouchType.SWIPE_UP ||
            touchType == TouchType.SWIPE_DOWN ||
            touchType == TouchType.SWIPE_LEFT ||
            touchType == TouchType.SWIPE_RIGHT ||
            touchType == TouchType.SCROLL
    
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
        val recentTouchesForProfile = behavioralRepository.getRecentTouches(sessionId, ANALYSIS_WINDOW)
        if (recentTouchesForProfile.size >= MIN_TOUCH_SAMPLES) {
            val pressures = recentTouchesForProfile.map { it.pressure }.filter { it > 0f }
            val areas = recentTouchesForProfile.map { if (it.touchArea > 0f) it.touchArea else it.touchSize }.filter { it > 0f }
            val durations = recentTouchesForProfile.map { it.duration.toFloat() }.filter { it > 0f }
            val holdDurations = recentTouchesForProfile.map { it.holdDuration.toFloat() }.filter { it > 0f }
            val swipes = recentTouchesForProfile.filter { it.isSwipeLike() }
            val velocities = swipes.map { it.velocity }.filter { it > 0f }
            val accelerations = swipes.map { it.acceleration }.filter { it > 0f }
            val total = recentTouchesForProfile.size.toFloat().coerceAtLeast(1f)
            val tapRatio = recentTouchesForProfile.count { it.touchType == TouchType.TAP }.toFloat() / total
            val swipeRatio = swipes.size.toFloat() / total
            val longPressRatio = recentTouchesForProfile.count { it.touchType == TouchType.LONG_PRESS }.toFloat() / total
            val interTouchIntervals = recentTouchesForProfile
                .sortedBy { it.timestamp }
                .zipWithNext()
                .map { (first, second) -> (second.timestamp - first.timestamp).toFloat() }
                .filter { it > 0f }

            latestTouchPressure = pressures.meanOrZero()
            latestSwipeVelocity = velocities.meanOrZero()

            val featureRisks = mutableListOf<Pair<String, Float>>()

            fun addFeatureRisk(name: String, risk: Float?) {
                if (risk != null) {
                    featureRisks.add(name to risk.coerceIn(0f, 1f))
                }
            }

            val pressureSaturated = pressures.count { it >= 0.95f } >= (pressures.size / 2).coerceAtLeast(2)
            if (!pressureSaturated) {
                addFeatureRisk("pressure", normalizedDeviation(pressures.meanOrZero(), baselineTouchPressure, baselineTouchPressureStdDev, 0.55f, 0.18f))
                addFeatureRisk("touch area", normalizedDeviation(areas.meanOrZero(), baselineTouchArea, baselineTouchAreaStdDev, 0.50f, 0.18f))
            }
            addFeatureRisk("duration", normalizedDeviation(durations.meanOrZero(), baselineTouchDuration, baselineTouchDurationStdDev, 0.35f, 40f))
            addFeatureRisk("hold duration", normalizedDeviation(holdDurations.meanOrZero(), baselineHoldDuration, baselineHoldDurationStdDev, 0.40f, 60f))
            addFeatureRisk("swipe velocity", normalizedDeviation(velocities.meanOrZero(), baselineSwipeVelocity, baselineSwipeVelocityStdDev, 0.35f, 75f))
            addFeatureRisk("swipe acceleration", normalizedDeviation(accelerations.meanOrZero(), baselineSwipeAcceleration, baselineSwipeAccelerationStdDev, 0.40f, 75f))

            if (baselineTapRatio > 0f) addFeatureRisk("tap ratio", abs(tapRatio - baselineTapRatio).coerceIn(0f, 1f))
            if (baselineSwipeRatio > 0f) addFeatureRisk("swipe ratio", abs(swipeRatio - baselineSwipeRatio).coerceIn(0f, 1f))
            if (baselineLongPressRatio > 0f || longPressRatio > 0.20f) {
                addFeatureRisk("long press ratio", abs(longPressRatio - baselineLongPressRatio).coerceIn(0f, 1f))
            }
            addFeatureRisk(
                "tap rhythm",
                normalizedDeviation(
                    interTouchIntervals.meanOrZero(),
                    baselineTouchDuration + 250f,
                    interTouchIntervals.stdOrZero().coerceAtLeast(120f),
                    0.50f,
                    120f
                )
            )

            val pressureRisk = featureRisks.firstOrNull { it.first == "pressure" }?.second ?: 0f
            val velocityRisk = featureRisks.firstOrNull { it.first == "swipe velocity" }?.second ?: 0f
            val longPressRisk = featureRisks.firstOrNull { it.first == "long press ratio" }?.second ?: 0f

            if (pressureRisk > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.TOUCH_PRESSURE_CHANGE,
                        severity = pressureRisk,
                        description = "Touch pressure deviation: ${(pressureRisk * 100).toInt()}%"
                    )
                )
            }

            if (velocityRisk > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.SWIPE_PATTERN_CHANGE,
                        severity = velocityRisk,
                        description = "Swipe velocity deviation: ${(velocityRisk * 100).toInt()}%"
                    )
                )
            }

            val topOtherRisk = featureRisks
                .filterNot { it.first == "pressure" || it.first == "swipe velocity" }
                .maxByOrNull { it.second }

            if (topOtherRisk != null && topOtherRisk.second > LOW_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.UNUSUAL_INTERACTION_PATTERN,
                        severity = topOtherRisk.second,
                        description = "Touch ${topOtherRisk.first} deviation: ${(topOtherRisk.second * 100).toInt()}%"
                    )
                )
            }

            if (longPressRisk > MEDIUM_THRESHOLD) {
                anomalies.add(
                    BehavioralAnomaly(
                        type = AnomalyType.UNUSUAL_INTERACTION_PATTERN,
                        severity = longPressRisk,
                        description = "Repeated long-press behavior detected"
                    )
                )
            }

            if (featureRisks.isEmpty()) return 0f

            val averageRisk = featureRisks.map { it.second }.average().toFloat()
            val maxRisk = featureRisks.maxOf { it.second }
            return ((averageRisk * 0.55f) + (maxRisk * 0.45f)).coerceIn(0f, 1f)
        }

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
                baselineDeviceState == DeviceState.HELD_IN_HAND && currentState == DeviceState.ON_TABLE -> 0.35f
                baselineDeviceState == DeviceState.HELD_IN_HAND && currentState == DeviceState.STATIONARY -> 0.30f
                baselineDeviceState == DeviceState.ON_TABLE && currentState == DeviceState.HELD_IN_HAND -> 0.25f
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
        val motionEvaluation = evaluateRealTimeMotion(
            motionData = motionData,
            previousMotionData = previousMotionData,
            baselinePitch = baselineDevicePitch,
            baselineRoll = baselineDeviceRoll,
            baselineGyroMagnitudeMean = baselineGyroMagnitudeMean,
            baselineGyroMagnitudeStdDev = baselineGyroMagnitudeStdDev
        )
        previousMotionData = motionData
        latestDevicePitch = motionData.pitch
        latestDeviceRoll = motionData.roll
        latestDeviceState = motionData.deviceStateEnum

        if (motionEvaluation.isCriticalSpike) {
            _currentRiskScore.value = motionEvaluation.riskScore.coerceAtLeast(CRITICAL_THRESHOLD)
            _currentRiskLevel.value = RiskLevel.CRITICAL
            return SecurityRecommendation.FORCE_LOGOUT
        }

        val instantaneousRisk = motionEvaluation.riskScore
        val smoothingFactor = if (motionEvaluation.isHighSpike) 0.45f else 0.1f
        val smoothedScore = (smoothingFactor * instantaneousRisk) +
            ((1f - smoothingFactor) * _currentRiskScore.value)
        
        _currentRiskScore.value = smoothedScore
        _currentRiskLevel.value = determineRiskLevel(smoothedScore)
        
        return if (_currentRiskLevel.value == RiskLevel.CRITICAL || _currentRiskLevel.value == RiskLevel.HIGH) {
            // Trigger immediate recommendation
             val anomalies = listOf(
                 BehavioralAnomaly(
                     type = AnomalyType.DEVICE_ORIENTATION_CHANGE,
                     severity = instantaneousRisk,
                     description = "Real-time motion spike detected"
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

    fun markUserVerified() {
        val verifiedScore = 0.10f
        _currentRiskScore.value = verifiedScore
        _currentRiskLevel.value = RiskLevel.LOW
        consecutiveAbnormalTouches = 0
        consecutiveHighPressureTouches = 0
        consecutiveLongPressTouches = 0
        publishDebugState(
            sessionState = "Identity verified",
            decision = SecurityRecommendation.CONTINUE.name,
            reason = "User completed PIN reauthentication. Risk counters were reset and monitoring continues.",
            riskScore = verifiedScore,
            riskLevel = RiskLevel.LOW,
            recommendation = SecurityRecommendation.CONTINUE,
            zScoreRisk = verifiedScore,
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
        isProfileBased = false
        baselineKeystrokeDwell = 0f
        baselineKeystrokeFlight = 0f
        baselineTouchPressure = 0.5f
        baselineTouchPressureStdDev = 0.1f
        baselineTouchArea = 0f
        baselineTouchAreaStdDev = 0.1f
        baselineTouchDuration = 150f
        baselineTouchDurationStdDev = 60f
        baselineHoldDuration = 0f
        baselineHoldDurationStdDev = 80f
        baselineSwipeVelocity = 500f
        baselineSwipeVelocityStdDev = 150f
        baselineSwipeAcceleration = 0f
        baselineSwipeAccelerationStdDev = 150f
        baselineTapRatio = 0f
        baselineSwipeRatio = 0f
        baselineLongPressRatio = 0f
        baselineDevicePitch = 45f
        baselineDeviceRoll = 0f
        baselineGyroMagnitudeMean = 0.15f
        baselineGyroMagnitudeStdDev = 0.08f
        baselineDeviceState = DeviceState.UNKNOWN
        consecutiveAbnormalTouches = 0
        consecutiveHighPressureTouches = 0
        consecutiveLongPressTouches = 0
        sessionStartedAt = 0L
        realTimeTouchSamples = 0
        saturatedPressureSamples = 0
        latestKeystrokeDwell = 0f
        latestKeystrokeFlight = 0f
        latestTouchPressure = 0f
        latestSwipeVelocity = 0f
        latestDevicePitch = 0f
        latestDeviceRoll = 0f
        latestDeviceState = null
        previousMotionData = null
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
