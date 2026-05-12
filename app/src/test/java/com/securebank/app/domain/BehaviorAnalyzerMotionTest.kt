package com.securebank.app.domain

import com.securebank.app.data.model.MotionData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BehaviorAnalyzerMotionTest {

    @Test
    fun suddenGyroSpikeIsCritical() {
        val result = BehaviorAnalyzer.evaluateRealTimeMotion(
            motionData = motion(gyroX = 4.2f, pitch = 43f, roll = 4f),
            previousMotionData = motion(pitch = 42f, roll = 3f),
            baselinePitch = 42f,
            baselineRoll = 3f,
            baselineGyroMagnitudeMean = 0.15f,
            baselineGyroMagnitudeStdDev = 0.04f
        )

        assertTrue(result.isCriticalSpike)
        assertTrue(result.riskScore >= BehaviorAnalyzer.CRITICAL_THRESHOLD)
    }

    @Test
    fun suddenOrientationDeltaIsCritical() {
        val result = BehaviorAnalyzer.evaluateRealTimeMotion(
            motionData = motion(pitch = 44f, roll = 58f),
            previousMotionData = motion(pitch = 42f, roll = 3f),
            baselinePitch = 42f,
            baselineRoll = 3f,
            baselineGyroMagnitudeMean = 0.15f,
            baselineGyroMagnitudeStdDev = 0.04f
        )

        assertTrue(result.isCriticalSpike)
        assertTrue(result.riskScore >= BehaviorAnalyzer.CRITICAL_THRESHOLD)
    }

    @Test
    fun normalMotionIsNotSpike() {
        val result = BehaviorAnalyzer.evaluateRealTimeMotion(
            motionData = motion(gyroX = 0.05f, pitch = 44f, roll = 4f),
            previousMotionData = motion(pitch = 43f, roll = 3f),
            baselinePitch = 42f,
            baselineRoll = 3f,
            baselineGyroMagnitudeMean = 0.15f,
            baselineGyroMagnitudeStdDev = 0.04f
        )

        assertFalse(result.isHighSpike)
        assertFalse(result.isCriticalSpike)
    }

    private fun motion(
        gyroX: Float = 0.02f,
        gyroY: Float = 0.02f,
        gyroZ: Float = 0.02f,
        pitch: Float,
        roll: Float
    ) = MotionData.create(
        sessionId = "session",
        accelX = 0.1f,
        accelY = 0.2f,
        accelZ = 9.7f,
        gyroX = gyroX,
        gyroY = gyroY,
        gyroZ = gyroZ,
        pitch = pitch,
        roll = roll,
        azimuth = 0f
    )
}
