package com.securebank.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securebank.app.data.model.RiskLevel
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.components.RiskIndicatorBadge
import com.securebank.app.ui.components.TouchCaptureWrapper
import com.securebank.app.ui.theme.*
import com.securebank.app.ui.viewmodel.TransferField
import com.securebank.app.ui.viewmodel.TransferState
import java.text.NumberFormat
import java.util.*

/**
 * ============================================
 * TRANSFER SCREEN
 * ============================================
 * Fund transfer screen with behavioral monitoring.
 * Captures keystroke and touch patterns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    balance: Double,
    transferState: TransferState,
    riskScore: Float,
    riskLevel: RiskLevel,
    touchDataCollector: TouchDataCollector,
    onFieldChange: (TransferField, String, String) -> Unit,
    onTransfer: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val finishTransfer: () -> Unit = {
        onReset()
        onBack()
    }
    
    TouchCaptureWrapper(
        modifier = Modifier.fillMaxSize(),
        touchDataCollector = touchDataCollector
    ) {
        Scaffold(
            containerColor = Obsidian,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Transfer Funds",
                            fontWeight = FontWeight.Bold,
                            color = CloudWhite
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (transferState.isSuccess) finishTransfer() else onBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = CloudWhite
                            )
                        }
                    },
                    actions = {
                        RiskIndicatorBadge(
                            riskLevel = riskLevel,
                            riskScore = riskScore,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Success State
                    AnimatedVisibility(
                        visible = transferState.isSuccess,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        SuccessCard(
                            transferState = transferState,
                            currencyFormat = currencyFormat,
                            onDone = finishTransfer
                        )
                    }
                    
                    // Transfer Form (hidden on success)
                    AnimatedVisibility(
                        visible = !transferState.isSuccess,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Balance Info
                            BalanceInfoCard(
                                balance = balance,
                                currencyFormat = currencyFormat
                            )
                            
                            // Transfer Form Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = ObsidianSurface
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Recipient Account
                                    OutlinedTextField(
                                        value = transferState.recipientAccount,
                                        onValueChange = { 
                                            onFieldChange(
                                                TransferField.ACCOUNT,
                                                transferState.recipientAccount,
                                                it
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Recipient Account Number") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.AccountCircle,
                                                contentDescription = null,
                                                tint = MutedGray
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        colors = transferFieldColors(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    // Amount
                                    OutlinedTextField(
                                        value = transferState.amount,
                                        onValueChange = { 
                                            onFieldChange(
                                                TransferField.AMOUNT,
                                                transferState.amount,
                                                it.filter { c -> c.isDigit() || c == '.' }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Amount") },
                                        leadingIcon = {
                                            Text(
                                                text = "₹",
                                                color = MutedGray,
                                                fontSize = 20.sp,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        colors = transferFieldColors(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    // Remarks
                                    OutlinedTextField(
                                        value = transferState.remarks,
                                        onValueChange = { 
                                            onFieldChange(
                                                TransferField.REMARKS,
                                                transferState.remarks,
                                                it
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Remarks (Optional)") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Note,
                                                contentDescription = null,
                                                tint = MutedGray
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { focusManager.clearFocus() }
                                        ),
                                        colors = transferFieldColors(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    // Error message
                                    AnimatedVisibility(
                                        visible = transferState.errorMessage != null,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Coral.copy(alpha = 0.15f))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Error,
                                                contentDescription = null,
                                                tint = Coral,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = transferState.errorMessage ?: "",
                                                color = Coral,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Security Note
                            SecurityNote()
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Transfer Button
                            Button(
                                onClick = onTransfer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = !transferState.isProcessing && 
                                         transferState.recipientAccount.isNotBlank() && 
                                         transferState.amount.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald,
                                    disabledContainerColor = Emerald.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (transferState.isProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Send Money",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceInfoCard(
    balance: Double,
    currencyFormat: NumberFormat
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ObsidianLight
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Available Balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray
                )
                Text(
                    text = currencyFormat.format(balance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CloudWhite
                )
            }
            
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Emerald.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
private fun SecurityNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EmeraldMuted.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = Emerald,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = "Your behavior is being monitored for security verification",
            color = CloudGray,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SuccessCard(
    transferState: TransferState,
    currencyFormat: NumberFormat,
    onDone: () -> Unit
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
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldDark,
                            Emerald
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Text(
                    text = "Transfer Successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "Your money has been sent successfully",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReceiptRow("Amount", currencyFormat.format(transferState.successAmount))
                    ReceiptRow("Recipient", transferState.successRecipientAccount)
                    ReceiptRow(
                        "Remarks",
                        transferState.successRemarks.ifBlank { "Fund Transfer" }
                    )
                    ReceiptRow("Reference", transferState.successReferenceId)
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done", color = EmeraldDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun transferFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Emerald,
    unfocusedBorderColor = ObsidianBorder,
    focusedLabelColor = Emerald,
    unfocusedLabelColor = MutedGray,
    cursorColor = Emerald,
    focusedTextColor = CloudWhite,
    unfocusedTextColor = CloudGray
)

