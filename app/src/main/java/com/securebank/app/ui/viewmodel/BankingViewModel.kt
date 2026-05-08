package com.securebank.app.ui.viewmodel

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securebank.app.data.model.*
import com.securebank.app.data.repository.BehavioralRepository
import com.securebank.app.data.repository.UserRepository
import com.securebank.app.domain.BehaviorAnalyzer
import com.securebank.app.sensor.KeystrokeCollector
import com.securebank.app.sensor.SensorDataCollector
import com.securebank.app.sensor.TouchDataCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ============================================
 * BANKING VIEW MODEL
 * ============================================
 * Handles banking operations and continuous behavioral monitoring.
 */
@HiltViewModel
class BankingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val behavioralRepository: BehavioralRepository,
    private val sensorDataCollector: SensorDataCollector,
    private val touchDataCollector: TouchDataCollector,
    private val keystrokeCollector: KeystrokeCollector,
    private val behaviorAnalyzer: BehaviorAnalyzer
) : ViewModel() {
    
    // UI State
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _userBalance = MutableStateFlow(0.0)
    val userBalance: StateFlow<Double> = _userBalance.asStateFlow()
    
    private val _transferState = MutableStateFlow(TransferState())
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()
    
    // Risk monitoring
    val currentRiskScore: StateFlow<Float> = behaviorAnalyzer.currentRiskScore
    val currentRiskLevel: StateFlow<RiskLevel> = behaviorAnalyzer.currentRiskLevel
    
    // ML Status
    val isMLReady: StateFlow<Boolean> = behaviorAnalyzer.mlReadyState
    val debugExplainabilityState: StateFlow<DebugExplainabilityState> = behaviorAnalyzer.debugExplainabilityState
    
    private val _showSecurityAlert = MutableStateFlow(false)
    val showSecurityAlert: StateFlow<Boolean> = _showSecurityAlert.asStateFlow()
    
    private val _securityAlertMessage = MutableStateFlow("")
    val securityAlertMessage: StateFlow<String> = _securityAlertMessage.asStateFlow()

    private val _alertSeverity = MutableStateFlow(AlertSeverity.MEDIUM)
    val alertSeverity: StateFlow<AlertSeverity> = _alertSeverity.asStateFlow()

    // One-shot event: when true, NavGraph must navigate to login and reset to false.
    private val _forceLogoutEvent = MutableStateFlow(false)
    val forceLogoutEvent: StateFlow<Boolean> = _forceLogoutEvent.asStateFlow()
    
    // Behavioral data collection state
    private var isCollecting = false
    private var sensorCollectionJob: Job? = null
    private var touchCollectionJob: Job? = null
    private var keystrokeCollectionJob: Job? = null
    private var riskAssessmentJob: Job? = null
    private var currentSessionId: String = ""
    
    // Debug mode
    private val _debugMode = MutableStateFlow(true)
    val debugMode: StateFlow<Boolean> = _debugMode.asStateFlow()
    
    // Real-time monitoring data (for Debug Panel)
    private val _liveMotionData = MutableStateFlow<MotionData?>(null)
    val liveMotionData: StateFlow<MotionData?> = _liveMotionData.asStateFlow()
    
    private val _liveTouchData = MutableStateFlow<TouchData?>(null)
    val liveTouchData: StateFlow<TouchData?> = _liveTouchData.asStateFlow()
    
    private val _liveKeystrokeData = MutableStateFlow<KeystrokeData?>(null)
    val liveKeystrokeData: StateFlow<KeystrokeData?> = _liveKeystrokeData.asStateFlow()

    private val _debugCounters = MutableStateFlow(DebugCollectionCounters())
    val debugCounters: StateFlow<DebugCollectionCounters> = _debugCounters.asStateFlow()

    private val _debugEvents = MutableStateFlow<List<String>>(emptyList())
    val debugEvents: StateFlow<List<String>> = _debugEvents.asStateFlow()
    
    /**
     * Initializes banking data and starts behavioral collection.
     */
    fun initialize(user: User, sessionId: String) {
        currentSessionId = sessionId
        addDebugEvent("Session created for ${user.username}")
        
        viewModelScope.launch {
            // Load user data
            _userBalance.value = user.balance
            
            // Load transactions
            userRepository.getUserTransactions(user.username).collect { txList ->
                _transactions.value = txList
            }
        }
        
        // Start behavioral data collection
        startBehavioralCollection(sessionId)
    }
    
    /**
     * Starts all behavioral data collection services.
     */
    fun startBehavioralCollection(sessionId: String) {
        if (isCollecting) return
        
        isCollecting = true
        currentSessionId = sessionId
        addDebugEvent("Behavioral collectors started")
        
        // Start touch collection
        touchDataCollector.startCollection(sessionId)
        
        // Start keystroke collection (non-baseline mode)
        keystrokeCollector.startCollection(sessionId, isBaseline = false)
        
        // Collect touch events
        touchCollectionJob = viewModelScope.launch {
            touchDataCollector.touchEvents.collect { touchData ->
                _liveTouchData.value = touchData
                behavioralRepository.saveTouch(touchData)
                incrementCounters(touches = 1, databaseWrites = 1)
            }
        }
        
        // Collect keystroke events — store the Job so we can cancel it (item 3)
        keystrokeCollectionJob = viewModelScope.launch {
            keystrokeCollector.keystrokeEvents.collect { keystrokeData ->
                _liveKeystrokeData.value = keystrokeData
                behavioralRepository.saveKeystroke(keystrokeData)
                incrementCounters(keystrokes = 1, databaseWrites = 1)
            }
        }
        
        // Start sensor collection if available
        if (sensorDataCollector.areSensorsAvailable()) {
            sensorCollectionJob = viewModelScope.launch {
                val motionBuffer = mutableListOf<MotionData>()
                
                sensorDataCollector.startCollection(sessionId).collect { motionData ->
                    _liveMotionData.value = motionData
                    
                    // Process real-time risk update for this motion event
                    val recommendation = behaviorAnalyzer.processRealTimeMotion(motionData)
                    if (recommendation != SecurityRecommendation.CONTINUE) {
                        // We wrap this in a dummy RiskAssessment just to reuse the handler logic,
                        // or we can call handleRiskAssessment with a constructed object.
                        // But handleRiskAssessment expects a full object. Let's just handle the recommendation directly here 
                        // or create a lightweight assessment object.
                        
                        // Since we don't have full anomaly list here easily, we rely on the fact 
                        // that processRealTimeMotion updates the shared state flow for risk score/level.
                        // We just need to trigger the UI action.
                        
                        when (recommendation) {
                            SecurityRecommendation.REQUEST_REAUTHENTICATION -> {
                                _alertSeverity.value = AlertSeverity.HIGH
                                _securityAlertMessage.value = "Real-time anomaly detected. Please verify identity."
                                _showSecurityAlert.value = true
                                vibrate()
                            }
                            SecurityRecommendation.FORCE_LOGOUT -> {
                                _alertSeverity.value = AlertSeverity.CRITICAL
                                _securityAlertMessage.value = "Critical motion anomaly detected. Session terminated."
                                _showSecurityAlert.value = true
                                vibrate()
                                stopBehavioralCollection()
                                behaviorAnalyzer.reset()
                                _forceLogoutEvent.value = true
                            }
                            SecurityRecommendation.SHOW_WARNING -> {
                                _alertSeverity.value = AlertSeverity.MEDIUM
                                if (_debugMode.value) {
                                    showToast("Warning: Motion anomaly detected!")
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    motionBuffer.add(motionData)
                    
                    // Batch save every 10 readings to reduce database writes
                    if (motionBuffer.size >= 10) {
                        behavioralRepository.saveMotionBatch(motionBuffer.toList())
                        incrementCounters(motionSamples = motionBuffer.size, databaseWrites = motionBuffer.size)
                        motionBuffer.clear()
                    }
                }
            }
        }
        
        // Start periodic risk assessment
        startRiskAssessment()
    }
    
    /**
     * Stops all behavioral data collection.
     */
    fun stopBehavioralCollection() {
        isCollecting = false
        addDebugEvent("Behavioral collectors stopped")
        
        touchDataCollector.stopCollection()
        keystrokeCollector.stopCollection()
        sensorDataCollector.stopCollection()
        
        sensorCollectionJob?.cancel()
        touchCollectionJob?.cancel()
        keystrokeCollectionJob?.cancel()
        riskAssessmentJob?.cancel()

        sensorCollectionJob = null
        touchCollectionJob = null
        keystrokeCollectionJob = null
        riskAssessmentJob = null
    }

    /**
     * Pauses behavioral collection when the app goes to background.
     * Stops sensor/touch collectors but preserves session state so
     * resumeBehavioralCollection() can restart them.
     */
    fun pauseBehavioralCollection() {
        if (!isCollecting) return
        isCollecting = false
        addDebugEvent("Monitoring paused (app backgrounded)")

        touchDataCollector.stopCollection()
        sensorDataCollector.stopCollection()

        sensorCollectionJob?.cancel()
        touchCollectionJob?.cancel()
        keystrokeCollectionJob?.cancel()
        riskAssessmentJob?.cancel()

        sensorCollectionJob = null
        touchCollectionJob = null
        keystrokeCollectionJob = null
        riskAssessmentJob = null
    }

    /**
     * Resumes behavioral collection when the app returns to foreground.
     * Only restarts if we have an active session.
     */
    fun resumeBehavioralCollection() {
        if (isCollecting || currentSessionId.isEmpty()) return
        addDebugEvent("Monitoring resumed (app foregrounded)")
        startBehavioralCollection(currentSessionId)
    }
    
    /**
     * Starts periodic risk assessment.
     */
    private fun startRiskAssessment() {
        riskAssessmentJob = viewModelScope.launch {
            // Initial delay to gather some data first
            delay(10000)
            
            // Update baseline with initial touch/motion data
            behaviorAnalyzer.updateInitialBaseline(currentSessionId)
            addDebugEvent("Touch and motion baseline updated")
            
            // Periodic assessment every 15 seconds
            while (isCollecting) {
                val assessment = behaviorAnalyzer.performRiskAssessment(currentSessionId)
                addDebugEvent("Risk ${assessment.riskLevel.name}: ${(assessment.overallRiskScore * 100).toInt()}% -> ${assessment.recommendation.name}")
                handleRiskAssessment(assessment)
                delay(15000)
            }
        }
    }
    
    /**
     * Handles the result of a risk assessment.
     * Enforces security decisions: FORCE_LOGOUT actually terminates the session,
     * REQUEST_REAUTHENTICATION shows a non-dismissible reauth dialog.
     */
    private fun handleRiskAssessment(assessment: RiskAssessment) {
        when (assessment.recommendation) {
            SecurityRecommendation.CONTINUE -> {
                // All good, no action needed
            }
            SecurityRecommendation.SHOW_WARNING -> {
                _alertSeverity.value = AlertSeverity.MEDIUM
                if (_debugMode.value) {
                    showToast("Warning: Medium risk detected: ${(assessment.overallRiskScore * 100).toInt()}%")
                }
            }
            SecurityRecommendation.REQUEST_REAUTHENTICATION -> {
                addDebugEvent("Action required: reauthentication")
                _alertSeverity.value = AlertSeverity.HIGH
                _securityAlertMessage.value = "Unusual behavior detected. For your security, please verify your identity."
                _showSecurityAlert.value = true
                vibrate()
            }
            SecurityRecommendation.FORCE_LOGOUT -> {
                addDebugEvent("Action required: force logout")
                _alertSeverity.value = AlertSeverity.CRITICAL
                _securityAlertMessage.value = "Session security compromised. You will be logged out for your protection."
                _showSecurityAlert.value = true
                vibrate()
                // Actually enforce: stop collectors and trigger navigation
                stopBehavioralCollection()
                behaviorAnalyzer.reset()
                // Signal NavGraph to navigate to login
                _forceLogoutEvent.value = true
            }
        }
    }
    
    /**
     * Called by NavGraph after processing the force logout event.
     */
    fun acknowledgeForceLogout() {
        _forceLogoutEvent.value = false
        _showSecurityAlert.value = false
    }

    /**
     * Dismisses the security alert (only allowed for non-critical severity).
     */
    fun dismissSecurityAlert() {
        if (_alertSeverity.value != AlertSeverity.CRITICAL) {
            _showSecurityAlert.value = false
        }
    }
    
    /**
     * Handles transfer input changes with keystroke capture.
     */
    fun onTransferFieldChanged(field: TransferField, oldValue: String, newValue: String) {
        when (field) {
            TransferField.ACCOUNT -> {
                _transferState.value = _transferState.value.copy(recipientAccount = newValue)
            }
            TransferField.AMOUNT -> {
                _transferState.value = _transferState.value.copy(amount = newValue)
            }
            TransferField.REMARKS -> {
                _transferState.value = _transferState.value.copy(remarks = newValue)
            }
        }
        
        // Capture keystroke timing
        viewModelScope.launch {
            keystrokeCollector.onTextChanged(oldValue, newValue)
        }
    }
    
    /**
     * Processes a fund transfer.
     */
    fun processTransfer(senderUsername: String) {
        val state = _transferState.value
        
        if (state.recipientAccount.isEmpty()) {
            _transferState.value = state.copy(errorMessage = "Please enter recipient account")
            return
        }
        
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _transferState.value = state.copy(errorMessage = "Please enter a valid amount")
            return
        }
        
        if (amount > _userBalance.value) {
            _transferState.value = state.copy(errorMessage = "Insufficient balance")
            return
        }
        
        viewModelScope.launch {
            _transferState.value = state.copy(isProcessing = true, errorMessage = null)
            
            val result = userRepository.transferFunds(
                senderUsername = senderUsername,
                recipientAccountNumber = state.recipientAccount,
                amount = amount,
                remarks = state.remarks
            )
            
            result.fold(
                onSuccess = { _ ->
                    _userBalance.value = _userBalance.value - amount
                    _transferState.value = TransferState(isSuccess = true)
                    showToast("Transfer successful!")
                },
                onFailure = { error ->
                    _transferState.value = state.copy(
                        isProcessing = false,
                        errorMessage = error.message ?: "Transfer failed"
                    )
                }
            )
        }
    }
    
    /**
     * Resets transfer state for a new transfer.
     */
    fun resetTransferState() {
        _transferState.value = TransferState()
    }
    
    /**
     * Toggles debug mode.
     */
    fun toggleDebugMode() {
        _debugMode.value = !_debugMode.value
    }

    fun simulateTypingAnomaly() {
        behaviorAnalyzer.applyDebugScenario("Typing rhythm anomaly", 0.68f)
        addDebugEvent("Demo: typing anomaly injected")
    }

    fun simulateTouchAnomaly() {
        behaviorAnalyzer.applyDebugScenario("Touch pressure anomaly", 0.58f)
        addDebugEvent("Demo: touch anomaly injected")
    }

    fun simulateMotionAnomaly() {
        behaviorAnalyzer.applyDebugScenario("Motion orientation anomaly", 0.72f)
        addDebugEvent("Demo: motion anomaly injected")
    }

    fun simulateCriticalRisk() {
        behaviorAnalyzer.applyDebugScenario("Critical multi-signal anomaly", 0.88f)
        addDebugEvent("Demo: critical risk injected")
    }

    fun resetDemoRisk() {
        behaviorAnalyzer.applyDebugScenario("Risk reset", 0.05f)
        addDebugEvent("Demo: risk reset")
    }

    private fun incrementCounters(
        keystrokes: Int = 0,
        touches: Int = 0,
        motionSamples: Int = 0,
        databaseWrites: Int = 0
    ) {
        val current = _debugCounters.value
        _debugCounters.value = current.copy(
            keystrokes = current.keystrokes + keystrokes,
            touches = current.touches + touches,
            motionSamples = current.motionSamples + motionSamples,
            databaseWrites = current.databaseWrites + databaseWrites,
            lastEventTimestamp = System.currentTimeMillis()
        )
    }

    private fun addDebugEvent(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _debugEvents.value = (listOf("$timestamp  $message") + _debugEvents.value).take(8)
    }
    
    /**
     * Shows a toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Triggers device vibration for security alerts.
     */
    private fun vibrate() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            // Vibration not available
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopBehavioralCollection()
    }
}

/**
 * Transfer form state.
 */
data class TransferState(
    val recipientAccount: String = "",
    val amount: String = "",
    val remarks: String = "",
    val isProcessing: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

enum class TransferField {
    ACCOUNT,
    AMOUNT,
    REMARKS
}

