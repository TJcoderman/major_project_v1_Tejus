package com.securebank.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.securebank.app.data.model.AlertSeverity
import com.securebank.app.data.model.ExperimentSessionType
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.SecurityAlertDialog
import com.securebank.app.ui.screens.DashboardScreen
import com.securebank.app.ui.screens.ExperimentHubScreen
import com.securebank.app.ui.screens.ExperimentSessionScreen
import com.securebank.app.ui.screens.LoginScreen
import com.securebank.app.ui.screens.SignupScreen
import com.securebank.app.ui.screens.TransferScreen
import com.securebank.app.ui.viewmodel.AuthViewModel
import com.securebank.app.ui.viewmodel.BankingViewModel
import com.securebank.app.ui.viewmodel.ExperimentViewModel

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Dashboard : Screen("dashboard")
    object Transfer : Screen("transfer")
    object ExperimentHub : Screen("experiment_hub")
    object ExperimentSession : Screen("experiment_session/{sessionType}/{profileOwnerId}") {
        fun createRoute(sessionType: ExperimentSessionType, profileOwnerId: String?) =
            "experiment_session/${sessionType.name}/${profileOwnerId ?: "self"}"
    }
}

/**
 * ============================================
 * NAVIGATION GRAPH
 * ============================================
 * Defines navigation structure and handles screen transitions.
 */
