package com.example.data

import android.content.Context
import android.os.Environment
import com.example.data.db.AppDatabase
import com.example.data.db.CallRecordingEntity
import com.example.data.models.CallRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).callRecordingDao()

    val recordingsFlow: Flow<List<CallRecording>> = dao.getAllRecordings().map { entities ->
        entities.map { it.toCallRecording() }
    }

    /**
     * Dedicated folder for recordings: R Dialer/Call Recordings/
     */
    fun getRecordingsDirectory(): File {
        val baseDir = context.getExternalFilesDir(null)
            ?: File(context.filesDir, "media")
        val recordingsDir = File(baseDir, "R Dialer/Call Recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return recordingsDir
    }

    /**
     * Generates a standardized filename:
     * e.g. 2026-09-17_21-30_Runa-Koroki.m4a
     */
    fun generateRecordingFileName(callerName: String?, phoneNumber: String, timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
        val datePart = dateFormat.format(Date(timestamp))

        val rawName = if (!callerName.isNullOrBlank()) {
            callerName
        } else if (phoneNumber.isNotBlank()) {
            phoneNumber
        } else {
            "Unknown"
        }

        val sanitizedName = rawName.replace(Regex("[^a-zA-Z0-9_-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')

        return "${datePart}_${sanitizedName}.m4a"
    }

    suspend fun saveRecording(
        file: File,
        contactName: String?,
        phoneNumber: String,
        timestamp: Long,
        durationSeconds: Long,
        isIncoming: Boolean
    ): CallRecording = withContext(Dispatchers.IO) {
        val entity = CallRecordingEntity(
            filePath = file.absolutePath,
            fileName = file.name,
            contactName = contactName,
            phoneNumber = phoneNumber,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            isIncoming = isIncoming,
            fileSize = file.length()
        )
        val id = dao.insertRecording(entity)
        entity.copy(id = id).toCallRecording()
    }

    suspend fun deleteRecording(recording: CallRecording) = withContext(Dispatchers.IO) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        dao.deleteById(recording.id)
    }
}
