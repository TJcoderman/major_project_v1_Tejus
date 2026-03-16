package com.securebank.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securebank.app.data.model.PinKeystrokeEvent
import com.securebank.app.ui.theme.*

/**
 * ============================================
 * CUSTOM PIN PAD WITH REAL DWELL TIME CAPTURE
 * ============================================
 * This composable captures REAL key down/up events via pointerInput,
 * solving the Android software keyboard limitation where dwell times
 * are unavailable in Jetpack Compose.
 *
 * Each key press records:
 * - Precise dwell time (finger down → finger up)
 * - Flight time (previous key up → current key down)
 * - Touch coordinates and contact area
 *
 * This is the KEY INNOVATION for the research paper.
 */
@Composable
fun CustomPinPad(
    pinLength: Int = 6,
    currentPin: String,
    targetPin: String?,                    // null = free entry, non-null = must match
    sessionId: String,
    attemptNumber: Int,
    onDigitPressed: (PinKeystrokeEvent) -> Unit,
    onPinComplete: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastKeyUpTime by remember { mutableLongStateOf(0L) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PIN Display Dots
        PinDotsDisplay(
            currentLength = currentPin.length,
            maxLength = pinLength
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Number pad grid: 1-9, then 0 row
        val digits = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    PinKey(
                        label = digit.toString(),
                        onKeyEvent = { keyDown, keyUp, touchX, touchY, touchSize ->
                            if (currentPin.length < pinLength) {
                                val flightTime = if (lastKeyUpTime > 0L) {
                                    keyDown - lastKeyUpTime
                                } else 0L

                                val event = PinKeystrokeEvent(
                                    sessionId = sessionId,
                                    timestamp = keyDown,
                                    digit = digit,
                                    keyDownTime = keyDown,
                                    keyUpTime = keyUp,
                                    dwellTime = keyUp - keyDown,
                                    flightTime = flightTime.coerceAtLeast(0),
                                    touchX = touchX,
                                    touchY = touchY,
                                    touchSize = touchSize,
                                    pinAttemptNumber = attemptNumber
                                )
                                lastKeyUpTime = keyUp
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigitPressed(event)

                                val newPin = currentPin + digit.toString()
                                if (newPin.length == pinLength) {
                                    onPinComplete(newPin)
                                }
                            }
                        }
                    )
                }
            }
        }

        // Bottom row: empty, 0, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Empty spacer
            Spacer(modifier = Modifier.size(80.dp))

            // Zero key
            PinKey(
                label = "0",
                onKeyEvent = { keyDown, keyUp, touchX, touchY, touchSize ->
                    if (currentPin.length < pinLength) {
                        val flightTime = if (lastKeyUpTime > 0L) {
                            keyDown - lastKeyUpTime
                        } else 0L

                        val event = PinKeystrokeEvent(
                            sessionId = sessionId,
                            timestamp = keyDown,
                            digit = 0,
                            keyDownTime = keyDown,
                            keyUpTime = keyUp,
                            dwellTime = keyUp - keyDown,
                            flightTime = flightTime.coerceAtLeast(0),
                            touchX = touchX,
                            touchY = touchY,
                            touchSize = touchSize,
                            pinAttemptNumber = attemptNumber
                        )
                        lastKeyUpTime = keyUp
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigitPressed(event)

                        val newPin = currentPin + "0"
                        if (newPin.length == pinLength) {
                            onPinComplete(newPin)
                        }
                    }
                }
            )

            // Backspace key
            BackspaceKey(
                enabled = currentPin.isNotEmpty(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                }
            )
        }
    }
}

/**
 * Individual PIN key that captures real touch down/up timing.
 * Uses pointerInput to get precise timestamps.
 */
@Composable
private fun PinKey(
    label: String,
    onKeyEvent: (keyDown: Long, keyUp: Long, touchX: Float, touchY: Float, touchSize: Float) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnKeyEvent by rememberUpdatedState(onKeyEvent)

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) Emerald.copy(alpha = 0.3f) else ObsidianSurface,
        animationSpec = tween(100),
        label = "keyBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isPressed) EmeraldBright else CloudWhite,
        animationSpec = tween(100),
        label = "keyText"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val keyDownTime = System.currentTimeMillis()
                        isPressed = true

                        // Wait for release — this is where we get REAL dwell time
                        val released = tryAwaitRelease()
                        val keyUpTime = System.currentTimeMillis()
                        isPressed = false

                        if (released) {
                            currentOnKeyEvent(
                                keyDownTime,
                                keyUpTime,
                                offset.x,
                                offset.y,
                                // Approximate touch size from offset distance to center
                                kotlin.math.sqrt(
                                    (offset.x - size.width / 2f).let { it * it } +
                                    (offset.y - size.height / 2f).let { it * it }
                                )
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Backspace key for deleting the last digit.
 */
@Composable
private fun BackspaceKey(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(if (enabled) ObsidianSurface.copy(alpha = 0.5f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { if (currentEnabled) currentOnClick() })
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Backspace,
            contentDescription = "Delete",
            tint = if (enabled) MutedGray else DimGray.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * PIN dots display showing entered digit count.
 */
@Composable
private fun PinDotsDisplay(
    currentLength: Int,
    maxLength: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < currentLength
            val dotColor by animateColorAsState(
                targetValue = if (isFilled) Emerald else ObsidianBorder,
                animationSpec = tween(200),
                label = "dot$index"
            )

            Box(
                modifier = Modifier
                    .size(if (isFilled) 16.dp else 14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * PIN entry result indicator showing correctness.
 */
@Composable
fun PinResultIndicator(
    isCorrect: Boolean,
    attemptNumber: Int,
    totalAttempts: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect)
                EmeraldMuted.copy(alpha = 0.4f)
            else
                Coral.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isCorrect) "✓ Correct" else "✗ Incorrect",
                color = if (isCorrect) EmeraldBright else Coral,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = "($attemptNumber/$totalAttempts)",
                color = MutedGray,
                fontSize = 12.sp
            )
        }
    }
}
