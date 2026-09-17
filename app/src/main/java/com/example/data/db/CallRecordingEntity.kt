package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.models.CallRecording

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val contactName: String?,
    val phoneNumber: String,
    val timestamp: Long,
    val durationSeconds: Long,
    val isIncoming: Boolean,
    val fileSize: Long = 0L
) {
    fun toCallRecording() = CallRecording(
        id = id,
        filePath = filePath,
        fileName = fileName,
        contactName = contactName,
        phoneNumber = phoneNumber,
        timestamp = timestamp,
        durationSeconds = durationSeconds,
        isIncoming = isIncoming,
        fileSize = fileSize
    )
}
