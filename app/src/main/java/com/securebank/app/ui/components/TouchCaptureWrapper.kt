package com.securebank.app.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.securebank.app.sensor.TouchDataCollector
import kotlinx.coroutines.launch

/**
 * ============================================
 * TOUCH CAPTURE WRAPPER
 * ============================================
 * Wraps UI content to intercept and collect touch events
 * for behavioral analysis without interfering with normal touch handling.
 */
@Composable
fun TouchCaptureWrapper(
    modifier: Modifier = Modifier,
    touchDataCollector: TouchDataCollector,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                // Capture the initial down event
                val down = awaitFirstDown(requireUnconsumed = false)
                val startPosition = down.position
                val startPressure = down.pressure
                
                // Record touch start
                touchDataCollector.onTouchStart(
                    position = startPosition,
                    pressure = startPressure,
                    size = 1f
                )
                
                var lastPosition = startPosition
                var lastPressure = startPressure
                
                // Track movement and final release
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    
                    when (event.type) {
                        PointerEventType.Move -> {
                            touchDataCollector.onTouchMove(change.position)
                            lastPosition = change.position
                            lastPressure = change.pressure
                        }
                        PointerEventType.Release -> {
                            coroutineScope.launch {
                                touchDataCollector.onTouchEnd(
                                    endPosition = lastPosition,
                                    pressure = lastPressure,
                                    size = 1f
                                )
                            }
                        }
                        else -> {}
                    }
                } while (event.changes.any { it.pressed })
            }
        },
        content = content
    )
}

/**
 * Simplified touch wrapper that only captures essential data.
 * Use this for less critical screens to reduce overhead.
 */
@Composable
fun LightTouchWrapper(
    modifier: Modifier = Modifier,
    onTouchStart: (Offset) -> Unit = {},
    onTouchEnd: (Offset, Long) -> Unit = { _, _ -> },
    content: @Composable BoxScope.() -> Unit
) {
    var startTime = 0L
    var startPosition = Offset.Zero
    
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                startTime = System.currentTimeMillis()
                startPosition = down.position
                onTouchStart(startPosition)
                
                var lastPosition = startPosition
                
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let { change ->
                        lastPosition = change.position
                    }
                } while (event.changes.any { it.pressed })
                
                val duration = System.currentTimeMillis() - startTime
                onTouchEnd(lastPosition, duration)
            }
        },
        content = content
    )
}

