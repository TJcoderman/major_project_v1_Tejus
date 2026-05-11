package com.securebank.app.data.model

/**
 * Preloaded behavioral baselines for the seeded reviewer accounts.
 *
 * These make the demo immediately multi-user without forcing a reviewer to run
 * signup enrollment first. Real signup users still get profiles computed from
 * their own guided enrollment samples.
 */
object DemoBehavioralProfiles {
    fun forUsername(username: String): BehavioralProfile? {
        val now = System.currentTimeMillis()
        return when (username.lowercase()) {
            "demo" -> BehavioralProfile(
                username = "demo",
                pressureMean = 0.62f,
                pressureStd = 0.10f,
                touchAreaMean = 0.68f,
                touchAreaStd = 0.12f,
                durationMean = 135f,
                durationStd = 35f,
                holdDurationMean = 55f,
                holdDurationStd = 45f,
                velocityMean = 760f,
                velocityStd = 180f,
                accelerationMean = 520f,
                accelerationStd = 140f,
                tapRatio = 0.68f,
                swipeRatio = 0.24f,
                longPressRatio = 0.04f,
                interTouchIntervalMean = 470f,
                interTouchIntervalStd = 140f,
                pinDwellMean = 92f,
                pinDwellStd = 16f,
                pinFlightMean = 118f,
                pinFlightStd = 28f,
                pitchMean = 43f,
                pitchStd = 8f,
                rollMean = 3f,
                rollStd = 7f,
                gyroMagnitudeMean = 0.10f,
                gyroMagnitudeStd = 0.04f,
                accelMagnitudeMean = 9.82f,
                accelMagnitudeStd = 0.24f,
                baselineDeviceState = DeviceState.HELD_IN_HAND.name,
                enrolledAt = now,
                sampleCount = 64
            )

            "john" -> BehavioralProfile(
                username = "john",
                pressureMean = 0.86f,
                pressureStd = 0.06f,
                touchAreaMean = 1.05f,
                touchAreaStd = 0.16f,
                durationMean = 95f,
                durationStd = 22f,
                holdDurationMean = 35f,
                holdDurationStd = 28f,
                velocityMean = 1180f,
                velocityStd = 210f,
                accelerationMean = 840f,
                accelerationStd = 190f,
                tapRatio = 0.54f,
                swipeRatio = 0.38f,
                longPressRatio = 0.02f,
                interTouchIntervalMean = 310f,
                interTouchIntervalStd = 95f,
                pinDwellMean = 68f,
                pinDwellStd = 11f,
                pinFlightMean = 82f,
                pinFlightStd = 20f,
                pitchMean = 55f,
                pitchStd = 7f,
                rollMean = -8f,
                rollStd = 8f,
                gyroMagnitudeMean = 0.18f,
                gyroMagnitudeStd = 0.07f,
                accelMagnitudeMean = 9.96f,
                accelMagnitudeStd = 0.34f,
                baselineDeviceState = DeviceState.HELD_IN_HAND.name,
                enrolledAt = now,
                sampleCount = 64
            )

            "jane" -> BehavioralProfile(
                username = "jane",
                pressureMean = 0.38f,
                pressureStd = 0.07f,
                touchAreaMean = 0.44f,
                touchAreaStd = 0.09f,
                durationMean = 185f,
                durationStd = 48f,
                holdDurationMean = 90f,
                holdDurationStd = 65f,
                velocityMean = 520f,
                velocityStd = 130f,
                accelerationMean = 320f,
                accelerationStd = 110f,
                tapRatio = 0.78f,
                swipeRatio = 0.16f,
                longPressRatio = 0.06f,
                interTouchIntervalMean = 620f,
                interTouchIntervalStd = 180f,
                pinDwellMean = 132f,
                pinDwellStd = 20f,
                pinFlightMean = 165f,
                pinFlightStd = 36f,
                pitchMean = 32f,
                pitchStd = 9f,
                rollMean = 11f,
                rollStd = 6f,
                gyroMagnitudeMean = 0.07f,
                gyroMagnitudeStd = 0.03f,
                accelMagnitudeMean = 9.72f,
                accelMagnitudeStd = 0.20f,
                baselineDeviceState = DeviceState.HELD_IN_HAND.name,
                enrolledAt = now,
                sampleCount = 64
            )

            else -> null
        }
    }

    fun all(): List<BehavioralProfile> =
        listOfNotNull(forUsername("demo"), forUsername("john"), forUsername("jane"))
}
