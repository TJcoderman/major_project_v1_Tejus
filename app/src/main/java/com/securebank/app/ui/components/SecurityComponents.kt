package com.securebank.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.securebank.app.data.model.DebugCollectionCounters
import com.securebank.app.data.model.DebugExplainabilityState
import com.securebank.app.data.model.KeystrokeData
import com.securebank.app.data.model.MotionData
import com.securebank.app.data.model.AlertSeverity
import com.securebank.app.data.model.RiskLevel
import com.securebank.app.data.model.SecurityRecommendation
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
 * Severity-aware:
 *   CRITICAL: Non-dismissible, logout only
 *   HIGH: "Verify Identity" action (re-auth stub)
 *   MEDIUM: "Continue Session" allowed
 */
@Composable
fun SecurityAlertDialog(
    message: String,
    severity: AlertSeverity = AlertSeverity.MEDIUM,
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
                    text = when (severity) {
                        AlertSeverity.CRITICAL -> "Critical Security Alert"
                        AlertSeverity.HIGH -> "Security Alert"
                        AlertSeverity.MEDIUM -> "Security Warning"
                    },
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
                
                // Action buttons — severity determines which buttons are shown
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Logout button always shown
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
                    
                    // CRITICAL: no dismiss button at all
                    // HIGH: "Verify Identity" button (re-auth action)
                    // MEDIUM: "Continue Session" button
                    if (severity != AlertSeverity.CRITICAL) {
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
                                text = when (severity) {
                                    AlertSeverity.HIGH -> "Verify Identity"
                                    else -> "Continue Session"
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
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
    isMLReady: Boolean = false,
    explainabilityState: DebugExplainabilityState = DebugExplainabilityState(),
    counters: DebugCollectionCounters = DebugCollectionCounters(),
    events: List<String> = emptyList(),
    motionData: MotionData? = null,
    touchData: TouchData? = null,
    keystrokeData: KeystrokeData? = null,
    onToggle: () -> Unit,
    onSimulateTypingAnomaly: () -> Unit = {},
    onSimulateTouchAnomaly: () -> Unit = {},
    onSimulateMotionAnomaly: () -> Unit = {},
    onSimulateCriticalRisk: () -> Unit = {},
    onResetDemoRisk: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val panelScroll = rememberScrollState()
    val displayRiskScore = explainabilityState.riskScore.takeIf { it > 0f } ?: riskScore
    val displayRiskLevel = explainabilityState.riskLevel.takeIf { displayRiskScore > 0f } ?: riskLevel
    val activeRiskColor = riskColor(displayRiskLevel)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = ObsidianSurface.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(panelScroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Behavior Intelligence Console",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CloudWhite
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
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DebugStatBox("Risk", "${(displayRiskScore * 100).toInt()}%", activeRiskColor, Modifier.weight(1f))
                    DebugStatBox("Trust", "${((1f - displayRiskScore) * 100).toInt()}%", Emerald, Modifier.weight(1f))
                    DebugStatBox("Level", displayRiskLevel.name, activeRiskColor, Modifier.weight(1.1f))
                }

                LinearProgressIndicator(
                    progress = displayRiskScore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = activeRiskColor,
                    trackColor = ObsidianBorder
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LOW", color = Emerald, fontSize = 10.sp)
                    Text("MEDIUM 40%", color = Gold, fontSize = 10.sp)
                    Text("HIGH 60%", color = Coral, fontSize = 10.sp)
                    Text("CRITICAL 80%", color = Color.Red, fontSize = 10.sp)
                }

                SectionTitle("Why this decision")
                DebugRow("State", explainabilityState.sessionState)
                DebugRow("Action", recommendationLabel(explainabilityState.recommendation))
                Text(
                    text = explainabilityState.reason,
                    color = CloudGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                SectionTitle("Weighted risk math")
                explainabilityState.contributions.forEach { contribution ->
                    ContributionRow(
                        label = contribution.signal,
                        deviation = contribution.deviation,
                        weight = contribution.weight,
                        contribution = contribution.contribution
                    )
                }
                DebugRow("Statistical risk", "${(explainabilityState.zScoreRisk * 100).toInt()}%")
                DebugRow("ML risk", explainabilityState.mlRisk?.let { "${(it * 100).toInt()}%" } ?: "Not used yet")

                SectionTitle("Baseline vs current")
                explainabilityState.comparisons.forEach {
                    MetricComparisonRow(it.label, it.baseline, it.current, it.deviation)
                }

                SectionTitle("ML engine")
                DebugRow("Model file", if (explainabilityState.modelLoaded) "Loaded" else "Not loaded")
                DebugRow("Enrollment", if (explainabilityState.enrollmentReady) "Ready" else "Waiting")
                DebugRow("Runtime", if (isMLReady) "Active" else "Inactive")
                DebugRow("Features", "${explainabilityState.extractedFeatureCount}/${explainabilityState.expectedFeatureCount}")
                DebugRow("Prediction", explainabilityState.mlPrediction)
                DebugRow("Confidence", explainabilityState.mlConfidence?.let { "${(it * 100).toInt()}%" } ?: "--")

                SectionTitle("Detected anomalies")
                if (explainabilityState.anomalies.isEmpty()) {
                    Text("No anomalies in the latest assessment.", color = MutedGray, fontSize = 12.sp)
                } else {
                    explainabilityState.anomalies.take(4).forEach {
                        DebugRow(it.type.name.replace('_', ' '), "${(it.severity * 100).toInt()}%")
                    }
                }

                SectionTitle("Collection pipeline")
                DebugRow("Keystrokes / Touches", "${counters.keystrokes} / ${counters.touches}")
                DebugRow("Motion samples", counters.motionSamples.toString())
                DebugRow("DB writes", counters.databaseWrites.toString())

                SectionTitle("Real-time sensors")
                if (motionData != null) {
                    DebugRow("Pitch / Roll", "%.1f deg / %.1f deg".format(motionData.pitch, motionData.roll))
                    DebugRow("Accel (X,Y,Z)", "%.1f, %.1f, %.1f".format(motionData.accelX, motionData.accelY, motionData.accelZ))
                } else {
                    DebugRow("Pitch / Roll", "Waiting")
                }

                if (touchData != null) {
                    DebugRow("Touch", "${touchData.touchType.name}, p=${"%.2f".format(touchData.pressure)}")
                    DebugRow("Touch velocity", "%.0f px/s".format(touchData.velocity))
                } else {
                    DebugRow("Touch", "Waiting")
                }

                if (keystrokeData != null) {
                    DebugRow("Last key", "dwell=${keystrokeData.dwellTime}ms flight=${keystrokeData.flightTime}ms")
                } else {
                    DebugRow("Last key", "Waiting")
                }

                SectionTitle("Decision timeline")
                if (events.isEmpty()) {
                    Text("No backend events yet.", color = MutedGray, fontSize = 12.sp)
                } else {
                    events.forEach { event ->
                        Text(event, color = CloudGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                SectionTitle("Reviewer demo controls")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebugActionButton("Typing", onSimulateTypingAnomaly, Modifier.weight(1f))
                    DebugActionButton("Touch", onSimulateTouchAnomaly, Modifier.weight(1f))
                    DebugActionButton("Motion", onSimulateMotionAnomaly, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DebugActionButton("Critical", onSimulateCriticalRisk, Modifier.weight(1f), Coral)
                    DebugActionButton("Reset", onResetDemoRisk, Modifier.weight(1f), Emerald)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = CloudWhite,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DebugStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = MutedGray, fontSize = 10.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ContributionRow(
    label: String,
    deviation: Float,
    weight: Float,
    contribution: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DebugRow(
            label,
            "${(deviation * 100).toInt()}% x ${"%.2f".format(weight)} = ${(contribution * 100).toInt()}%"
        )
        LinearProgressIndicator(
            progress = deviation.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = when (label) {
                "Keystroke" -> Gold
                "Touch" -> Emerald
                else -> Coral
            },
            trackColor = ObsidianBorder
        )
    }
}

@Composable
private fun MetricComparisonRow(label: String, baseline: String, current: String, deviation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(ObsidianLight.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = CloudWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("B: $baseline", color = MutedGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text("C: $current", color = CloudGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(deviation, color = if (deviation.startsWith("+")) Gold else Emerald, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun DebugActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Gold
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(color.copy(alpha = 0.8f), color.copy(alpha = 0.35f)))
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

private fun riskColor(riskLevel: RiskLevel): Color = when (riskLevel) {
    RiskLevel.LOW -> Emerald
    RiskLevel.MEDIUM -> Gold
    RiskLevel.HIGH -> Coral
    RiskLevel.CRITICAL -> Color.Red
}

private fun recommendationLabel(recommendation: SecurityRecommendation): String {
    return recommendation.name.replace('_', ' ')
}
