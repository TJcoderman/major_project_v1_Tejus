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
    
    private val _showSecurityAlert = MutableStateFlow(false)
    val showSecurityAlert: StateFlow<Boolean> = _showSecurityAlert.asStateFlow()
    
    private val _securityAlertMessage = MutableStateFlow("")
    val securityAlertMessage: StateFlow<String> = _securityAlertMessage.asStateFlow()
    
    // Behavioral data collection state
    private var isCollecting = false
    private var sensorCollectionJob: Job? = null
    private var touchCollectionJob: Job? = null
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
    
    /**
     * Initializes banking data and starts behavioral collection.
     */
    fun initialize(user: User, sessionId: String) {
        currentSessionId = sessionId
        
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
        
        // Start touch collection
        touchDataCollector.startCollection(sessionId)
        
        // Start keystroke collection (non-baseline mode)
        keystrokeCollector.startCollection(sessionId, isBaseline = false)
        
        // Collect touch events
        touchCollectionJob = viewModelScope.launch {
            touchDataCollector.touchEvents.collect { touchData ->
                _liveTouchData.value = touchData
                behavioralRepository.saveTouch(touchData)
            }
        }
        
        // Collect keystroke events
        viewModelScope.launch {
            keystrokeCollector.keystrokeEvents.collect { keystrokeData ->
                _liveKeystrokeData.value = keystrokeData
                behavioralRepository.saveKeystroke(keystrokeData)
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
                                _securityAlertMessage.value = "Real-time anomaly detected. Please verify identity."
                                _showSecurityAlert.value = true
                                vibrate()
                            }
                            SecurityRecommendation.FORCE_LOGOUT -> {
                                _securityAlertMessage.value = "Critical motion anomaly detected. Session terminated."
                                _showSecurityAlert.value = true
                                vibrate()
                            }
                            SecurityRecommendation.SHOW_WARNING -> {
                                if (_debugMode.value) {
                                    showToast("⚠️ Motion anomaly detected!")
                                }
                            }
                            else -> {}
                        }
                    }
                    
                    motionBuffer.add(motionData)
                    
                    // Batch save every 10 readings to reduce database writes
                    if (motionBuffer.size >= 10) {
                        behavioralRepository.saveMotionBatch(motionBuffer.toList())
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
        
        touchDataCollector.stopCollection()
        keystrokeCollector.stopCollection()
        sensorDataCollector.stopCollection()
        
        sensorCollectionJob?.cancel()
        touchCollectionJob?.cancel()
        riskAssessmentJob?.cancel()
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
            
            // Periodic assessment every 15 seconds
            while (isCollecting) {
                val assessment = behaviorAnalyzer.performRiskAssessment(currentSessionId)
                handleRiskAssessment(assessment)
                delay(15000)
            }
        }
    }
    
    /**
     * Handles the result of a risk assessment.
     */
    private fun handleRiskAssessment(assessment: RiskAssessment) {
        when (assessment.recommendation) {
            SecurityRecommendation.CONTINUE -> {
                // All good, no action needed
            }
            SecurityRecommendation.SHOW_WARNING -> {
                if (_debugMode.value) {
                    showToast("⚠️ Medium risk detected: ${(assessment.overallRiskScore * 100).toInt()}%")
                }
            }
            SecurityRecommendation.REQUEST_REAUTHENTICATION -> {
                _securityAlertMessage.value = "Unusual behavior detected. For your security, please verify your identity."
                _showSecurityAlert.value = true
                vibrate()
            }
            SecurityRecommendation.FORCE_LOGOUT -> {
                _securityAlertMessage.value = "Session security compromised. You will be logged out for your protection."
                _showSecurityAlert.value = true
                vibrate()
            }
        }
    }
    
    /**
     * Dismisses the security alert.
     */
    fun dismissSecurityAlert() {
        _showSecurityAlert.value = false
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

