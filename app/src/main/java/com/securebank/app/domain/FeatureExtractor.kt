package com.securebank.app.domain

import com.securebank.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ============================================
 * FEATURE EXTRACTOR - 52 BEHAVIORAL FEATURES
 * ============================================
 * Extracts numerical feature vectors from raw behavioral data
 * for ML model input. Features are computed per time window
 * and can be used for both on-device inference and offline training.
 *
 * Feature Groups:
 * - Keystroke features (12): timing rhythm
 * - Touch features (18): interaction patterns
 * - Motion features (22): device handling
 *
 * Total: 52 features per time window
 */
@Singleton
class FeatureExtractor @Inject constructor() {

    companion object {
        const val TOTAL_FEATURES = 52
        const val KEYSTROKE_FEATURES = 12
        const val TOUCH_FEATURES = 18
        const val MOTION_FEATURES = 22
    }

    /**
     * Extracts a complete feature vector from all available data.
     * Returns a FloatArray of size TOTAL_FEATURES.
     */
    fun extractFeatures(
        keystrokes: List<KeystrokeData>,
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>
    ): FloatArray {
        val features = FloatArray(TOTAL_FEATURES)
        var idx = 0

        // Keystroke features (0-11)
        val keystrokeFeatures = extractKeystrokeFeatures(keystrokes, pinKeystrokes)
        keystrokeFeatures.copyInto(features, idx)
        idx += KEYSTROKE_FEATURES

        // Touch features (12-29)
        val touchFeatures = extractTouchFeatures(touches)
        touchFeatures.copyInto(features, idx)
        idx += TOUCH_FEATURES

        // Motion features (30-51)
        val motionFeatures = extractMotionFeatures(motionData)
        motionFeatures.copyInto(features, idx)

        return features
    }

    /**
     * Extracts only keystroke features.
     * Returns FloatArray of size 12.
     *
     * [0]  mean_dwell_time       - Average dwell time (ms)
     * [1]  std_dwell_time        - Std deviation of dwell time
     * [2]  mean_flight_time      - Average flight time (ms)
     * [3]  std_flight_time       - Std deviation of flight time
     * [4]  max_dwell_time        - Maximum dwell time
     * [5]  min_dwell_time        - Minimum dwell time
     * [6]  dwell_time_range      - Max - Min dwell time
     * [7]  typing_speed          - Characters per second
     * [8]  digraph_01_latency    - Mean latency for top digraph pair 1
     * [9]  digraph_02_latency    - Mean latency for top digraph pair 2
     * [10] dwell_coefficient_var - CV of dwell time (how consistent)
     * [11] flight_coefficient_var - CV of flight time
     */
    fun extractKeystrokeFeatures(
        keystrokes: List<KeystrokeData>,
        pinKeystrokes: List<PinKeystrokeEvent>
    ): FloatArray {
        val features = FloatArray(KEYSTROKE_FEATURES)

        // Combine dwell times: prefer PIN (real) over software keyboard (estimated)
        val dwellTimes = if (pinKeystrokes.isNotEmpty()) {
            pinKeystrokes.map { it.dwellTime.toFloat() }
        } else {
            keystrokes.map { it.dwellTime.toFloat() }
        }

        val flightTimes = if (pinKeystrokes.isNotEmpty()) {
            pinKeystrokes.filter { it.flightTime > 0 }.map { it.flightTime.toFloat() }
        } else {
            keystrokes.filter { it.flightTime > 0 }.map { it.flightTime.toFloat() }
        }

        if (dwellTimes.isNotEmpty()) {
            features[0] = dwellTimes.average().toFloat()          // mean dwell
            features[1] = stdDev(dwellTimes)                      // std dwell
            features[4] = dwellTimes.max()                        // max dwell
            features[5] = dwellTimes.min()                        // min dwell
            features[6] = features[4] - features[5]               // dwell range
            features[10] = if (features[0] > 0) features[1] / features[0] else 0f  // CV dwell
        }

        if (flightTimes.isNotEmpty()) {
            features[2] = flightTimes.average().toFloat()         // mean flight
            features[3] = stdDev(flightTimes)                     // std flight
            features[11] = if (features[2] > 0) features[3] / features[2] else 0f  // CV flight
        }

        // Typing speed (chars per second)
        if (pinKeystrokes.size >= 2) {
            val firstTime = pinKeystrokes.first().keyDownTime
            val lastTime = pinKeystrokes.last().keyUpTime
            val durationSec = (lastTime - firstTime) / 1000f
            features[7] = if (durationSec > 0) pinKeystrokes.size / durationSec else 0f
        } else if (keystrokes.size >= 2) {
            val firstTime = keystrokes.first().timestamp
            val lastTime = keystrokes.last().timestamp
            val durationSec = (lastTime - firstTime) / 1000f
            features[7] = if (durationSec > 0) keystrokes.size / durationSec else 0f
        }

        // Digraph latencies (consecutive key pair timings)
        if (pinKeystrokes.size >= 3) {
            val digraphs = pinKeystrokes.zipWithNext().map { (a, b) ->
                b.keyDownTime - a.keyUpTime
            }
            if (digraphs.size >= 2) {
                features[8] = digraphs.take(digraphs.size / 2).average().toFloat()
                features[9] = digraphs.drop(digraphs.size / 2).average().toFloat()
            }
        }

        return features
    }

