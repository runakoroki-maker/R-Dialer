package com.example.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.models.SmsConversation
import com.example.data.models.SmsMessageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getConversations(): List<SmsConversation> = withContext(Dispatchers.IO) {
        val conversationsMap = LinkedHashMap<Long, SmsConversation>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.TYPE
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val readIdx = it.getColumnIndex(Telephony.Sms.READ)

                while (it.moveToNext()) {
                    val threadId = if (threadIdIdx >= 0) it.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx >= 0) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                    val read = if (readIdx >= 0) it.getInt(readIdx) == 1 else true

                    if (!conversationsMap.containsKey(threadId)) {
                        val contactName = resolveContactName(context, address)
                        conversationsMap[threadId] = SmsConversation(
                            threadId = threadId,
                            address = address,
                            snippet = body,
                            date = date,
                            read = read,
                            contactName = contactName
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission denied
        } catch (e: Exception) {
            // Error querying SMS
        }

        conversationsMap.values.toList()
    }

    suspend fun getMessagesForThread(threadId: Long): List<SmsMessageItem> = withContext(Dispatchers.IO) {
        val messages = mutableListOf<SmsMessageItem>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} ASC"
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val threadIdIdx = it.getColumnIndex(Telephony.Sms.THREAD_ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val tId = if (threadIdIdx >= 0) it.getLong(threadIdIdx) else 0L
                    val address = if (addressIdx >= 0) it.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx >= 0) it.getLong(dateIdx) else 0L
                    val type = if (typeIdx >= 0) it.getInt(typeIdx) else 1

                    messages.add(
                        SmsMessageItem(
                            id = id,
                            threadId = tId,
                            address = address,
                            body = body,
                            date = date,
                            type = type
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Error
        }

        messages
    }

    suspend fun sendSms(address: String, body: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(address, null, body, null, null)

            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, System.currentTimeMillis())
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            }
            context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        return cursor.getString(nameIdx)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }
}
