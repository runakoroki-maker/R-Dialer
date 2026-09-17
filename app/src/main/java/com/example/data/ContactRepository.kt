package com.example.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.example.data.models.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val context: Context) {

    suspend fun getContacts(query: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ContactItem>()
        val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )

        val selection = if (query.isNotBlank()) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        } else {
            null
        }

        val selectionArgs = if (query.isNotBlank()) {
            val q = "%$query%"
            arrayOf(q, q)
        } else {
            null
        }

        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                val lookupIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val lookupKey = if (lookupIdx >= 0) it.getString(lookupIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""
                    val photoUri = if (photoIdx >= 0) it.getString(photoIdx) else null

                    // Normalize to avoid exact duplicate entries for same person and number
                    val normalized = number.replace("[^0-9+]".toRegex(), "")
                    val dedupeKey = "$name-$normalized"
                    if (!seenNumbers.contains(dedupeKey) && number.isNotBlank()) {
                        seenNumbers.add(dedupeKey)
                        contactsList.add(
                            ContactItem(
                                id = id,
                                lookupKey = lookupKey,
                                displayName = name,
                                phoneNumber = number,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted or revoked
        } catch (e: Exception) {
            // Content provider read failure
        }

        contactsList
    }

    suspend fun findContactByNumber(number: String): ContactItem? = withContext(Dispatchers.IO) {
        if (number.isBlank()) return@withContext null
        val normalizedSearch = number.replace("[^0-9+]".toRegex(), "")
        if (normalizedSearch.isEmpty()) return@withContext null

        try {
            // Try PhoneLookup URI first (Android's optimized fast lookup for incoming caller ID)
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
            )

            context.contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val lookupIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (!name.isNullOrBlank()) {
                        return@withContext ContactItem(
                            id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L,
                            lookupKey = if (lookupIdx >= 0) cursor.getString(lookupIdx) ?: "" else "",
                            displayName = name,
                            phoneNumber = if (numIdx >= 0) cursor.getString(numIdx) ?: number else number,
                            photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            // Contacts permission not granted
        } catch (e: Exception) {
            // Ignore and fallback
        }

        // Fallback: search in in-memory contacts if phone lookup fails
        val all = getContacts()
        return@withContext all.firstOrNull { contact ->
            val contactNormalized = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
            PhoneNumberUtils.compare(context, contact.phoneNumber, number) ||
                    (contactNormalized.length >= 6 && normalizedSearch.endsWith(contactNormalized.takeLast(6))) ||
                    (normalizedSearch.length >= 6 && contactNormalized.endsWith(normalizedSearch.takeLast(6)))
        }
    }
}