    /**
     * Extracts touch interaction features.
     * Returns FloatArray of size 18.
     *
     * [0]  mean_touch_area       - Average touch contact area
     * [1]  std_touch_area        - Touch area consistency
     * [2]  mean_touch_duration   - Average touch duration (ms)
     * [3]  std_touch_duration    - Touch duration consistency
     * [4]  max_touch_duration    - Longest touch
     * [5]  mean_swipe_velocity   - Average swipe speed
     * [6]  std_swipe_velocity    - Swipe speed consistency
     * [7]  mean_swipe_accel      - Average swipe acceleration
     * [8]  tap_ratio             - Proportion of taps vs total touches
     * [9]  swipe_ratio           - Proportion of swipes vs total
     * [10] mean_inter_touch_interval - Time between touches
     * [11] std_inter_touch_interval
     * [12] touch_x_entropy       - Spatial entropy of X coordinates
     * [13] touch_y_entropy       - Spatial entropy of Y coordinates
     * [14] mean_pressure         - Average pressure (may be binary on modern devices)
     * [15] swipe_angle_mean      - Average swipe direction angle
     * [16] swipe_angle_std       - Swipe direction consistency
     * [17] long_press_ratio      - Proportion of long presses
     */
    fun extractTouchFeatures(touches: List<TouchData>): FloatArray {
        val features = FloatArray(TOUCH_FEATURES)
        if (touches.isEmpty()) return features

        // Touch area features — prefer touchArea (normalized) over touchSize (raw sensor)
        val areas = touches.map { if (it.touchArea > 0f) it.touchArea else it.touchSize }
        features[0] = areas.average().toFloat()
        features[1] = stdDev(areas)

        // Duration features
        val durations = touches.map { it.duration.toFloat() }
        features[2] = durations.average().toFloat()
        features[3] = stdDev(durations)
        features[4] = durations.max()

        // Swipe features
        val swipes = touches.filter {
            it.touchType in listOf(
                TouchType.SWIPE_UP, TouchType.SWIPE_DOWN,
                TouchType.SWIPE_LEFT, TouchType.SWIPE_RIGHT
            )
        }
        if (swipes.isNotEmpty()) {
            val velocities = swipes.map { it.velocity }
            features[5] = velocities.average().toFloat()
            features[6] = stdDev(velocities)

            val accels = swipes.map { it.acceleration }
            features[7] = accels.average().toFloat()

            // Swipe angles
            val angles = swipes.map { swipe ->
                atan2(
                    (swipe.endY - swipe.startY).toDouble(),
                    (swipe.endX - swipe.startX).toDouble()
                ).toFloat()
            }
            features[15] = angles.average().toFloat()
            features[16] = stdDev(angles)
        }

        // Touch type ratios
        val total = touches.size.toFloat()
        val taps = touches.count { it.touchType == TouchType.TAP }
        val swipeCount = swipes.size
        val longPresses = touches.count { it.touchType == TouchType.LONG_PRESS }

        features[8] = taps / total              // tap ratio
        features[9] = swipeCount / total         // swipe ratio
        features[17] = longPresses / total       // long press ratio (count)

        // Enhance long press ratio with holdDuration data when available
        val holdDurations = touches.map { it.holdDuration.toFloat() }.filter { it > 0f }
        if (holdDurations.isNotEmpty()) {
            // Override with proportion of total hold time to total duration
            val totalHold = holdDurations.sum()
            val totalDuration = durations.sum()
            if (totalDuration > 0f) {
                features[17] = totalHold / totalDuration
            }
        }

        // Inter-touch intervals
        if (touches.size >= 2) {
            val intervals = touches.zipWithNext().map {
                (it.second.timestamp - it.first.timestamp).toFloat()
            }
            features[10] = intervals.average().toFloat()
            features[11] = stdDev(intervals)
        }

        // Spatial entropy (how spread out are touches)
        features[12] = calculateEntropy(touches.map { it.startX })
        features[13] = calculateEntropy(touches.map { it.startY })

        // Pressure
        features[14] = touches.map { it.pressure }.average().toFloat()

        return features
    }

