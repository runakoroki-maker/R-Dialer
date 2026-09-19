package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.CallLog
import com.example.data.models.CallLogEntry
import com.example.data.models.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    suspend fun getCallLogs(limit: Int = 100): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val callLogs = mutableListOf<CallLogEntry>()
        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val cachedName = if (nameIdx >= 0) it.getString(nameIdx) else null
                    val rawType = if (typeIdx >= 0) it.getInt(typeIdx) else -1
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                    val duration = if (durationIdx >= 0) it.getLong(durationIdx) else 0L

                    val callType = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                        else -> CallType.UNKNOWN
                    }

                    callLogs.add(
                        CallLogEntry(
                            id = id,
                            number = number,
                            cachedName = cachedName,
                            type = callType,
                            timestamp = date,
                            durationSeconds = duration
                        )
                    )
                    count++
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Log read error
        }

        callLogs
    }

    suspend fun getCallLogsForNumber(number: String): List<CallLogEntry> = withContext(Dispatchers.IO) {
        val allLogs = getCallLogs(500)
        allLogs.filter { it.number == number || (it.number.isNotBlank() && number.isNotBlank() && it.number.takeLast(7) == number.takeLast(7)) }
    }

    suspend fun deleteCallLog(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.withAppendedPath(CallLog.Calls.CONTENT_URI, id.toString())
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isNumberBlocked(number: String): Boolean = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                BlockedNumberContract.isBlocked(context, number)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun setNumberBlocked(number: String, block: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (block) {
                    val values = ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                    }
                    context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values) != null
                } else {
                    context.contentResolver.delete(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                        arrayOf(number)
                    ) > 0
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

