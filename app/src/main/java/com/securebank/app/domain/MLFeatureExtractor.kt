package com.securebank.app.domain

import com.securebank.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * ============================================
 * ML FEATURE EXTRACTOR
 * ============================================
 * Computes enrollment-relative deviation features that match
 * the Python ml_model.py pipeline exactly.
 *
 * Flow:
 *   1. Extract raw features from enrollment session (baseline)
 *   2. Extract raw features from current session
 *   3. Compute deviation features (absolute + relative + raw)
 *   4. Feed into MLModelInference for genuine/impostor prediction
 *
 * Feature count: 124 (matches trained model)
 */
@Singleton
class MLFeatureExtractor @Inject constructor() {

    /**
     * Raw features extracted from a single session.
     * Maps feature name -> value (mimics Python dict).
     */
    data class RawFeatures(val features: MutableMap<String, Float> = mutableMapOf())

    // Stored enrollment baseline
    private var enrollmentFeatures: RawFeatures? = null

    /**
     * Set the enrollment baseline from collected session data.
     * Call this once after enrollment is complete.
     */
    fun setEnrollmentBaseline(
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>
    ) {
        enrollmentFeatures = extractRawFeatures(pinKeystrokes, touches, motionData)
    }

    /**
     * Restore an enrollment baseline from the persisted signup profile.
     * This lets ML become available after app restart without retaining raw
     * enrollment samples long-term.
     */
    fun setEnrollmentBaseline(profile: BehavioralProfile) {
        enrollmentFeatures = RawFeatures(
            mutableMapOf(
                "pin_dwell_mean" to profile.pinDwellMean,
                "pin_dwell_std" to profile.pinDwellStd,
                "pin_dwell_median" to profile.pinDwellMean,
                "pin_dwell_q25" to (profile.pinDwellMean - profile.pinDwellStd * 0.67f).coerceAtLeast(0f),
                "pin_dwell_q75" to (profile.pinDwellMean + profile.pinDwellStd * 0.67f),
                "pin_flight_mean" to profile.pinFlightMean,
                "pin_flight_std" to profile.pinFlightStd,
                "pin_flight_median" to profile.pinFlightMean,
                "pin_flight_iqr" to (profile.pinFlightStd * 1.35f),
                "pin_touch_x_mean" to 0f,
                "pin_touch_x_std" to 0f,
                "pin_touch_y_mean" to 0f,
                "pin_touch_y_std" to 0f,
                "pin_touch_size_mean" to profile.touchAreaMean,
                "pin_touch_size_std" to profile.touchAreaStd,
                "pin_dwell_skew" to 0f,
                "pin_dwell_kurtosis" to 0f,
                "pin_dwell_iqr" to (profile.pinDwellStd * 1.35f),
                "touch_pressure_mean" to profile.pressureMean,
                "touch_pressure_std" to profile.pressureStd,
                "touch_area_mean" to profile.touchAreaMean,
                "touch_area_std" to profile.touchAreaStd,
                "touch_hold_mean" to profile.holdDurationMean,
                "touch_hold_std" to profile.holdDurationStd,
                "touch_duration_mean" to profile.durationMean,
                "touch_duration_std" to profile.durationStd,
                "touch_velocity_mean" to profile.velocityMean,
                "touch_velocity_std" to profile.velocityStd,
                "touch_velocity_max" to (profile.velocityMean + profile.velocityStd * 2f),
                "touch_accel_mean" to profile.accelerationMean,
                "touch_accel_std" to profile.accelerationStd,
                "touch_tap_ratio" to profile.tapRatio,
                "touch_swipe_ratio" to profile.swipeRatio,
                "touch_long_press_ratio" to profile.longPressRatio,
                "touch_count" to profile.sampleCount.toFloat(),
                "motion_accel_mag_mean" to profile.accelMagnitudeMean,
                "motion_accel_mag_std" to profile.accelMagnitudeStd,
                "motion_gyro_mag_mean" to profile.gyroMagnitudeMean,
                "motion_gyro_mag_std" to profile.gyroMagnitudeStd,
                "motion_accel_x_mean" to 0f,
                "motion_accel_y_mean" to 0f,
                "motion_accel_z_mean" to profile.accelMagnitudeMean
            )
        )

        for (pos in 0 until 6) {
            enrollmentFeatures?.features?.put("pin_rhythm_d$pos", profile.pinDwellMean)
        }
    }