@Composable
fun SecureBankNavGraph(
    navController: NavHostController,
    touchDataCollector: TouchDataCollector,
    authViewModel: AuthViewModel = hiltViewModel(),
    bankingViewModel: BankingViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    
    // Banking state
    val transactions by bankingViewModel.transactions.collectAsState()
    val balance by bankingViewModel.userBalance.collectAsState()
    val transferState by bankingViewModel.transferState.collectAsState()
    val riskScore by bankingViewModel.currentRiskScore.collectAsState()
    val riskLevel by bankingViewModel.currentRiskLevel.collectAsState()
    val debugMode by bankingViewModel.debugMode.collectAsState()
    val isMLReady by bankingViewModel.isMLReady.collectAsState()
    val debugExplainabilityState by bankingViewModel.debugExplainabilityState.collectAsState()
    val debugCounters by bankingViewModel.debugCounters.collectAsState()
    val debugEvents by bankingViewModel.debugEvents.collectAsState()
    val showSecurityAlert by bankingViewModel.showSecurityAlert.collectAsState()
    val securityAlertMessage by bankingViewModel.securityAlertMessage.collectAsState()
    val alertSeverity by bankingViewModel.alertSeverity.collectAsState()
    val forceLogoutEvent by bankingViewModel.forceLogoutEvent.collectAsState()
    
    // Real-time monitoring
    val liveMotionData by bankingViewModel.liveMotionData.collectAsState()
    val liveTouchData by bankingViewModel.liveTouchData.collectAsState()
    val liveKeystrokeData by bankingViewModel.liveKeystrokeData.collectAsState()
    
    // Security Alert Dialog — severity determines available actions
    if (showSecurityAlert) {
        val currentUser = authState.currentUser
        val verificationValue = currentUser?.pin?.takeIf { it.isNotBlank() }
            ?: currentUser?.passwordHash
        val verificationLabel = if (!currentUser?.pin.isNullOrBlank()) "PIN" else "password"

        SecurityAlertDialog(
            message = securityAlertMessage,
            severity = alertSeverity,
            expectedVerificationValue = verificationValue,
            verificationLabel = verificationLabel,
            onDismiss = {
                if (alertSeverity == AlertSeverity.HIGH) {
                    bankingViewModel.acknowledgeVerifiedIdentity()
                } else {
                    bankingViewModel.dismissSecurityAlert()
                }
            },
            onLogout = {
                bankingViewModel.dismissSecurityAlert()
                bankingViewModel.stopBehavioralCollection()
                authViewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    // Handle force-logout events from the ViewModel (item 1 enforcement)
    LaunchedEffect(forceLogoutEvent) {
        if (forceLogoutEvent) {
            bankingViewModel.acknowledgeForceLogout()
            authViewModel.logout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Lifecycle observer: pause/resume behavioral collection (item 2)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    bankingViewModel.pauseBehavioralCollection()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    bankingViewModel.resumeBehavioralCollection()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) Screen.Dashboard.route else Screen.Login.route
    ) {
        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    val user = authState.currentUser
                    val sessionId = authState.sessionId
                    
                    if (user != null && sessionId != null) {
                        bankingViewModel.initialize(user, sessionId)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onCreateAccount = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }
        
        // Signup Screen
        composable(Screen.Signup.route) {
            SignupScreen(
                touchDataCollector = touchDataCollector,
                onSignupComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Dashboard Screen
        composable(Screen.Dashboard.route) {
            val user = authState.currentUser
            
            if (user != null) {
                DashboardScreen(
                    user = user,
                    balance = balance,
                    transactions = transactions,
                    riskScore = riskScore,
                    riskLevel = riskLevel,
                    debugMode = debugMode,
                    isMLReady = isMLReady,
                    debugExplainabilityState = debugExplainabilityState,
                    debugCounters = debugCounters,
                    debugEvents = debugEvents,
                    liveMotionData = liveMotionData,
                    liveTouchData = liveTouchData,
                    liveKeystrokeData = liveKeystrokeData,
                    touchDataCollector = touchDataCollector,
                    onTransferClick = {
                        navController.navigate(Screen.Transfer.route)
                    },
                    onResearchClick = {
                        navController.navigate(Screen.ExperimentHub.route)
                    },
                    onLogout = {
                        bankingViewModel.stopBehavioralCollection()
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onToggleDebug = {
                        bankingViewModel.toggleDebugMode()
                    },
                    onSimulateTypingAnomaly = bankingViewModel::simulateTypingAnomaly,
                    onSimulateTouchAnomaly = bankingViewModel::simulateTouchAnomaly,
                    onSimulateMotionAnomaly = bankingViewModel::simulateMotionAnomaly,
                    onSimulateCriticalRisk = bankingViewModel::simulateCriticalRisk,
                    onResetDemoRisk = bankingViewModel::resetDemoRisk
                )
            } else {
                // User not logged in, redirect to login
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        
        // Transfer Screen
        composable(Screen.Transfer.route) {
            val user = authState.currentUser
            
            if (user != null) {
                TransferScreen(
                    balance = balance,
                    transferState = transferState,
                    riskScore = riskScore,
                    riskLevel = riskLevel,
                    touchDataCollector = touchDataCollector,
                    onFieldChange = { field, oldValue, newValue ->
                        bankingViewModel.onTransferFieldChanged(field, oldValue, newValue)
                    },
                    onTransfer = {
                        bankingViewModel.processTransfer(user.username)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    onReset = {
                        bankingViewModel.resetTransferState()
                    }
                )
            }
        }
        
        // Experiment Flow (nested graph to share ViewModel between Hub and Session)
        navigation(
            startDestination = Screen.ExperimentHub.route,
            route = "experiment_flow"
        ) {
            composable(Screen.ExperimentHub.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("experiment_flow")
                }
                val experimentViewModel: ExperimentViewModel = hiltViewModel(parentEntry)

                ExperimentHubScreen(
                    viewModel = experimentViewModel,
                    onStartSession = { sessionType, profileOwnerId ->
                        experimentViewModel.startSession(sessionType, profileOwnerId)
                        navController.navigate(
                            Screen.ExperimentSession.createRoute(sessionType, profileOwnerId)
                        )
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Experiment Session Screen (active data collection)
            composable(Screen.ExperimentSession.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("experiment_flow")
                }
                val experimentViewModel: ExperimentViewModel = hiltViewModel(parentEntry)

                ExperimentSessionScreen(
                    viewModel = experimentViewModel,
                    onComplete = {
                        experimentViewModel.resetSession()
                        navController.popBackStack(Screen.ExperimentHub.route, false)
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