    /**
     * Extracts motion/sensor features.
     * Returns FloatArray of size 22.
     *
     * [0-2]   accel_x/y/z_mean    - Average acceleration per axis
     * [3-5]   accel_x/y/z_std     - Acceleration variability per axis
     * [6-8]   gyro_x/y/z_mean     - Average rotation rate per axis
     * [9-11]  gyro_x/y/z_std      - Rotation variability per axis
     * [12]    pitch_mean           - Average device pitch
     * [13]    pitch_std            - Pitch variability
     * [14]    roll_mean            - Average device roll
     * [15]    roll_std             - Roll variability
     * [16]    accel_magnitude_mean - Average total acceleration
     * [17]    accel_magnitude_std  - Acceleration magnitude variability
     * [18]    gyro_magnitude_mean  - Average total rotation
     * [19]    gyro_magnitude_std
     * [20]    dominant_device_state - Most common device state (encoded)
     * [21]    device_state_changes - Number of state transitions
     */
    fun extractMotionFeatures(motionData: List<MotionData>): FloatArray {
        val features = FloatArray(MOTION_FEATURES)
        if (motionData.isEmpty()) return features

        // Accelerometer per axis
        val accelX = motionData.map { it.accelX }
        val accelY = motionData.map { it.accelY }
        val accelZ = motionData.map { it.accelZ }

        features[0] = accelX.average().toFloat()
        features[1] = accelY.average().toFloat()
        features[2] = accelZ.average().toFloat()
        features[3] = stdDev(accelX)
        features[4] = stdDev(accelY)
        features[5] = stdDev(accelZ)

        // Gyroscope per axis
        val gyroX = motionData.map { it.gyroX }
        val gyroY = motionData.map { it.gyroY }
        val gyroZ = motionData.map { it.gyroZ }

        features[6] = gyroX.average().toFloat()
        features[7] = gyroY.average().toFloat()
        features[8] = gyroZ.average().toFloat()
        features[9] = stdDev(gyroX)
        features[10] = stdDev(gyroY)
        features[11] = stdDev(gyroZ)

        // Orientation
        val pitchValues = motionData.map { it.pitch }
        val rollValues = motionData.map { it.roll }

        features[12] = pitchValues.average().toFloat()
        features[13] = stdDev(pitchValues)
        features[14] = rollValues.average().toFloat()
        features[15] = stdDev(rollValues)

        // Acceleration magnitude
        val accelMagnitudes = motionData.map { m ->
            sqrt(m.accelX * m.accelX + m.accelY * m.accelY + m.accelZ * m.accelZ)
        }
        features[16] = accelMagnitudes.average().toFloat()
        features[17] = stdDev(accelMagnitudes)

        // Gyro magnitude
        val gyroMagnitudes = motionData.map { m ->
            sqrt(m.gyroX * m.gyroX + m.gyroY * m.gyroY + m.gyroZ * m.gyroZ)
        }
        features[18] = gyroMagnitudes.average().toFloat()
        features[19] = stdDev(gyroMagnitudes)

        // Device state encoding (most common state)
        val states = motionData.map { it.deviceStateEnum }
        val mostCommon = states.groupBy { it }.maxByOrNull { it.value.size }?.key
        features[20] = (mostCommon?.ordinal ?: DeviceState.UNKNOWN.ordinal).toFloat()

        // State transitions
        var stateChanges = 0
        for (i in 1 until states.size) {
            if (states[i] != states[i - 1]) stateChanges++
        }
        features[21] = stateChanges.toFloat()

        return features
    }

