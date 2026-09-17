package com.example.data

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.example.data.models.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ContactRepository(private val context: Context) {

    fun hasWritePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getContacts(query: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<ContactItem>()
        val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
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

        val contactIds = mutableSetOf<Long>()

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
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
                        if (id > 0) contactIds.add(id)
                        contactsList.add(
                            ContactItem(
                                id = id,
                                lookupKey = lookupKey,
                                displayName = name,
                                phoneNumber = number,
                                photoUri = photoUri,
                                businessName = null,
                                address = null
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

        // Fetch organizations and addresses for retrieved contacts
        if (contactsList.isNotEmpty()) {
            val orgMap = fetchOrganizations()
            val addrMap = fetchAddresses()

            return@withContext contactsList.map { item ->
                val org = orgMap[item.id]
                val addr = addrMap[item.id]
                if (org != null || addr != null) {
                    item.copy(businessName = org, address = addr)
                } else {
                    item
                }
            }
        }

        contactsList
    }

    private fun fetchOrganizations(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        try {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val compIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val comp = if (compIdx >= 0) cursor.getString(compIdx) else null
                    if (id > 0 && !comp.isNullOrBlank()) {
                        result[id] = comp
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore optional data fetch failure
        }
        return result
    }

    private fun fetchAddresses(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        try {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)

            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val addr = if (addrIdx >= 0) cursor.getString(addrIdx) else null
                    if (id > 0 && !addr.isNullOrBlank()) {
                        result[id] = addr
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore optional data fetch failure
        }
        return result
    }

    suspend fun saveContact(
        displayName: String,
        phoneNumber: String,
        businessName: String? = null,
        address: String? = null,
        photoBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. Raw Contact insertion
            val rawContactInsertIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Structured Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName.trim())
                    .build()
            )

            // 3. Phone Number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.trim())
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build()
            )

            // 4. Business / Organization Name (if provided)
            if (!businessName.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, businessName.trim())
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                        .build()
                )
            }

            // 5. Postal Address (if provided)
            if (!address.isNullOrBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address.trim())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
                        .build()
                )
            }

            // 6. Profile Photo (if provided)
            if (photoBytes != null && photoBytes.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                        .build()
                )
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(Exception("Contacts permission denied by system."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save contact: ${e.localizedMessage ?: "Unknown error"}"))
        }
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
                        val contactId = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                        val business = fetchOrganizationForContact(contactId)
                        val address = fetchAddressForContact(contactId)

                        return@withContext ContactItem(
                            id = contactId,
                            lookupKey = if (lookupIdx >= 0) cursor.getString(lookupIdx) ?: "" else "",
                            displayName = name,
                            phoneNumber = if (numIdx >= 0) cursor.getString(numIdx) ?: number else number,
                            photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null,
                            businessName = business,
                            address = address
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

    private fun fetchOrganizationForContact(contactId: Long): String? {
        if (contactId <= 0) return null
        try {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Organization.COMPANY)
            val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            )
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun fetchAddressForContact(contactId: Long): String? {
        if (contactId <= 0) return null
        try {
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
            val selection = "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE
            )
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        } catch (e: Exception) {}
        return null
    }
}
