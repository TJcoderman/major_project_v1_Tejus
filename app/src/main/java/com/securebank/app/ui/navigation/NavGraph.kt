package com.securebank.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.securebank.app.data.model.ExperimentSessionType
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.SecurityAlertDialog
import com.securebank.app.ui.screens.DashboardScreen
import com.securebank.app.ui.screens.ExperimentHubScreen
import com.securebank.app.ui.screens.ExperimentSessionScreen
import com.securebank.app.ui.screens.LoginScreen
import com.securebank.app.ui.screens.TransferScreen
import com.securebank.app.ui.viewmodel.AuthViewModel
import com.securebank.app.ui.viewmodel.BankingViewModel
import com.securebank.app.ui.viewmodel.ExperimentViewModel

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
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
    val showSecurityAlert by bankingViewModel.showSecurityAlert.collectAsState()
    val securityAlertMessage by bankingViewModel.securityAlertMessage.collectAsState()
    
    // Real-time monitoring
    val liveMotionData by bankingViewModel.liveMotionData.collectAsState()
    val liveTouchData by bankingViewModel.liveTouchData.collectAsState()
    val liveKeystrokeData by bankingViewModel.liveKeystrokeData.collectAsState()
    
    // Security Alert Dialog
    if (showSecurityAlert) {
        SecurityAlertDialog(
            message = securityAlertMessage,
            onDismiss = {
                bankingViewModel.dismissSecurityAlert()
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
                    }
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

