package com.securebank.app.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.SecurityAlertDialog
import com.securebank.app.ui.screens.DashboardScreen
import com.securebank.app.ui.screens.LoginScreen
import com.securebank.app.ui.screens.TransferScreen
import com.securebank.app.ui.viewmodel.AuthViewModel
import com.securebank.app.ui.viewmodel.BankingViewModel

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Transfer : Screen("transfer")
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
                    touchDataCollector = touchDataCollector,
                    onTransferClick = {
                        navController.navigate(Screen.Transfer.route)
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
    }
}

