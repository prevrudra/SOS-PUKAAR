package com.pukaar.highalert

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
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
    private const val KEY_SAVED = "saved_v1"

    const val PHONE_E164 = "+918037126014"
    const val DISPLAY_NAME = "PUKAAR High Alert"

    fun ensureSaved(context: Context): Boolean {
        val app = context.applicationContext
        if (ContextCompat.checkSelfPermission(app, android.Manifest.permission.WRITE_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return runCatching {
            if (findContactRawId(app) != null) {
                markSaved(app)
                return true
            }
            insertContact(app)
            markSaved(app)
            Log.i(TAG, "Saved $DISPLAY_NAME ($PHONE_E164) to phone contacts")
            true
        }.getOrElse { e ->
            Log.w(TAG, "Could not save caller ID contact: ${e.message}")
            false
        }
    }

    fun alreadySaved(context: Context): Boolean {
        val app = context.applicationContext
        if (!app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SAVED, false)) {
            return false
        }
        return findContactRawId(app) != null
    }

    private fun markSaved(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAVED, true)
            .apply()
    }

    private fun insertContact(context: Context) {
        val ops = ArrayList<ContentProviderOperation>()
        val rawIndex = ops.size
        ops.add(
            ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, null)
                .withValue(RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.DISPLAY_NAME, DISPLAY_NAME)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(Data.CONTENT_URI)
                .withValueBackReference(Data.RAW_CONTACT_ID, rawIndex)
                .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                .withValue(Phone.NUMBER, PHONE_E164)
                .withValue(Phone.TYPE, Phone.TYPE_MAIN)
                .build()
        )
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private fun findContactRawId(context: Context): Long? {
        val want = last10(PHONE_E164)
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

    private fun last10(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length <= 10) digits else digits.takeLast(10)
    }
}
