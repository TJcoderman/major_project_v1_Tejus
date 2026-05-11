package com.securebank.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securebank.app.data.model.*
import com.securebank.app.data.repository.BehavioralRepository
import com.securebank.app.data.repository.UserRepository
import com.securebank.app.sensor.SensorDataCollector
import com.securebank.app.sensor.TouchDataCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * ============================================
 * SIGNUP VIEW MODEL
 * ============================================
 * Manages the account creation + guided behavioral enrollment flow.
 *
 * State machine:
 *   FORM → PIN_ENTRY → TAP_TARGETS → SWIPE_TEST → HOLD_PHONE → COMPLETE
 */
@HiltViewModel
class SignupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val behavioralRepository: BehavioralRepository,
    private val touchDataCollector: TouchDataCollector,
    private val sensorDataCollector: SensorDataCollector
) : ViewModel() {

    companion object {
        private const val TAG = "SignupViewModel"
        private const val MIN_PIN_ENTRIES = 2
        private const val MIN_TAP_SAMPLES = 8
        private const val MIN_SWIPE_SAMPLES = 4
        private const val HOLD_DURATION_SECONDS = 4
    }

    // ── Form fields ──
    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _pin = MutableStateFlow("")
    val pin: StateFlow<String> = _pin.asStateFlow()

    private val _accountNumber = MutableStateFlow("")
    val accountNumber: StateFlow<String> = _accountNumber.asStateFlow()

    // ── Enrollment state ──
    enum class EnrollmentStep {
        FORM, PIN_ENTRY, TAP_TARGETS, SWIPE_TEST, HOLD_PHONE, SAVING, COMPLETE
    }

    private val _currentStep = MutableStateFlow(EnrollmentStep.FORM)
    val currentStep: StateFlow<EnrollmentStep> = _currentStep.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _signupSuccess = MutableStateFlow(false)
    val signupSuccess: StateFlow<Boolean> = _signupSuccess.asStateFlow()

    // ── PIN enrollment ──
    private val _currentPinInput = MutableStateFlow("")
    val currentPinInput: StateFlow<String> = _currentPinInput.asStateFlow()

    private val _pinAttemptNumber = MutableStateFlow(1)
    val pinAttemptNumber: StateFlow<Int> = _pinAttemptNumber.asStateFlow()

    private val _pinEntriesCompleted = MutableStateFlow(0)
    val pinEntriesCompleted: StateFlow<Int> = _pinEntriesCompleted.asStateFlow()

    private val enrollmentPinKeystrokes = mutableListOf<PinKeystrokeEvent>()

    // ── Tap enrollment ──
    private val _tapCount = MutableStateFlow(0)
    val tapCount: StateFlow<Int> = _tapCount.asStateFlow()

    private val enrollmentTouches = mutableListOf<TouchData>()

    // ── Swipe enrollment ──
    private val _swipeCount = MutableStateFlow(0)
    val swipeCount: StateFlow<Int> = _swipeCount.asStateFlow()

    // ── Hold phone ──
    private val _holdSecondsRemaining = MutableStateFlow(HOLD_DURATION_SECONDS)
    val holdSecondsRemaining: StateFlow<Int> = _holdSecondsRemaining.asStateFlow()

    private val _holdComplete = MutableStateFlow(false)
    val holdComplete: StateFlow<Boolean> = _holdComplete.asStateFlow()

    private val enrollmentMotion = mutableListOf<MotionData>()
    private var motionCollectionJob: Job? = null
    private var enrollmentSessionId = "enrollment_${System.currentTimeMillis()}"

    // ── Step progress ──
    private val _stepProgress = MutableStateFlow("")
    val stepProgress: StateFlow<String> = _stepProgress.asStateFlow()

    // ── Form actions ──
    fun onFullNameChanged(value: String) { _fullName.value = value }
    fun onUsernameChanged(value: String) { _username.value = value }
    fun onPasswordChanged(value: String) { _password.value = value }
    fun onPinChanged(value: String) {
        if (value.length <= 6 && value.all { it.isDigit() }) {
            _pin.value = value
        }
    }
    fun onAccountNumberChanged(value: String) { _accountNumber.value = value }
    fun clearError() { _errorMessage.value = null }

    /**
     * Validates form and moves to enrollment steps.
     */
    fun submitForm() {
        viewModelScope.launch {
            _errorMessage.value = null
            _isLoading.value = true

            val name = _fullName.value.trim()
            val user = _username.value.trim()
            val pass = _password.value
            val pinVal = _pin.value
            val acctNum = _accountNumber.value.trim()

            // Validation
            when {
                name.length < 2 -> {
                    _errorMessage.value = "Full name is required"
                    _isLoading.value = false
                    return@launch
                }
                user.length < 3 -> {
                    _errorMessage.value = "Username must be at least 3 characters"
                    _isLoading.value = false
                    return@launch
                }
                pass.length < 6 -> {
                    _errorMessage.value = "Password must be at least 6 characters"
                    _isLoading.value = false
                    return@launch
                }
                pinVal.length != 6 -> {
                    _errorMessage.value = "PIN must be exactly 6 digits"
                    _isLoading.value = false
                    return@launch
                }
                acctNum.length < 3 -> {
                    _errorMessage.value = "Account number must be at least 3 characters"
                    _isLoading.value = false
                    return@launch
                }
                userRepository.usernameExists(user) -> {
                    _errorMessage.value = "Username \"$user\" is already taken"
                    _isLoading.value = false
                    return@launch
                }
                userRepository.accountNumberExists(acctNum) -> {
                    _errorMessage.value = "Account number \"$acctNum\" is already taken"
                    _isLoading.value = false
                    return@launch
                }
            }

            _isLoading.value = false
            _currentStep.value = EnrollmentStep.PIN_ENTRY
            _stepProgress.value = "Enter your PIN ${MIN_PIN_ENTRIES} times"
        }
    }

    // ── PIN Enrollment ──

    fun onPinDigitPressed(event: PinKeystrokeEvent) {
        enrollmentPinKeystrokes.add(event)
        _currentPinInput.value = _currentPinInput.value + event.digit.toString()
    }

    fun onPinBackspace() {
        if (_currentPinInput.value.isNotEmpty()) {
            _currentPinInput.value = _currentPinInput.value.dropLast(1)
        }
    }

    fun onPinEntryComplete(enteredPin: String) {
        if (enteredPin == _pin.value) {
            val completed = _pinEntriesCompleted.value + 1
            _pinEntriesCompleted.value = completed
            _stepProgress.value = "PIN correct! ($completed/$MIN_PIN_ENTRIES)"

            if (completed >= MIN_PIN_ENTRIES) {
                // Move to tap targets
                _currentStep.value = EnrollmentStep.TAP_TARGETS
                _stepProgress.value = "Tap the targets below ($MIN_TAP_SAMPLES needed)"
                // Start touch collection for tap phase
                touchDataCollector.startCollection(enrollmentSessionId)
            }
        } else {
            _stepProgress.value = "Incorrect PIN, try again"
        }

        // Reset PIN input for next attempt
        _currentPinInput.value = ""
        _pinAttemptNumber.value = _pinAttemptNumber.value + 1
    }

    // ── Tap Enrollment ──

    fun onEnrollmentTouchEvent(touchData: TouchData) {
        enrollmentTouches.add(touchData)
        val step = _currentStep.value

        if (step == EnrollmentStep.TAP_TARGETS) {
            if (touchData.touchType == TouchType.TAP || touchData.touchType == TouchType.LONG_PRESS) {
                val count = _tapCount.value + 1
                _tapCount.value = count
                _stepProgress.value = "Taps: $count/$MIN_TAP_SAMPLES"

                if (count >= MIN_TAP_SAMPLES) {
                    _currentStep.value = EnrollmentStep.SWIPE_TEST
                    _stepProgress.value = "Swipe in different directions ($MIN_SWIPE_SAMPLES needed)"
                }
            }
        } else if (step == EnrollmentStep.SWIPE_TEST) {
            val isSwipe = touchData.touchType in listOf(
                TouchType.SWIPE_UP, TouchType.SWIPE_DOWN,
                TouchType.SWIPE_LEFT, TouchType.SWIPE_RIGHT,
                TouchType.SCROLL
            )
            if (isSwipe) {
                val count = _swipeCount.value + 1
                _swipeCount.value = count
                _stepProgress.value = "Swipes: $count/$MIN_SWIPE_SAMPLES"

                if (count >= MIN_SWIPE_SAMPLES) {
                    touchDataCollector.stopCollection()
                    startHoldPhonePhase()
                }
            }
        }
    }

    // ── Hold Phone Enrollment ──

    private fun startHoldPhonePhase() {
        _currentStep.value = EnrollmentStep.HOLD_PHONE
        _holdSecondsRemaining.value = HOLD_DURATION_SECONDS
        _stepProgress.value = "Hold your phone naturally for ${HOLD_DURATION_SECONDS}s"

        // Collect motion data
        if (sensorDataCollector.areSensorsAvailable()) {
            motionCollectionJob = viewModelScope.launch {
                sensorDataCollector.startCollection(enrollmentSessionId).collect { motionData ->
                    enrollmentMotion.add(motionData)
                }
            }
        }

        // Countdown timer
        viewModelScope.launch {
            for (i in HOLD_DURATION_SECONDS downTo 1) {
                _holdSecondsRemaining.value = i
                delay(1000)
            }
            _holdSecondsRemaining.value = 0
            _holdComplete.value = true

            // Stop motion collection
            motionCollectionJob?.cancel()
            sensorDataCollector.stopCollection()

            _stepProgress.value = "Enrollment complete! Saving..."

            // Finalize enrollment
            finalizeEnrollment()
        }
    }

    // ── Finalize ──

    private fun finalizeEnrollment() {
        viewModelScope.launch {
            _currentStep.value = EnrollmentStep.SAVING
            _isLoading.value = true

            try {
                // 1. Create the user
                val user = User(
                    username = _username.value.trim(),
                    passwordHash = _password.value, // Demo: store plaintext like seeded users
                    fullName = _fullName.value.trim(),
                    accountNumber = _accountNumber.value.trim(),
                    balance = 50000.0,
                    pin = _pin.value,
                    enrollmentComplete = true
                )
                userRepository.createUser(user)

                // 2. Compute and save behavioral profile
                val profile = computeProfile(user.username)
                behavioralRepository.saveProfile(profile)

                Log.d(TAG, "Enrollment complete for ${user.username}: " +
                    "${enrollmentPinKeystrokes.size} keystrokes, " +
                    "${enrollmentTouches.size} touches, " +
                    "${enrollmentMotion.size} motion samples")

                _isLoading.value = false
                _currentStep.value = EnrollmentStep.COMPLETE
                _signupSuccess.value = true
                _stepProgress.value = "Account created successfully!"

            } catch (e: Exception) {
                Log.e(TAG, "Enrollment failed", e)
                _errorMessage.value = "Account creation failed: ${e.message}"
                _isLoading.value = false
                _currentStep.value = EnrollmentStep.FORM
            }
        }
    }

    /**
     * Computes a BehavioralProfile from the enrollment data.
     */
    private fun computeProfile(username: String): BehavioralProfile {
        // ── Touch stats ──
        val pressures = enrollmentTouches.map { it.pressure }
        val areas = enrollmentTouches.map { if (it.touchArea > 0f) it.touchArea else it.touchSize }
        val durations = enrollmentTouches.map { it.duration.toFloat() }
        val holdDurations = enrollmentTouches.map { it.holdDuration.toFloat() }
        val velocities = enrollmentTouches.filter { it.velocity > 0f }.map { it.velocity }
        val accels = enrollmentTouches.filter { it.acceleration > 0f }.map { it.acceleration }

        // Gesture ratios
        val total = enrollmentTouches.size.toFloat().coerceAtLeast(1f)
        val taps = enrollmentTouches.count {
            it.touchType == TouchType.TAP || it.touchType == TouchType.LONG_PRESS
        }
        val swipes = enrollmentTouches.count {
            it.touchType in listOf(TouchType.SWIPE_UP, TouchType.SWIPE_DOWN,
                TouchType.SWIPE_LEFT, TouchType.SWIPE_RIGHT, TouchType.SCROLL)
        }
        val longPresses = enrollmentTouches.count { it.touchType == TouchType.LONG_PRESS }

        // Inter-touch intervals
        val intervals = if (enrollmentTouches.size >= 2) {
            enrollmentTouches.zipWithNext().map {
                (it.second.timestamp - it.first.timestamp).toFloat()
            }
        } else emptyList()

        // ── PIN keystroke stats ──
        val dwells = enrollmentPinKeystrokes.map { it.dwellTime.toFloat() }
        val flights = enrollmentPinKeystrokes.filter { it.flightTime > 0 }.map { it.flightTime.toFloat() }

        // ── Motion stats ──
        val pitches = enrollmentMotion.map { it.pitch }
        val rolls = enrollmentMotion.map { it.roll }
        val gyroMags = enrollmentMotion.map {
            sqrt(it.gyroX * it.gyroX + it.gyroY * it.gyroY + it.gyroZ * it.gyroZ)
        }
        val accelMags = enrollmentMotion.map {
            sqrt(it.accelX * it.accelX + it.accelY * it.accelY + it.accelZ * it.accelZ)
        }

        // Device state baseline
        val states = enrollmentMotion.map { it.deviceStateEnum }
        val mostCommonState = states.groupBy { it }.maxByOrNull { it.value.size }?.key
            ?: DeviceState.UNKNOWN

        return BehavioralProfile(
            username = username,
            pressureMean = pressures.meanSafe(),
            pressureStd = pressures.stdSafe(),
            touchAreaMean = areas.meanSafe(),
            touchAreaStd = areas.stdSafe(),
            durationMean = durations.meanSafe(),
            durationStd = durations.stdSafe(),
            holdDurationMean = holdDurations.meanSafe(),
            holdDurationStd = holdDurations.stdSafe(),
            velocityMean = velocities.meanSafe(),
            velocityStd = velocities.stdSafe(),
            accelerationMean = accels.meanSafe(),
            accelerationStd = accels.stdSafe(),
            tapRatio = taps / total,
            swipeRatio = swipes / total,
            longPressRatio = longPresses / total,
            interTouchIntervalMean = intervals.meanSafe(),
            interTouchIntervalStd = intervals.stdSafe(),
            pinDwellMean = dwells.meanSafe(),
            pinDwellStd = dwells.stdSafe(),
            pinFlightMean = flights.meanSafe(),
            pinFlightStd = flights.stdSafe(),
            pitchMean = pitches.meanSafe(),
            pitchStd = pitches.stdSafe(),
            rollMean = rolls.meanSafe(),
            rollStd = rolls.stdSafe(),
            gyroMagnitudeMean = gyroMags.meanSafe(),
            gyroMagnitudeStd = gyroMags.stdSafe(),
            accelMagnitudeMean = accelMags.meanSafe(),
            accelMagnitudeStd = accelMags.stdSafe(),
            baselineDeviceState = mostCommonState.name,
            sampleCount = enrollmentPinKeystrokes.size + enrollmentTouches.size
        )
    }

    // ── Stat helpers ──

    private fun List<Float>.meanSafe(): Float =
        if (isEmpty()) 0f else (sum() / size)

    private fun List<Float>.stdSafe(): Float {
        if (size < 2) return 0f
        val m = meanSafe()
        val variance = map { (it - m) * (it - m) }.sum() / size
        return sqrt(variance)
    }

    override fun onCleared() {
        super.onCleared()
        motionCollectionJob?.cancel()
        touchDataCollector.stopCollection()
        sensorDataCollector.stopCollection()
    }
}
