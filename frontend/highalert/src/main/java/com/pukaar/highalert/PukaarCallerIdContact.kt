package com.pukaar.highalert

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Saves the official PUKAAR High Alert caller ID so voice/SMS show a name, not a raw number.
 */
object PukaarCallerIdContact {
    private const val TAG = "PukaarCallerId"
    private const val PREFS = "pukaar_caller_id"
    private const val KEY_SAVED = "saved_v2"

    const val PHONE_E164 = "+918037126014"
    const val DISPLAY_NAME = "PUKAAR High Alert"

    fun hasPermissions(context: Context): Boolean {
        val app = context.applicationContext
        return hasRead(app) && hasWrite(app)
    }

    fun isSaved(context: Context): Boolean {
        val app = context.applicationContext
        if (!hasRead(app)) return false
        return findContactRawId(app) != null
    }

    fun ensureSaved(context: Context): Boolean {
        val app = context.applicationContext
        if (!hasPermissions(app)) return false
        return runCatching {
            val existing = findContactRawId(app)
            if (existing != null) {
                updateDisplayName(app, existing)
                markSaved(app)
                return true
            }
            insertContact(app)
            val verified = findContactRawId(app) != null
            if (verified) {
                markSaved(app)
                Log.i(TAG, "Saved $DISPLAY_NAME ($PHONE_E164) to phone contacts")
            } else {
                clearSavedFlag(app)
                Log.w(TAG, "Insert completed but contact not found on verify")
            }
            verified
        }.getOrElse { e ->
            clearSavedFlag(app)
            Log.w(TAG, "Could not save caller ID contact: ${e.message}", e)
            false
        }
    }

    private fun hasRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasWrite(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun markSaved(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAVED, true)
            .apply()
    }

    private fun clearSavedFlag(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SAVED)
            .apply()
    }

    private fun insertContact(context: Context) {
        runCatching { insertContactBatch(context) }
            .onFailure { batchErr ->
                Log.w(TAG, "Batch insert failed (${batchErr.message}) — trying sequential")
                insertContactSequential(context)
            }
    }

    private fun insertContactBatch(context: Context) {
        val ops = ArrayList<ContentProviderOperation>()
        val rawIndex = ops.size
        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null as String?)
                .withValue(RawContacts.ACCOUNT_NAME, null as String?)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, DISPLAY_NAME)
                .build()
        )
        addPhoneOps(ops, rawIndex, PHONE_E164)
        addPhoneOps(ops, rawIndex, "8037126014")
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun addPhoneOps(ops: ArrayList<ContentProviderOperation>, rawIndex: Int, number: String) {
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, number)
                .withValue(Phone.TYPE, Phone.TYPE_MOBILE)
                .build()
        )
    }

    private fun insertContactSequential(context: Context) {
        val resolver = context.contentResolver
        val rawValues = ContentValues().apply {
            put(RawContacts.ACCOUNT_TYPE, null as String?)
            put(RawContacts.ACCOUNT_NAME, null as String?)
        }
        val rawUri = resolver.insert(RawContacts.CONTENT_URI, rawValues)
            ?: throw IllegalStateException("RawContacts insert returned null")
        val rawId = ContentUris.parseId(rawUri)

        resolver.insert(
            Data.CONTENT_URI,
            ContentValues().apply {
                put(Data.RAW_CONTACT_ID, rawId)
                put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                put(StructuredName.DISPLAY_NAME, DISPLAY_NAME)
            }
        ) ?: throw IllegalStateException("Name insert returned null")

        for (number in listOf(PHONE_E164, "8037126014", "+91 80371 26014")) {
            resolver.insert(
                Data.CONTENT_URI,
                ContentValues().apply {
                    put(Data.RAW_CONTACT_ID, rawId)
                    put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                    put(Phone.NUMBER, number)
                    put(Phone.TYPE, Phone.TYPE_MOBILE)
                }
            )
        }
    }

    private fun updateDisplayName(context: Context, rawContactId: Long) {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data._ID, StructuredName.DISPLAY_NAME),
            "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE}=?",
            arrayOf(rawContactId.toString(), StructuredName.CONTENT_ITEM_TYPE),
            null
        )
        cursor?.use {
            val idCol = it.getColumnIndex(Data._ID)
            val nameCol = it.getColumnIndex(StructuredName.DISPLAY_NAME)
            if (idCol < 0 || nameCol < 0) return
            if (it.moveToFirst()) {
                val current = it.getString(nameCol)
                if (current != DISPLAY_NAME) {
                    val values = ContentValues().apply {
                        put(StructuredName.DISPLAY_NAME, DISPLAY_NAME)
                    }
                    resolver.update(
                        Data.CONTENT_URI,
                        values,
                        "${Data._ID}=?",
                        arrayOf(it.getLong(idCol).toString())
                    )
                }
            }
        }
    }

    private fun findContactRawId(context: Context): Long? {
        if (!hasRead(context)) return null
        val want = last10(PHONE_E164)
        findByFilterUri(context, PHONE_E164)?.let { return it }
        findByFilterUri(context, "8037126014")?.let { return it }
        val cursor = context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Data.RAW_CONTACT_ID, Phone.NUMBER),
            null,
            null,
            null
        ) ?: return null
        cursor.use {
            val idCol = it.getColumnIndex(Data.RAW_CONTACT_ID)
            val phoneCol = it.getColumnIndex(Phone.NUMBER)
            if (idCol < 0 || phoneCol < 0) return null
            while (it.moveToNext()) {
                val num = it.getString(phoneCol) ?: continue
                if (last10(num) == want) return it.getLong(idCol)
            }
        }
        return null
    }

    private fun findByFilterUri(context: Context, phone: String): Long? {
        val uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(phone))
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(Data.RAW_CONTACT_ID),
            null,
            null,
            null
        ) ?: return null
        cursor.use {
            val idCol = it.getColumnIndex(Data.RAW_CONTACT_ID)
            if (idCol < 0) return null
            if (it.moveToFirst()) return it.getLong(idCol)
        }
        return null
    }

    private fun last10(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length <= 10) digits else digits.takeLast(10)
    }
}
