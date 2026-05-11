package com.securebank.app.domain

import com.securebank.app.data.model.BehavioralProfile
import com.securebank.app.data.model.MotionData
import com.securebank.app.data.model.PinKeystrokeEvent
import com.securebank.app.data.model.TouchData
import com.securebank.app.data.model.TouchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MLFeatureExtractorTest {

    @Test
    fun profileBaselineMarksExtractorReady() {
        val extractor = MLFeatureExtractor()

        assertFalse(extractor.hasEnrollmentBaseline())

        extractor.setEnrollmentBaseline(profile())

        assertTrue(extractor.hasEnrollmentBaseline())
    }

    @Test
    fun profileBaselineProducesOrderedDeviationFeatures() {
        val extractor = MLFeatureExtractor()
        extractor.setEnrollmentBaseline(profile())

        val features = extractor.computeDeviationFeatures(
            pinKeystrokes = pinEvents(),
            touches = touches(),
            motionData = motion(),
            featureOrder = listOf(
                "dev_pin_dwell_mean_abs",
                "dev_touch_pressure_mean_abs",
                "dev_touch_area_mean_abs",
                "dev_touch_hold_mean_abs",
                "raw_touch_long_press_ratio"
            )
        )

        assertNotNull(features)
        assertEquals(5, features!!.size)
        assertTrue(features[0] > 0f)
        assertTrue(features[1] > 0f)
        assertTrue(features[2] > 0f)
        assertTrue(features[3] > 0f)
        assertEquals(1f / 3f, features[4], 0.001f)
    }

    private fun profile() = BehavioralProfile(
        username = "tester",
        pressureMean = 0.45f,
        pressureStd = 0.05f,
        touchAreaMean = 1.1f,
        touchAreaStd = 0.1f,
        durationMean = 120f,
        durationStd = 25f,
        holdDurationMean = 40f,
        holdDurationStd = 20f,
        velocityMean = 500f,
        velocityStd = 80f,
        accelerationMean = 250f,
        accelerationStd = 60f,
        tapRatio = 0.7f,
        swipeRatio = 0.3f,
        longPressRatio = 0f,
        pinDwellMean = 90f,
        pinDwellStd = 12f,
        pinFlightMean = 110f,
        pinFlightStd = 20f,
        pitchMean = 42f,
        rollMean = 3f,
        gyroMagnitudeMean = 0.15f,
        gyroMagnitudeStd = 0.04f,
        accelMagnitudeMean = 9.8f,
        accelMagnitudeStd = 0.2f,
        sampleCount = 18
    )

    private fun pinEvents(): List<PinKeystrokeEvent> =
        (0 until 6).map { index ->
            PinKeystrokeEvent(
                sessionId = "session",
                timestamp = index * 200L,
                digit = index,
                keyDownTime = index * 200L,
                keyUpTime = index * 200L + 115L,
                dwellTime = 115L,
                flightTime = if (index == 0) 0L else 140L,
                touchX = 10f + index,
                touchY = 20f + index,
                touchSize = 1.4f,
                pinAttemptNumber = 1
            )
        }

    private fun touches() = listOf(
        touch(TouchType.TAP, pressure = 0.60f, area = 1.4f, duration = 150L, hold = 30L),
        touch(TouchType.LONG_PRESS, pressure = 0.62f, area = 1.5f, duration = 420L, hold = 380L),
        touch(TouchType.SWIPE_RIGHT, pressure = 0.58f, area = 1.3f, duration = 210L, hold = 20L, velocity = 720f)
    )

    private fun touch(
        type: TouchType,
        pressure: Float,
        area: Float,
        duration: Long,
        hold: Long,
        velocity: Float = 0f
    ) = TouchData(
        sessionId = "session",
        touchType = type,
        startX = 0f,
        startY = 0f,
        endX = 100f,
        endY = 0f,
        pressure = pressure,
        touchSize = area,
        duration = duration,
        velocity = velocity,
        acceleration = if (velocity > 0f) 300f else 0f,
        holdDuration = hold,
        touchArea = area
    )

    private fun motion(): List<MotionData> =
        (0 until 60).map {
            MotionData.create(
                sessionId = "session",
                accelX = 0.1f,
                accelY = 0.2f,
                accelZ = 9.7f,
                gyroX = 0.01f,
                gyroY = 0.02f,
                gyroZ = 0.03f,
                pitch = 45f,
                roll = 4f,
                azimuth = 0f
            )
        }
}
