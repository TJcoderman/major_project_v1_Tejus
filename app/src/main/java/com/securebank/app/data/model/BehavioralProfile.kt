package com.securebank.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ============================================
 * BEHAVIORAL PROFILE
 * ============================================
 * Stores per-user enrollment behavioral baseline.
 * Computed from guided calibration during signup.
 * Used by BehaviorAnalyzer as the reference baseline
 * instead of learning from the live banking session.
 */
@Entity(tableName = "behavioral_profiles")
data class BehavioralProfile(
    @PrimaryKey
    val username: String,              // FK to users table

    // ── Touch Pressure ──
    val pressureMean: Float = 0f,
    val pressureStd: Float = 0f,

    // ── Touch Area / Proxy ──
    val touchAreaMean: Float = 0f,
    val touchAreaStd: Float = 0f,

    // ── Touch Duration ──
    val durationMean: Float = 0f,
    val durationStd: Float = 0f,

    // ── Hold Duration ──
    val holdDurationMean: Float = 0f,
    val holdDurationStd: Float = 0f,

    // ── Swipe Velocity ──
    val velocityMean: Float = 0f,
    val velocityStd: Float = 0f,

    // ── Swipe Acceleration ──
    val accelerationMean: Float = 0f,
    val accelerationStd: Float = 0f,

    // ── Gesture Ratios ──
    val tapRatio: Float = 0f,
    val swipeRatio: Float = 0f,
    val longPressRatio: Float = 0f,

    // ── Inter-Touch Interval ──
    val interTouchIntervalMean: Float = 0f,
    val interTouchIntervalStd: Float = 0f,

    // ── PIN Keystroke Stats ──
    val pinDwellMean: Float = 0f,
    val pinDwellStd: Float = 0f,
    val pinFlightMean: Float = 0f,
    val pinFlightStd: Float = 0f,

    // ── Motion / Orientation ──
    val pitchMean: Float = 0f,
    val pitchStd: Float = 0f,
    val rollMean: Float = 0f,
    val rollStd: Float = 0f,

    // ── Motion Magnitudes ──
    val gyroMagnitudeMean: Float = 0f,
    val gyroMagnitudeStd: Float = 0f,
    val accelMagnitudeMean: Float = 0f,
    val accelMagnitudeStd: Float = 0f,

    // ── Device State ──
    val baselineDeviceState: String = DeviceState.UNKNOWN.name,

    // ── Metadata ──
    val enrolledAt: Long = System.currentTimeMillis(),
    val sampleCount: Int = 0           // Total enrollment touch + PIN samples collected
)