    /**
     * Generates feature names for CSV header row.
     * Useful for data export and model interpretation.
     */
    fun getFeatureNames(): List<String> = listOf(
        // Keystroke (0-11)
        "ks_mean_dwell", "ks_std_dwell", "ks_mean_flight", "ks_std_flight",
        "ks_max_dwell", "ks_min_dwell", "ks_dwell_range", "ks_typing_speed",
        "ks_digraph_1", "ks_digraph_2", "ks_cv_dwell", "ks_cv_flight",
        // Touch (12-29)
        "tc_mean_area", "tc_std_area", "tc_mean_duration", "tc_std_duration",
        "tc_max_duration", "tc_mean_swipe_vel", "tc_std_swipe_vel", "tc_mean_swipe_accel",
        "tc_tap_ratio", "tc_swipe_ratio", "tc_mean_iti", "tc_std_iti",
        "tc_x_entropy", "tc_y_entropy", "tc_mean_pressure",
        "tc_swipe_angle_mean", "tc_swipe_angle_std", "tc_longpress_ratio",
        // Motion (30-51)
        "mo_accel_x_mean", "mo_accel_y_mean", "mo_accel_z_mean",
        "mo_accel_x_std", "mo_accel_y_std", "mo_accel_z_std",
        "mo_gyro_x_mean", "mo_gyro_y_mean", "mo_gyro_z_mean",
        "mo_gyro_x_std", "mo_gyro_y_std", "mo_gyro_z_std",
        "mo_pitch_mean", "mo_pitch_std", "mo_roll_mean", "mo_roll_std",
        "mo_accel_mag_mean", "mo_accel_mag_std",
        "mo_gyro_mag_mean", "mo_gyro_mag_std",
        "mo_dominant_state", "mo_state_changes"
    )

    // ========================
    // STATISTICAL HELPERS
    // ========================

    private fun stdDev(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).let { d -> d * d } }.average().toFloat()
        return sqrt(variance)
    }

    private fun calculateEntropy(values: List<Float>, bins: Int = 10): Float {
        if (values.isEmpty()) return 0f

        val min = values.min()
        val max = values.max()
        val range = max - min

        if (range == 0f) return 0f

        // Bin the values
        val binCounts = IntArray(bins)
        values.forEach { v ->
            val binIndex = ((v - min) / range * (bins - 1)).toInt().coerceIn(0, bins - 1)
            binCounts[binIndex]++
        }

        // Calculate Shannon entropy
        val total = values.size.toFloat()
        var entropy = 0f
        binCounts.forEach { count ->
            if (count > 0) {
                val p = count / total
                entropy -= p * log2(p)
            }
        }

        return entropy
    }
}
