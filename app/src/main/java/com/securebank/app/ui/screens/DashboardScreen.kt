package com.securebank.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securebank.app.data.model.DebugCollectionCounters
import com.securebank.app.data.model.DebugExplainabilityState
import com.securebank.app.data.model.KeystrokeData
import com.securebank.app.data.model.MotionData
import com.securebank.app.data.model.RiskLevel
import com.securebank.app.data.model.TouchData
import com.securebank.app.data.model.Transaction
import com.securebank.app.data.model.TransactionType
import com.securebank.app.data.model.User
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.DebugBehaviorPanel
import com.securebank.app.ui.components.RiskIndicatorBadge
import com.securebank.app.ui.components.TouchCaptureWrapper
import com.securebank.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * ============================================
 * DASHBOARD SCREEN
 * ============================================
 * Main banking dashboard with balance, transactions,
 * and quick actions. Captures touch behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    balance: Double,
    transactions: List<Transaction>,
    riskScore: Float,
    riskLevel: RiskLevel,
    debugMode: Boolean,
    isMLReady: Boolean = false,
    debugExplainabilityState: DebugExplainabilityState = DebugExplainabilityState(),
    debugCounters: DebugCollectionCounters = DebugCollectionCounters(),
    debugEvents: List<String> = emptyList(),
    liveMotionData: MotionData? = null,
    liveTouchData: TouchData? = null,
    liveKeystrokeData: KeystrokeData? = null,
    touchDataCollector: TouchDataCollector,
    onTransferClick: () -> Unit,
    onResearchClick: () -> Unit = {},
    onLogout: () -> Unit,
    onToggleDebug: () -> Unit,
    onSimulateTypingAnomaly: () -> Unit = {},
    onSimulateTouchAnomaly: () -> Unit = {},
    onSimulateMotionAnomaly: () -> Unit = {},
    onSimulateCriticalRisk: () -> Unit = {},
    onResetDemoRisk: () -> Unit = {}
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    
    TouchCaptureWrapper(
        modifier = Modifier.fillMaxSize(),
        touchDataCollector = touchDataCollector
    ) {
        Scaffold(
            containerColor = Obsidian,
            topBar = {
                DashboardTopBar(
                    userName = user.fullName,
                    riskScore = riskScore,
                    riskLevel = riskLevel,
                    onLogout = onLogout
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Balance Card
                    item {
                        BalanceCard(
                            balance = balance,
                            currencyFormat = currencyFormat
                        )
                    }
                    
                    // Quick Actions
                    item {
                        QuickActionsSection(
                            onTransferClick = onTransferClick,
                            onResearchClick = onResearchClick
                        )
                    }
                    
                    // Recent Transactions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CloudWhite
                            )
                            
                            TextButton(onClick = { /* View all */ }) {
                                Text(
                                    text = "See All",
                                    color = Emerald
                                )
                            }
                        }
                    }
                    
                    // Transactions List
                    if (transactions.isEmpty()) {
                        item {
                            EmptyTransactionsCard()
                        }
                    } else {
                        items(transactions.take(6)) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                currencyFormat = currencyFormat
                            )
                        }
                    }
                    
                    // Debug toggle
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDebug() }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (debugMode) Icons.Default.BugReport else Icons.Outlined.BugReport,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (debugMode) "Hide Debug Panel" else "Show Debug Panel",
                                color = MutedGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Bottom padding for debug panel
                    item {
                        Spacer(modifier = Modifier.height(if (debugMode) 460.dp else 16.dp))
                    }
                }
                
                // Debug Panel
                DebugBehaviorPanel(
                    riskScore = riskScore,
                    riskLevel = riskLevel,
                    isVisible = debugMode,
                    isMLReady = isMLReady,
                    explainabilityState = debugExplainabilityState,
                    counters = debugCounters,
                    events = debugEvents,
                    motionData = liveMotionData,
                    touchData = liveTouchData,
                    keystrokeData = liveKeystrokeData,
                    onToggle = onToggleDebug,
                    onSimulateTypingAnomaly = onSimulateTypingAnomaly,
                    onSimulateTouchAnomaly = onSimulateTouchAnomaly,
                    onSimulateMotionAnomaly = onSimulateMotionAnomaly,
                    onSimulateCriticalRisk = onSimulateCriticalRisk,
                    onResetDemoRisk = onResetDemoRisk,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    userName: String,
    riskScore: Float,
    riskLevel: RiskLevel,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CloudWhite
                )
            }
        },
        actions = {
            RiskIndicatorBadge(
                riskLevel = riskLevel,
                riskScore = riskScore,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = MutedGray
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun BalanceCard(
    balance: Double,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            EmeraldDark,
                            Emerald,
                            EmeraldBright.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Available Balance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currencyFormat.format(balance),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "•••• •••• •••• 7890",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Primary Account",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onTransferClick: () -> Unit,
    onResearchClick: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CloudWhite
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Send,
                label = "Transfer",
                color = Emerald,
                onClick = onTransferClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.Science,
                label = "Research",
                color = Gold,
                onClick = onResearchClick,
                modifier = Modifier.weight(1f)
            )
            
            QuickActionButton(
                icon = Icons.Default.History,
                label = "History",
                color = CloudGray,
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CloudGray
            )
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    currencyFormat: NumberFormat
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    
    val (icon, iconColor, amountColor, amountPrefix) = when (transaction.type) {
        TransactionType.CREDIT, TransactionType.TRANSFER_IN -> {
            listOf(Icons.Default.ArrowDownward, Emerald, Emerald, "+")
        }
        TransactionType.DEBIT, TransactionType.TRANSFER_OUT, TransactionType.BILL_PAYMENT -> {
            listOf(Icons.Default.ArrowUpward, Coral, Coral, "-")
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background((iconColor as Color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon as ImageVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = CloudWhite
                )
                Text(
                    text = dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )
            }
            
            // Amount
            Text(
                text = "$amountPrefix${currencyFormat.format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor as Color
            )
        }
    }
}

@Composable
private fun EmptyTransactionsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ObsidianSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = MutedGray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No transactions yet",
                color = MutedGray
            )
        }
    }
}

