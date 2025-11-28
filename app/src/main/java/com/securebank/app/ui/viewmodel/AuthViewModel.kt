package com.securebank.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securebank.app.data.model.AuthState
import com.securebank.app.data.model.BehavioralSession
import com.securebank.app.data.model.KeystrokeData
import com.securebank.app.data.model.User
import com.securebank.app.data.repository.BehavioralRepository
import com.securebank.app.data.repository.UserRepository
import com.securebank.app.domain.BehaviorAnalyzer
import com.securebank.app.sensor.KeystrokeCollector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ============================================
 * AUTHENTICATION VIEW MODEL
 * ============================================
 * Handles login/logout and captures keystroke baseline during authentication.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val behavioralRepository: BehavioralRepository,
    private val keystrokeCollector: KeystrokeCollector,
    private val behaviorAnalyzer: BehaviorAnalyzer
) : ViewModel() {
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> = _usernameInput.asStateFlow()
    
    private val _passwordInput = MutableStateFlow("")
    val passwordInput: StateFlow<String> = _passwordInput.asStateFlow()
    
    // Keystroke collection for baseline
    private var sessionId: String = ""
    private var keystrokesCollected = 0
    
    init {
        // Generate initial session ID for baseline collection
        sessionId = UUID.randomUUID().toString()
        keystrokeCollector.startCollection(sessionId, isBaseline = true)

        // Observe keystroke events during login
        viewModelScope.launch {
            keystrokeCollector.keystrokeEvents.collect { keystrokeData ->
                // Always save keystrokes during login phase (baseline mode)
                behavioralRepository.saveKeystroke(keystrokeData)
                keystrokesCollected++
            }
        }
    }
    
    /**
     * Updates username input and captures keystroke timing.
     */
    fun onUsernameChanged(newValue: String) {
        val oldValue = _usernameInput.value
        _usernameInput.value = newValue
        
        // Capture keystroke timing
        viewModelScope.launch {
            keystrokeCollector.onTextChanged(oldValue, newValue)
        }
    }
    
    /**
     * Updates password input and captures keystroke timing.
     */
    fun onPasswordChanged(newValue: String) {
        val oldValue = _passwordInput.value
        _passwordInput.value = newValue
        
        // Capture keystroke timing
        viewModelScope.launch {
            keystrokeCollector.onTextChanged(oldValue, newValue)
        }
    }
    
    /**
     * Attempts login with provided credentials.
     * Captures keystroke baseline during the process.
     */
    fun login() {
        val username = _usernameInput.value.trim()
        val password = _passwordInput.value
        
        if (username.isEmpty() || password.isEmpty()) {
            _authState.value = _authState.value.copy(
                errorMessage = "Please enter username and password"
            )
            return
        }
        
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
            
            // Authenticate user
            val user = userRepository.authenticate(username, password)
            
            if (user != null) {
                // Stop baseline collection
                keystrokeCollector.stopCollection()
                
                // Create behavioral session
                val session = BehavioralSession(
                    sessionId = sessionId,
                    userId = user.username,
                    isBaseline = true,
                    avgKeystrokeDwellTime = keystrokeCollector.getBaselineAverageDwellTime(),
                    avgKeystrokeFlightTime = keystrokeCollector.getBaselineAverageFlightTime()
                )
                behavioralRepository.createSession(session)
                
                // Initialize behavior analyzer with baseline
                behaviorAnalyzer.initializeBaseline(sessionId)
                
                _authState.value = AuthState(
                    isAuthenticated = true,
                    currentUser = user,
                    sessionId = sessionId,
                    loginTimestamp = System.currentTimeMillis(),
                    isLoading = false
                )
                
                // Clear input fields
                _usernameInput.value = ""
                _passwordInput.value = ""
            } else {
                // Login failed - keep collecting baseline for retry
                _authState.value = AuthState(
                    isAuthenticated = false,
                    isLoading = false,
                    errorMessage = "Invalid username or password"
                )
            }
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            val currentSessionId = _authState.value.sessionId
            
            // Update session end time
            if (currentSessionId != null) {
                val session = behavioralRepository.getSession(currentSessionId)
                session?.let {
                    behavioralRepository.updateSession(
                        it.copy(endTime = System.currentTimeMillis())
                    )
                }
            }
            
            // Reset behavior analyzer
            behaviorAnalyzer.reset()
            
            // Clear keystroke baseline
            keystrokeCollector.clearBaseline()
            
            // Reset auth state
            _authState.value = AuthState()
            
            // Start collection for next user
            sessionId = UUID.randomUUID().toString()
            keystrokeCollector.startCollection(sessionId, isBaseline = true)
        }
    }
    
    /**
     * Clears any error message.
     */
    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
    
    /**
     * Gets the current session ID.
     */
    fun getCurrentSessionId(): String = sessionId
}

