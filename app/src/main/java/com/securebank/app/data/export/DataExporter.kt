package com.securebank.app.data.export

import android.content.Context
import android.os.Environment
import com.securebank.app.data.model.*
import com.securebank.app.data.repository.BehavioralRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ============================================
 * DATA EXPORTER - CSV EXPORT FOR ML TRAINING
 * ============================================
 * Exports all behavioral data (keystroke, touch, motion) as CSV files
 * for offline ML model training in Python.
 *
 * Each session generates 3 CSV files:
 * - {sessionId}_keystrokes.csv
 * - {sessionId}_touches.csv
 * - {sessionId}_motion.csv
 *
 * Plus a metadata file:
 * - {sessionId}_metadata.csv
 *
 * Files are saved to:
 * /sdcard/Documents/SecureBank_Research/{participantId}/
 */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val behavioralRepository: BehavioralRepository
) {
    companion object {
        private const val ROOT_DIR = "SecureBank_Research"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }

    /**
     * Gets the export directory for a participant.
     */
    private fun getExportDir(participantId: String): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val exportDir = File(documentsDir, "$ROOT_DIR/$participantId")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return exportDir
    }

    /**
     * Exports all data for a session to CSV files.
     *
     * @return The directory path where files were saved
     */
    suspend fun exportSession(
        sessionId: String,
        participantId: String,
        profileOwnerId: String,
        sessionType: ExperimentSessionType,
        pinKeystrokes: List<PinKeystrokeEvent> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val exportDir = getExportDir(participantId)
        val timestamp = dateFormat.format(Date())
        val prefix = "${sessionType.name.lowercase()}_${timestamp}"

        // Export keystroke data (from Room DB)
        exportKeystrokes(exportDir, prefix, sessionId)

        // Export PIN keystroke data (real dwell times)
        if (pinKeystrokes.isNotEmpty()) {
            exportPinKeystrokes(exportDir, prefix, pinKeystrokes)
        }

        // Export touch data
        exportTouches(exportDir, prefix, sessionId)

        // Export motion data
        exportMotion(exportDir, prefix, sessionId)

        // Export session metadata
        exportMetadata(
            exportDir, prefix, sessionId,
            participantId, profileOwnerId, sessionType
        )

        exportDir.absolutePath
    }

    /**
     * Exports keystroke data from software keyboard (flight time only).
     */
    private suspend fun exportKeystrokes(dir: File, prefix: String, sessionId: String) {
        val file = File(dir, "${prefix}_keystrokes.csv")
        val keystrokes = behavioralRepository.getSessionKeystrokes(sessionId).first()

        FileWriter(file).use { writer ->
            // CSV Header
            writer.appendLine("timestamp,key_code,dwell_time_ms,flight_time_ms,is_baseline")

            keystrokes.forEach { k ->
                writer.appendLine(
                    "${k.timestamp},${k.keyCode},${k.dwellTime},${k.flightTime},${k.isLoginBaseline}"
                )
            }
        }
    }

    /**
     * Exports PIN keystrokes with REAL dwell times from custom pin pad.
     * This is the high-quality keystroke data for the research paper.
     */
    private fun exportPinKeystrokes(
        dir: File,
        prefix: String,
        pinKeystrokes: List<PinKeystrokeEvent>
    ) {
        val file = File(dir, "${prefix}_pin_keystrokes.csv")

        FileWriter(file).use { writer ->
            // CSV Header
            writer.appendLine(
                "timestamp,digit,key_down_time,key_up_time," +
                "dwell_time_ms,flight_time_ms," +
                "touch_x,touch_y,touch_size,attempt_number"
            )

            pinKeystrokes.forEach { k ->
                writer.appendLine(
                    "${k.timestamp},${k.digit},${k.keyDownTime},${k.keyUpTime}," +
                    "${k.dwellTime},${k.flightTime}," +
                    "${k.touchX},${k.touchY},${k.touchSize},${k.pinAttemptNumber}"
                )
            }
        }
    }

    /**
     * Exports touch interaction data.
     */
    private suspend fun exportTouches(dir: File, prefix: String, sessionId: String) {
        val file = File(dir, "${prefix}_touches.csv")
        val touches = behavioralRepository.getSessionTouches(sessionId).first()

        FileWriter(file).use { writer ->
            // CSV Header
            writer.appendLine(
                "timestamp,touch_type,start_x,start_y,end_x,end_y," +
                "pressure,touch_size,duration_ms,velocity,acceleration," +
                "hold_duration_ms,touch_area"
            )

            touches.forEach { t ->
                writer.appendLine(
                    "${t.timestamp},${t.touchType.name}," +
                    "${t.startX},${t.startY},${t.endX},${t.endY}," +
                    "${t.pressure},${t.touchSize},${t.duration},${t.velocity},${t.acceleration}," +
                    "${t.holdDuration},${t.touchArea}"
                )
            }
        }
    }

    /**
     * Exports motion/sensor data.
     */
    private suspend fun exportMotion(dir: File, prefix: String, sessionId: String) {
        val file = File(dir, "${prefix}_motion.csv")
        val motionData = behavioralRepository.getSessionMotion(sessionId).first()

        FileWriter(file).use { writer ->
            // CSV Header
            writer.appendLine(
                "timestamp,accel_x,accel_y,accel_z," +
                "gyro_x,gyro_y,gyro_z," +
                "pitch,roll,azimuth," +
                "filtered_accel_x,filtered_accel_y,filtered_accel_z," +
                "device_state"
            )

            motionData.forEach { m ->
                writer.appendLine(
                    "${m.timestamp},${m.accelX},${m.accelY},${m.accelZ}," +
                    "${m.gyroX},${m.gyroY},${m.gyroZ}," +
                    "${m.pitch},${m.roll},${m.azimuth}," +
                    "${m.filteredAccelX},${m.filteredAccelY},${m.filteredAccelZ}," +
                    "${m.deviceState}"
                )
            }
        }
    }

    /**
     * Exports session metadata for experiment tracking.
     */
    private fun exportMetadata(
        dir: File,
        prefix: String,
        sessionId: String,
        participantId: String,
        profileOwnerId: String,
        sessionType: ExperimentSessionType
    ) {
        val file = File(dir, "${prefix}_metadata.csv")

        FileWriter(file).use { writer ->
            writer.appendLine("key,value")
            writer.appendLine("session_id,$sessionId")
            writer.appendLine("participant_id,$participantId")
            writer.appendLine("profile_owner_id,$profileOwnerId")
            writer.appendLine("session_type,${sessionType.name}")
            writer.appendLine("export_timestamp,${System.currentTimeMillis()}")
            writer.appendLine("device_model,${android.os.Build.MODEL}")
            writer.appendLine("device_manufacturer,${android.os.Build.MANUFACTURER}")
            writer.appendLine("android_version,${android.os.Build.VERSION.SDK_INT}")
        }
    }

    /**
     * Exports a summary CSV listing all sessions for all participants.
     * Call this after the entire experiment is complete.
     */
    suspend fun exportExperimentSummary(
        participants: List<Participant>,
        sessions: List<ExperimentSession>
    ): String = withContext(Dispatchers.IO) {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val summaryDir = File(documentsDir, ROOT_DIR)
        if (!summaryDir.exists()) summaryDir.mkdirs()

        val file = File(summaryDir, "experiment_summary_${dateFormat.format(Date())}.csv")

        FileWriter(file).use { writer ->
            writer.appendLine(
                "session_id,participant_id,profile_owner_id," +
                "session_type,start_time,end_time,is_complete,is_genuine"
            )

            sessions.forEach { s ->
                val isGenuine = s.participantId == s.profileOwnerId
                writer.appendLine(
                    "${s.sessionId},${s.participantId},${s.profileOwnerId}," +
                    "${s.sessionType.name},${s.startTime},${s.endTime ?: ""}," +
                    "${s.isComplete},$isGenuine"
                )
            }
        }

        file.absolutePath
    }

    /**
     * Gets the total export size for a participant.
     */
    fun getExportSize(participantId: String): Long {
        val dir = getExportDir(participantId)
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Lists all exported sessions for a participant.
     */
    fun listExportedSessions(participantId: String): List<String> {
        val dir = getExportDir(participantId)
        return dir.listFiles()
            ?.filter { it.name.endsWith("_metadata.csv") }
            ?.map { it.name.removeSuffix("_metadata.csv") }
            ?: emptyList()
    }
}
