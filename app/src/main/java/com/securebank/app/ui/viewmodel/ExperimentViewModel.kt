package com.securebank.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securebank.app.data.export.DataExporter
import com.securebank.app.data.model.*
import com.securebank.app.data.repository.BehavioralRepository
import com.securebank.app.domain.FeatureExtractor
import com.securebank.app.sensor.KeystrokeCollector
import com.securebank.app.sensor.SensorDataCollector
import com.securebank.app.sensor.TouchDataCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ============================================
 * EXPERIMENT VIEW MODEL
 * ============================================
 * Manages the controlled experiment workflow:
 * - Participant registration
 * - Enrollment sessions (baseline capture)
 * - Genuine sessions (same user)
 * - Impostor sessions (different user)
 * - Data export for ML training
 */
@HiltViewModel
class ExperimentViewModel @Inject constructor(
    private val behavioralRepository: BehavioralRepository,
    private val keystrokeCollector: KeystrokeCollector,
    val touchDataCollector: TouchDataCollector,
    private val sensorDataCollector: SensorDataCollector,
    private val dataExporter: DataExporter,
    private val featureExtractor: FeatureExtractor
) : ViewModel() {

    // ========================
    // STATE
    // ========================

    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants.asStateFlow()

    private val _currentParticipant = MutableStateFlow<Participant?>(null)
    val currentParticipant: StateFlow<Participant?> = _currentParticipant.asStateFlow()

    private val _currentSession = MutableStateFlow<ExperimentSession?>(null)
    val currentSession: StateFlow<ExperimentSession?> = _currentSession.asStateFlow()

    private val _currentTask = MutableStateFlow(ExperimentTaskType.PIN_ENTRY)
    val currentTask: StateFlow<ExperimentTaskType> = _currentTask.asStateFlow()

    // PIN state
    private val _currentPin = MutableStateFlow("")
    val currentPin: StateFlow<String> = _currentPin.asStateFlow()

    private val _pinAttemptNumber = MutableStateFlow(1)
    val pinAttemptNumber: StateFlow<Int> = _pinAttemptNumber.asStateFlow()

    private val _pinAttemptResults = MutableStateFlow<List<PinAttemptResult>>(emptyList())
    val pinAttemptResults: StateFlow<List<PinAttemptResult>> = _pinAttemptResults.asStateFlow()

    // Text typing state
    private val _currentPromptIndex = MutableStateFlow(0)
    val currentPromptIndex: StateFlow<Int> = _currentPromptIndex.asStateFlow()

    private val _typedText = MutableStateFlow("")
    val typedText: StateFlow<String> = _typedText.asStateFlow()

    // Session progress
    private val _sessionProgress = MutableStateFlow(0f)
    val sessionProgress: StateFlow<Float> = _sessionProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportPath = MutableStateFlow<String?>(null)
    val exportPath: StateFlow<String?> = _exportPath.asStateFlow()

    // All sessions for experiment tracking
    private val _allSessions = MutableStateFlow<List<ExperimentSession>>(emptyList())

    // Collected PIN keystrokes for current session
    private val currentPinKeystrokes = mutableListOf<PinKeystrokeEvent>()

    // ========================
    // PARTICIPANT MANAGEMENT
    // ========================

    fun addParticipant(name: String) {
        val id = "P${(_participants.value.size + 1).toString().padStart(2, '0')}"
        val participant = Participant(
            participantId = id,
            name = name
        )
        _participants.value = _participants.value + participant
        _statusMessage.value = "Participant $id ($name) added"
    }

    fun selectParticipant(participant: Participant) {
        _currentParticipant.value = participant
    }

    // ========================
    // SESSION MANAGEMENT
    // ========================

    /**
     * Starts a new experiment session.
     * @param profileOwnerId Whose profile is being tested
     *                       (same as participantId for genuine, different for impostor)
     */
    fun startSession(sessionType: ExperimentSessionType, profileOwnerId: String? = null) {
        val participant = _currentParticipant.value ?: return
        val sessionId = UUID.randomUUID().toString()
        val ownerId = profileOwnerId ?: participant.participantId

        val session = ExperimentSession(
            sessionId = sessionId,
            participantId = participant.participantId,
            profileOwnerId = ownerId,
            sessionType = sessionType
        )

        _currentSession.value = session
        _currentTask.value = ExperimentTaskType.PIN_ENTRY
        _currentPin.value = ""
        _pinAttemptNumber.value = 1
        _pinAttemptResults.value = emptyList()
        _currentPromptIndex.value = 0
        _typedText.value = ""
        _sessionProgress.value = 0f
        currentPinKeystrokes.clear()

        // Start behavioral collection
        keystrokeCollector.startCollection(sessionId, isBaseline = sessionType == ExperimentSessionType.ENROLLMENT)
        touchDataCollector.startCollection(sessionId)

        // Start sensor collection
        viewModelScope.launch {
            sensorDataCollector.startCollection(sessionId).collect { motionData ->
                behavioralRepository.saveMotion(motionData)
            }
        }

        // Collect keystroke events
        viewModelScope.launch {
            keystrokeCollector.keystrokeEvents.collect { keystrokeData ->
                behavioralRepository.saveKeystroke(keystrokeData)
            }
        }

        // Create behavioral session record
        viewModelScope.launch {
            val behavioralSession = BehavioralSession(
                sessionId = sessionId,
                userId = participant.participantId,
                isBaseline = sessionType == ExperimentSessionType.ENROLLMENT
            )
            behavioralRepository.createSession(behavioralSession)
        }

        _statusMessage.value = "${sessionType.name} session started for ${participant.name}"
    }

    // ========================
    // PIN ENTRY TASK
    // ========================

    fun onPinDigitPressed(event: PinKeystrokeEvent) {
        currentPinKeystrokes.add(event)
        _currentPin.value = _currentPin.value + event.digit.toString()
    }

    fun onPinBackspace() {
        if (_currentPin.value.isNotEmpty()) {
            _currentPin.value = _currentPin.value.dropLast(1)
        }
    }

    fun onPinComplete(pin: String) {
        val session = _currentSession.value ?: return
        val isEnrollment = session.sessionType == ExperimentSessionType.ENROLLMENT
        val maxAttempts = if (isEnrollment) PinConfig.PIN_REPETITIONS_ENROLLMENT
                          else PinConfig.PIN_REPETITIONS_SESSION

        // Calculate statistics for this attempt
        val attemptKeystrokes = currentPinKeystrokes.filter {
            it.pinAttemptNumber == _pinAttemptNumber.value
        }

        val dwellTimes = attemptKeystrokes.map { it.dwellTime.toFloat() }
        val flightTimes = attemptKeystrokes.filter { it.flightTime > 0 }.map { it.flightTime.toFloat() }

        val result = PinAttemptResult(
            attemptNumber = _pinAttemptNumber.value,
            enteredPin = pin,
            isCorrect = pin == PinConfig.DEFAULT_PIN,
            totalDuration = if (attemptKeystrokes.isNotEmpty())
                attemptKeystrokes.last().keyUpTime - attemptKeystrokes.first().keyDownTime
            else 0L,
            keystrokes = attemptKeystrokes,
            avgDwellTime = if (dwellTimes.isNotEmpty()) dwellTimes.average().toFloat() else 0f,
            avgFlightTime = if (flightTimes.isNotEmpty()) flightTimes.average().toFloat() else 0f,
            stdDwellTime = stdDev(dwellTimes),
            stdFlightTime = stdDev(flightTimes)
        )

        _pinAttemptResults.value = _pinAttemptResults.value + result

        if (_pinAttemptNumber.value >= maxAttempts) {
            // PIN task complete, move to next task
            advanceTask()
        } else {
            // Reset for next attempt
            _pinAttemptNumber.value = _pinAttemptNumber.value + 1
            _currentPin.value = ""
        }

        _sessionProgress.value = calculateProgress()
    }

    // ========================
    // TEXT TYPING TASK
    // ========================

    fun onTextChanged(oldText: String, newText: String) {
        _typedText.value = newText
        viewModelScope.launch {
            keystrokeCollector.onTextChanged(oldText, newText)
        }
    }

    fun submitTypedText() {
        val session = _currentSession.value ?: return
        val isEnrollment = session.sessionType == ExperimentSessionType.ENROLLMENT
        val prompts = if (isEnrollment) PromptedTexts.enrollmentTexts
                      else PromptedTexts.sessionTexts

        if (_currentPromptIndex.value < prompts.size - 1) {
            _currentPromptIndex.value = _currentPromptIndex.value + 1
            _typedText.value = ""
        } else {
            // Text task complete
            advanceTask()
        }

        _sessionProgress.value = calculateProgress()
    }

    // ========================
    // TASK PROGRESSION
    // ========================

    private fun advanceTask() {
        when (_currentTask.value) {
            ExperimentTaskType.PIN_ENTRY -> {
                _currentTask.value = ExperimentTaskType.TEXT_TYPING
                _typedText.value = ""
                _currentPromptIndex.value = 0
            }
            ExperimentTaskType.TEXT_TYPING -> {
                _currentTask.value = ExperimentTaskType.TOUCH_INTERACTION
            }
            ExperimentTaskType.TOUCH_INTERACTION -> {
                _currentTask.value = ExperimentTaskType.FREE_BROWSING
            }
            ExperimentTaskType.FREE_BROWSING -> {
                completeSession()
            }
        }
    }

    fun completeTouchTask() {
        advanceTask()
        _sessionProgress.value = calculateProgress()
    }

    fun completeFreeBrowsingTask() {
        advanceTask()
        _sessionProgress.value = calculateProgress()
    }

    private fun completeSession() {
        val session = _currentSession.value ?: return

        // Stop all collectors
        keystrokeCollector.stopCollection()
        touchDataCollector.stopCollection()
        sensorDataCollector.stopCollection()

        // Update session
        val completedSession = session.copy(
            endTime = System.currentTimeMillis(),
            isComplete = true,
            tasksCompleted = 4
        )
        _currentSession.value = completedSession
        _allSessions.value = _allSessions.value + completedSession

        // Update participant
        _currentParticipant.value?.let { p ->
            val updated = p.copy(
                sessionCount = p.sessionCount + 1,
                enrollmentComplete = p.enrollmentComplete || session.sessionType == ExperimentSessionType.ENROLLMENT
            )
            _currentParticipant.value = updated
            _participants.value = _participants.value.map {
                if (it.participantId == p.participantId) updated else it
            }
        }

        _sessionProgress.value = 1f
        _statusMessage.value = "Session complete! Data ready for export."

        // Auto-export
        exportCurrentSession()
    }

    // ========================
    // DATA EXPORT
    // ========================

    private fun exportCurrentSession() {
        val session = _currentSession.value ?: return
        val participant = _currentParticipant.value ?: return

        viewModelScope.launch {
            _isExporting.value = true
            try {
                val path = dataExporter.exportSession(
                    sessionId = session.sessionId,
                    participantId = participant.participantId,
                    profileOwnerId = session.profileOwnerId,
                    sessionType = session.sessionType,
                    pinKeystrokes = currentPinKeystrokes.toList()
                )
                _exportPath.value = path
                _statusMessage.value = "Data exported to: $path"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportAllData() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val path = dataExporter.exportExperimentSummary(
                    participants = _participants.value,
                    sessions = _allSessions.value
                )
                _exportPath.value = path
                _statusMessage.value = "Full experiment data exported to: $path"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    // ========================
    // FEATURE EXTRACTION
    // ========================

    suspend fun extractFeaturesForSession(sessionId: String): FloatArray {
        val keystrokes = behavioralRepository.getBaselineKeystrokes(sessionId)
        val touches = behavioralRepository.getRecentTouches(sessionId, 500)
        val motion = behavioralRepository.getRecentMotion(sessionId, 500)

        return featureExtractor.extractFeatures(
            keystrokes = keystrokes,
            pinKeystrokes = currentPinKeystrokes.toList(),
            touches = touches,
            motionData = motion
        )
    }

    // ========================
    // HELPERS
    // ========================

    private fun calculateProgress(): Float {
        val taskIndex = ExperimentTaskType.entries.indexOf(_currentTask.value)
        val totalTasks = ExperimentTaskType.entries.size
        return taskIndex.toFloat() / totalTasks
    }

    private fun stdDev(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean).let { d -> d * d } }.average().toFloat()
        return kotlin.math.sqrt(variance)
    }

    fun resetSession() {
        _currentSession.value = null
        _currentTask.value = ExperimentTaskType.PIN_ENTRY
        _currentPin.value = ""
        _pinAttemptNumber.value = 1
        _pinAttemptResults.value = emptyList()
        _typedText.value = ""
        _sessionProgress.value = 0f
        _exportPath.value = null
        currentPinKeystrokes.clear()
    }
}
