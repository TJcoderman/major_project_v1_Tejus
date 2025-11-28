package com.securebank.app.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.securebank.app.data.model.DeviceState
import com.securebank.app.data.model.MotionData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

import java.util.Collections

/**
 * ============================================
 * SENSOR DATA COLLECTOR
 * ============================================
 * Manages accelerometer and gyroscope data collection.
 * Implements noise filtering using a moving average filter.
 */
@Singleton
class SensorDataCollector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    // Moving average filter parameters
    private val filterWindowSize = 10
    private val accelHistory = Collections.synchronizedList(mutableListOf<FloatArray>())
    private val gyroHistory = Collections.synchronizedList(mutableListOf<FloatArray>())
    
    // Current sensor values
    private var currentAccel = floatArrayOf(0f, 0f, 0f)
    private var currentGyro = floatArrayOf(0f, 0f, 0f)
    
    // State tracking
    private var isCollecting = false
    private var currentSessionId: String = ""
    
    /**
     * Checks if required sensors are available on the device.
     */
    fun areSensorsAvailable(): Boolean {
        return accelerometer != null && gyroscope != null
    }
    
    /**
     * Starts collecting sensor data for a session.
     * Returns a Flow of MotionData that can be collected by the caller.
     */
    fun startCollection(sessionId: String): Flow<MotionData> = callbackFlow {
        currentSessionId = sessionId
        isCollecting = true
        
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!isCollecting) return
                
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        currentAccel = event.values.clone()
                        addToHistory(accelHistory, currentAccel)
                    }
                    Sensor.TYPE_GYROSCOPE -> {
                        currentGyro = event.values.clone()
                        addToHistory(gyroHistory, currentGyro)
                        
                        // Emit motion data when we have both sensor readings
                        val motionData = createMotionData()
                        trySend(motionData)
                    }
                }
            }
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }
        
        // Register listeners with normal delay for battery efficiency
        accelerometer?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI  // ~60ms delay, good balance
            )
        }
        
        gyroscope?.let {
            sensorManager.registerListener(
                sensorListener,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        
        awaitClose {
            sensorManager.unregisterListener(sensorListener)
            isCollecting = false
            clearHistory()
        }
    }
    
    /**
     * Stops sensor data collection.
     */
    fun stopCollection() {
        isCollecting = false
    }
    
    /**
     * Creates a MotionData object from current sensor readings.
     */
    private fun createMotionData(): MotionData {
        val filteredAccel = applyMovingAverageFilter(accelHistory)
        val filteredGyro = applyMovingAverageFilter(gyroHistory)
        
        // Calculate device orientation from accelerometer
        val (pitch, roll, azimuth) = calculateOrientation(filteredAccel)
        
        // Infer device state
        val deviceState = inferDeviceState(filteredAccel, filteredGyro)
        
        return MotionData.create(
            sessionId = currentSessionId,
            timestamp = System.currentTimeMillis(),
            accelX = currentAccel[0],
            accelY = currentAccel[1],
            accelZ = currentAccel[2],
            gyroX = currentGyro[0],
            gyroY = currentGyro[1],
            gyroZ = currentGyro[2],
            pitch = pitch,
            roll = roll,
            azimuth = azimuth,
            filteredAccel = filteredAccel,
            deviceState = deviceState
        )
    }
    
    /**
     * Adds sensor reading to history for filtering.
     */
    private fun addToHistory(history: MutableList<FloatArray>, values: FloatArray) {
        history.add(values.clone())
        if (history.size > filterWindowSize) {
            history.removeAt(0)
        }
    }
    
    /**
     * Applies moving average filter to reduce noise.
     * This is crucial for accurate behavioral analysis.
     */
    private fun applyMovingAverageFilter(history: List<FloatArray>): FloatArray {
        synchronized(history) {
            if (history.isEmpty()) return floatArrayOf(0f, 0f, 0f)
            
            val result = floatArrayOf(0f, 0f, 0f)
            for (values in history) {
                result[0] += values[0]
                result[1] += values[1]
                result[2] += values[2]
            }
            
            val size = history.size.toFloat()
            return floatArrayOf(
                result[0] / size,
                result[1] / size,
                result[2] / size
            )
        }
    }
    
    /**
     * Calculates device orientation (pitch, roll, azimuth) from accelerometer data.
     * Returns angles in degrees.
     */
    private fun calculateOrientation(accel: FloatArray): Triple<Float, Float, Float> {
        val (ax, ay, az) = accel.toList()
        
        // Calculate pitch (rotation around X-axis)
        // When device is flat, pitch = 0
        val pitch = atan2(ay, sqrt(ax * ax + az * az)) * (180f / Math.PI.toFloat())
        
        // Calculate roll (rotation around Y-axis)
        val roll = atan2(-ax, az) * (180f / Math.PI.toFloat())
        
        // Azimuth requires magnetometer, simplified here
        val azimuth = 0f
        
        return Triple(pitch, roll, azimuth)
    }
    
    /**
     * Infers the device state from sensor readings.
     * This helps identify if the user's interaction pattern has changed.
     */
    private fun inferDeviceState(accel: FloatArray, gyro: FloatArray): DeviceState {
        val (ax, ay, az) = accel.toList()
        val (gx, gy, gz) = gyro.toList()
        
        // Calculate total acceleration magnitude
        val accelMagnitude = sqrt(ax * ax + ay * ay + az * az)
        
        // Calculate total rotation rate
        val gyroMagnitude = sqrt(gx * gx + gy * gy + gz * gz)
        
        // Device is flat on table if Z-axis acceleration is close to gravity
        // and X/Y are close to zero
        val isFlat = abs(az) > 9.0f && abs(ax) < 1.5f && abs(ay) < 1.5f
        
        // Device is stationary if gyro readings are very low
        val isStationary = gyroMagnitude < 0.1f
        
        // Detect walking pattern (periodic acceleration changes)
        val isWalking = accelMagnitude > 13.0f && !isStationary
        
        return when {
            isFlat && isStationary -> DeviceState.ON_TABLE
            isWalking -> DeviceState.WALKING
            isStationary -> DeviceState.STATIONARY
            gyroMagnitude < 0.5f -> DeviceState.HELD_IN_HAND
            else -> DeviceState.UNKNOWN
        }
    }
    
    /**
     * Clears sensor history buffers.
     */
    private fun clearHistory() {
        accelHistory.clear()
        gyroHistory.clear()
    }
    
    /**
     * Gets the current filtered accelerometer values.
     */
    fun getCurrentFilteredAccel(): FloatArray {
        return applyMovingAverageFilter(accelHistory)
    }
    
    /**
     * Gets the current device state based on latest readings.
     */
    fun getCurrentDeviceState(): DeviceState {
        val filteredAccel = applyMovingAverageFilter(accelHistory)
        val filteredGyro = applyMovingAverageFilter(gyroHistory)
        return inferDeviceState(filteredAccel, filteredGyro)
    }
}