    /**
     * Compute the full 124-dimension deviation feature vector
     * for a current session vs the enrollment baseline.
     *
     * @param featureOrder The model's feature_names list that defines output ordering.
     *                     Obtain from MLModelInference.getFeatureNames().
     * @return FloatArray of deviation features in model's expected order,
     *         or null if no enrollment baseline.
     */
    fun computeDeviationFeatures(
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>,
        featureOrder: List<String>? = null
    ): FloatArray? {
        val enrollment = enrollmentFeatures ?: return null
        val session = extractRawFeatures(pinKeystrokes, touches, motionData)
        val deviationMap = computeDeviationMap(enrollment, session)

        if (featureOrder != null) {
            // Return features in exactly the order the model expects
            return FloatArray(featureOrder.size) { i ->
                deviationMap[featureOrder[i]] ?: 0f
            }
        }

        // Fallback: return in map iteration order (not recommended)
        return deviationMap.values.toFloatArray()
    }

    /**
     * Extract raw features from session data.
     * Mirrors Python extract_raw_features() exactly.
     */
    private fun extractRawFeatures(
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>
    ): RawFeatures {
        val f = RawFeatures()

        // ── PIN Keystroke Features ──
        if (pinKeystrokes.size >= 6) {
            val dwells = pinKeystrokes.map { it.dwellTime.toFloat() }
            val flights = pinKeystrokes.filter { it.flightTime > 0 }.map { it.flightTime.toFloat() }
            val touchXs = pinKeystrokes.map { it.touchX }
            val touchYs = pinKeystrokes.map { it.touchY }
            val touchSizes = pinKeystrokes.map { it.touchSize }

            f.put("pin_dwell_mean", dwells.mean())
            f.put("pin_dwell_std", dwells.std())
            f.put("pin_dwell_median", dwells.median())
            f.put("pin_dwell_q25", dwells.percentile(25f))
            f.put("pin_dwell_q75", dwells.percentile(75f))
            f.put("pin_flight_mean", if (flights.isNotEmpty()) flights.mean() else 0f)
            f.put("pin_flight_std", if (flights.isNotEmpty()) flights.std() else 0f)
            f.put("pin_flight_median", if (flights.isNotEmpty()) flights.median() else 0f)
            f.put("pin_touch_x_mean", touchXs.mean())
            f.put("pin_touch_x_std", touchXs.std())
            f.put("pin_touch_y_mean", touchYs.mean())
            f.put("pin_touch_y_std", touchYs.std())
            f.put("pin_touch_size_mean", touchSizes.mean())
            f.put("pin_touch_size_std", touchSizes.std())
            f.put("pin_dwell_skew", dwells.skewness())
            f.put("pin_dwell_kurtosis", dwells.kurtosis())
            f.put("pin_dwell_iqr", dwells.percentile(75f) - dwells.percentile(25f))
            f.put("pin_flight_iqr",
                if (flights.size > 3) flights.percentile(75f) - flights.percentile(25f) else 0f)

            // Per-digit position rhythm (positions 0-5 in the PIN)
            for (pos in 0 until 6) {
                val positionDwells = pinKeystrokes.filterIndexed { i, _ -> i % 6 == pos }
                    .map { it.dwellTime.toFloat() }
                f.put("pin_rhythm_d$pos", if (positionDwells.isNotEmpty()) positionDwells.mean() else 0f)
            }
        }

        // ── Touch Features ──
        if (touches.size >= 3) {
            val durations = touches.map { it.duration.toFloat() }
            val velocities = touches.map { it.velocity }
            val accels = touches.map { it.acceleration }
            val pressures = touches.map { it.pressure }
            val areas = touches.map { if (it.touchArea > 0f) it.touchArea else it.touchSize }
            val holds = touches.map { it.holdDuration.toFloat() }
            val taps = touches.count { it.touchType == TouchType.TAP }
            val swipes = touches.count {
                it.touchType == TouchType.SWIPE_UP ||
                    it.touchType == TouchType.SWIPE_DOWN ||
                    it.touchType == TouchType.SWIPE_LEFT ||
                    it.touchType == TouchType.SWIPE_RIGHT ||
                    it.touchType == TouchType.SCROLL
            }
            val longPresses = touches.count { it.touchType == TouchType.LONG_PRESS }

            f.put("touch_pressure_mean", pressures.mean())
            f.put("touch_pressure_std", pressures.std())
            f.put("touch_area_mean", areas.mean())
            f.put("touch_area_std", areas.std())
            f.put("touch_hold_mean", holds.mean())
            f.put("touch_hold_std", holds.std())
            f.put("touch_duration_mean", durations.mean())
            f.put("touch_duration_std", durations.std())
            f.put("touch_velocity_mean", velocities.mean())
            f.put("touch_velocity_std", velocities.std())
            f.put("touch_velocity_max", velocities.max())
            f.put("touch_accel_mean", accels.mean())
            f.put("touch_accel_std", accels.std())
            f.put("touch_tap_ratio", taps.toFloat() / touches.size)
            f.put("touch_swipe_ratio", swipes.toFloat() / touches.size)
            f.put("touch_long_press_ratio", longPresses.toFloat() / touches.size)
            f.put("touch_count", touches.size.toFloat())
        }

        // ── Motion Features ──
        if (motionData.size >= 50) {
            val sample = if (motionData.size > 500) motionData.take(500) else motionData

            val axVals = sample.map { it.accelX }
            val ayVals = sample.map { it.accelY }
            val azVals = sample.map { it.accelZ }
            val gxVals = sample.map { it.gyroX }
            val gyVals = sample.map { it.gyroY }
            val gzVals = sample.map { it.gyroZ }

            val accelMag = sample.map { sqrt(it.accelX.p2() + it.accelY.p2() + it.accelZ.p2()) }
            val gyroMag = sample.map { sqrt(it.gyroX.p2() + it.gyroY.p2() + it.gyroZ.p2()) }

            f.put("motion_accel_mag_mean", accelMag.mean())
            f.put("motion_accel_mag_std", accelMag.std())
            f.put("motion_gyro_mag_mean", gyroMag.mean())
            f.put("motion_gyro_mag_std", gyroMag.std())
            f.put("motion_accel_x_mean", axVals.mean())
            f.put("motion_accel_y_mean", ayVals.mean())
            f.put("motion_accel_z_mean", azVals.mean())
        }

        return f
    }

