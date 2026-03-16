package com.securebank.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.rememberNavController
import com.securebank.app.sensor.SensorDataCollector
import com.securebank.app.sensor.TouchDataCollector
import com.securebank.app.ui.navigation.SecureBankNavGraph
import com.securebank.app.ui.theme.Obsidian
import com.securebank.app.ui.theme.SecureBankTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ============================================
 * MAIN ACTIVITY
 * ============================================
 * Entry point for the SecureBank application.
 * Handles lifecycle events for proper sensor management.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var sensorDataCollector: SensorDataCollector
    
    @Inject
    lateinit var touchDataCollector: TouchDataCollector
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            SecureBankTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Obsidian
                ) {
                    val navController = rememberNavController()
                    
                    // Handle lifecycle for sensor management
                    LifecycleHandler(
                        onPause = {
                            // Stop sensor collection when app goes to background
                            sensorDataCollector.stopCollection()
                            touchDataCollector.stopCollection()
                        },
                        onResume = {
                            // Sensors will be restarted by ViewModel if session is active
                        }
                    )
                    
                    SecureBankNavGraph(
                        navController = navController,
                        touchDataCollector = touchDataCollector
                    )
                }
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle screen rotation without losing sensor data continuity
        // The sensor collectors maintain their state across configuration changes
        // because they are singleton instances injected by Hilt
    }
}

/**
 * Composable to handle lifecycle events.
 */
@Composable
fun LifecycleHandler(
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

