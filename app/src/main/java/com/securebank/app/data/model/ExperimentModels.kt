package com.securebank.app.data.model

/**
 * ============================================
 * EXPERIMENT DATA MODELS
 * ============================================
 * Models for the controlled experiment protocol.
 * Supports enrollment, genuine, and impostor sessions
 * for research data collection and ML model training.
 */

/**
 * Represents a participant in the experiment.
 */
data class Participant(
    val participantId: String,       // Unique ID (e.g., "P01", "P02")
    val name: String,                // Display name
    val enrolledAt: Long = System.currentTimeMillis(),
    val enrollmentComplete: Boolean = false,
    val sessionCount: Int = 0
)

/**
 * Type of experiment session.
 * ENROLLMENT: Initial data capture to build user profile
 * GENUINE: Same user interacting (should be accepted)
 * IMPOSTOR: Different user on someone else's profile (should be rejected)
 */
enum class ExperimentSessionType {
    ENROLLMENT,
    GENUINE,
    IMPOSTOR
}

/**
 * Task type within an experiment session.
 * Each session consists of multiple tasks to capture
 * different behavioral modalities.
 */
enum class ExperimentTaskType {
    PIN_ENTRY,          // 6-digit PIN on custom keypad (real dwell time)
    TEXT_TYPING,        // Typing prompted sentences (flight time)
    TOUCH_INTERACTION,  // Tap targets, swipe cards, scroll list
    FREE_BROWSING       // Free interaction with the app (natural behavior)
}

/**
 * Represents a single experiment session.
 */
data class ExperimentSession(
    val sessionId: String,
    val participantId: String,
    val profileOwnerId: String,      // Whose behavioral profile is being tested against
    val sessionType: ExperimentSessionType,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val currentTask: ExperimentTaskType = ExperimentTaskType.PIN_ENTRY,
    val tasksCompleted: Int = 0,
    val totalTasks: Int = 4,
    val isComplete: Boolean = false
)

/**
 * Prompted text for typing tasks.
 * Using standardized sentences ensures consistent features across participants.
 */
object PromptedTexts {
    val enrollmentTexts = listOf(
        "The quick brown fox jumps over the lazy dog near the bank.",
        "Please enter your account details to proceed with the transfer.",
        "Security is our top priority and we protect your financial data."
    )

    val sessionTexts = listOf(
        "Mobile banking makes managing your finances easy and secure.",
        "Always verify the recipient details before confirming a transfer."
    )
}

/**
 * PIN data for PIN entry tasks.
 */
object PinConfig {
    const val PIN_LENGTH = 6
    const val PIN_REPETITIONS_ENROLLMENT = 5  // Enter PIN 5 times during enrollment
    const val PIN_REPETITIONS_SESSION = 3     // Enter PIN 3 times during sessions
    val DEFAULT_PIN = "382946"               // Standardized PIN for all participants
}

/**
 * Keystroke event captured from the custom PIN pad.
 * Unlike the software keyboard, this gives REAL dwell times.
 */
data class PinKeystrokeEvent(
    val sessionId: String,
    val timestamp: Long,
    val digit: Int,                  // The digit pressed (0-9)
    val keyDownTime: Long,           // When finger touched the key
    val keyUpTime: Long,             // When finger lifted from the key
    val dwellTime: Long,             // keyUpTime - keyDownTime (REAL, not estimated)
    val flightTime: Long,            // keyDownTime - previousKeyUpTime
    val touchX: Float,               // X coordinate of touch on the key
    val touchY: Float,               // Y coordinate of touch on the key
    val touchSize: Float,            // Contact area of the touch
    val pinAttemptNumber: Int         // Which PIN attempt (1st, 2nd, etc.)
)

/**
 * Result of a complete PIN entry attempt.
 */
data class PinAttemptResult(
    val attemptNumber: Int,
    val enteredPin: String,
    val isCorrect: Boolean,
    val totalDuration: Long,         // Time from first key to last key
    val keystrokes: List<PinKeystrokeEvent>,
    val avgDwellTime: Float,
    val avgFlightTime: Float,
    val stdDwellTime: Float,
    val stdFlightTime: Float
)

/**
 * Aggregated experiment results for a participant.
 */
data class ParticipantResults(
    val participantId: String,
    val enrollmentSessionId: String,
    val genuineSessionIds: List<String>,
    val impostorSessionIds: List<String>,
    val totalKeystrokes: Int,
    val totalTouchEvents: Int,
    val totalMotionSamples: Int
)
