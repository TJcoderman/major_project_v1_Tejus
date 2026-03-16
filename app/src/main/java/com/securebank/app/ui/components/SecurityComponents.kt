package com.securebank.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.securebank.app.data.model.KeystrokeData
import com.securebank.app.data.model.MotionData
import com.securebank.app.data.model.RiskLevel
import com.securebank.app.data.model.TouchData
import com.securebank.app.ui.theme.*

/**
 * ============================================
 * RISK INDICATOR BADGE
 * ============================================
 * Shows current risk level with animated indicator.
 */
@Composable
fun RiskIndicatorBadge(
    riskLevel: RiskLevel,
    riskScore: Float,
    modifier: Modifier = Modifier,
    showScore: Boolean = true
) {
    val (color, icon, label) = when (riskLevel) {
        RiskLevel.LOW -> Triple(Emerald, Icons.Default.Shield, "Secure")
        RiskLevel.MEDIUM -> Triple(Gold, Icons.Default.Warning, "Caution")
        RiskLevel.HIGH -> Triple(Coral, Icons.Default.GppBad, "Risk")
        RiskLevel.CRITICAL -> Triple(Color.Red, Icons.Default.GppBad, "Critical")
    }
    
    // Pulsing animation for elevated risk levels
    val infiniteTransition = rememberInfiniteTransition(label = "risk_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                color.copy(
                    alpha = if (riskLevel == RiskLevel.LOW) 0.15f else alpha * 0.2f
                )
            )
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = if (showScore) {
                "$label ${(riskScore * 100).toInt()}%"
            } else {
                label
            },
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * ============================================
 * SECURITY ALERT DIALOG
 * ============================================
 * Full-screen alert for session hijacking detection.
 */
@Composable
fun SecurityAlertDialog(
    message: String,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent dismiss by clicking outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CoralMuted.copy(alpha = 0.95f),
                            Obsidian.copy(alpha = 0.98f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Warning icon with animation
                val infiniteTransition = rememberInfiniteTransition(label = "alert")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Box(
                    modifier = Modifier
                        .size((80 * scale).dp)
                        .clip(CircleShape)
                        .background(Coral.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GppBad,
                        contentDescription = "Security Alert",
                        tint = Coral,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Text(
                    text = "Security Alert",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CloudGray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Coral
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logout Now",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CloudGray
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(DimGray, DimGray))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Continue Session",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * ============================================
 * DEBUG PANEL
 * ============================================
 * Shows behavioral metrics in debug mode.
 */
@Composable
fun DebugBehaviorPanel(
    riskScore: Float,
    riskLevel: RiskLevel,
    isVisible: Boolean,
    motionData: MotionData? = null,
    touchData: TouchData? = null,
    keystrokeData: KeystrokeData? = null,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ObsidianSurface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔍 Behavior Debug",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CloudGray
                    )
                    
                    IconButton(onClick = onToggle) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MutedGray
                        )
                    }
                }
                
                Divider(color = ObsidianBorder)
                
                // Risk Info
                DebugRow("Risk Score", "${(riskScore * 100).toInt()}%")
                DebugRow("Risk Level", riskLevel.name)
                
                LinearProgressIndicator(
                    progress = riskScore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (riskLevel) {
                        RiskLevel.LOW -> Emerald
                        RiskLevel.MEDIUM -> Gold
                        RiskLevel.HIGH, RiskLevel.CRITICAL -> Coral
                    },
                    trackColor = ObsidianBorder
                )

                Divider(color = ObsidianBorder)
                Text("Real-time Sensors", style = MaterialTheme.typography.labelSmall, color = MutedGray)

                // Motion Data
                if (motionData != null) {
                    DebugRow("Pitch / Roll", "%.1f° / %.1f°".format(motionData.pitch, motionData.roll))
                    DebugRow("Accel (X,Y,Z)", "%.1f, %.1f, %.1f".format(motionData.accelX, motionData.accelY, motionData.accelZ))
                } else {
                    DebugRow("Motion", "Waiting for data...")
                }

                // Touch Data
                if (touchData != null) {
                    DebugRow("Touch Pressure", "%.2f".format(touchData.pressure))
                    DebugRow("Swipe Velocity", "%.0f px/s".format(touchData.velocity))
                } else {
                    DebugRow("Touch", "Waiting for interaction...")
                }

                // Keystroke Data
                if (keystrokeData != null) {
                    DebugRow("Last Key Dwell", "${keystrokeData.dwellTime}ms")
                    DebugRow("Last Flight Time", "${keystrokeData.flightTime}ms")
                } else {
                    DebugRow("Keystroke", "Waiting for typing...")
                }
            }
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MutedGray, fontSize = 12.sp)
        Text(value, color = CloudWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