    /**
     * Compute deviation features between enrollment and session as a named map.
     * Mirrors Python compute_deviation_features() exactly.
     *
     * For each common feature key:
     *   dev_{key}_abs = |session - enrollment|
     *   dev_{key}_rel = |session - enrollment| / |enrollment|
     *   raw_{key} = session value
     * Plus 4 aggregate features.
     */
    private fun computeDeviationMap(enrollment: RawFeatures, session: RawFeatures): Map<String, Float> {
        val deviation = mutableMapOf<String, Float>()

        val commonKeys = enrollment.features.keys.intersect(session.features.keys)

        for (key in commonKeys) {
            val ev = enrollment.features[key] ?: 0f
            val sv = session.features[key] ?: 0f

            deviation["dev_${key}_abs"] = abs(sv - ev)
            deviation["dev_${key}_rel"] = if (abs(ev) > 1e-6f) abs(sv - ev) / abs(ev) else abs(sv)
            deviation["raw_$key"] = sv
        }

        // Aggregate deviation features
        val absDevs = deviation.filter { "_abs" in it.key }.values.toList()
        val relDevs = deviation.filter { "_rel" in it.key }.values.toList()

        deviation["overall_abs_deviation"] = if (absDevs.isNotEmpty()) absDevs.mean() else 0f
        deviation["overall_rel_deviation"] = if (relDevs.isNotEmpty()) relDevs.mean() else 0f
        deviation["max_abs_deviation"] = if (absDevs.isNotEmpty()) absDevs.max() else 0f
        deviation["max_rel_deviation"] = if (relDevs.isNotEmpty()) relDevs.max() else 0f

        return deviation
    }

    /**
     * Compute deviation features and return as a named map.
     * This is preferred over the array version as it preserves feature ordering.
     */
    fun computeDeviationFeaturesMap(
        pinKeystrokes: List<PinKeystrokeEvent>,
        touches: List<TouchData>,
        motionData: List<MotionData>
    ): Map<String, Float>? {
        val enrollment = enrollmentFeatures ?: return null
        val session = extractRawFeatures(pinKeystrokes, touches, motionData)
        return computeDeviationMap(enrollment, session)
    }

    fun hasEnrollmentBaseline(): Boolean = enrollmentFeatures != null

    fun clearBaseline() {
        enrollmentFeatures = null
    }

    // ── Extension helpers ──

    private fun RawFeatures.put(key: String, value: Float) {
        features[key] = value
    }

    private fun List<Float>.mean(): Float =
        if (isEmpty()) 0f else (sum() / size)

    private fun List<Float>.std(): Float {
        if (size < 2) return 0f
        val m = mean()
        return sqrt(map { (it - m).let { d -> d * d } }.mean())
    }

    private fun List<Float>.median(): Float {
        if (isEmpty()) return 0f
        val sorted = sorted()
        return if (size % 2 == 0) (sorted[size / 2 - 1] + sorted[size / 2]) / 2f
        else sorted[size / 2]
    }

    private fun List<Float>.percentile(p: Float): Float {
        if (isEmpty()) return 0f
        val sorted = sorted()
        val idx = (p / 100f * (size - 1)).let { it.toInt().coerceIn(0, size - 1) }
        return sorted[idx]
    }

    private fun List<Float>.skewness(): Float {
        if (size < 3) return 0f
        val m = mean()
        val s = std()
        if (s < 1e-8f) return 0f
        return map { ((it - m) / s).let { d -> d * d * d } }.mean()
    }

    private fun List<Float>.kurtosis(): Float {
        if (size < 4) return 0f
        val m = mean()
        val s = std()
        if (s < 1e-8f) return 0f
        return map { ((it - m) / s).let { d -> d * d * d * d } }.mean() - 3f
    }

    private fun Float.p2(): Float = this * this
}
