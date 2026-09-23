package com.pukaar.highalert

import android.content.ContentProviderOperation
import android.content.Context
import android.content.SharedPreferences
import android.provider.ContactsContract
import android.util.Log

/**
 * Saves the PUKAAR voice/caller number into the device address book so the dialer
 * shows "PUKAAR Alert" instead of a raw number.
 */
object PukaarCallerIdHelper {
    private const val TAG = "PukaarCallerId"
    private const val PREFS = "pukaar_caller_id"
    private const val KEY_SAVED = "saved_v1"
    const val DISPLAY_NAME = "PUKAAR Alert"
    /** AuthKey / IVR outbound number used for High Alert voice escalation. */
    const val VOICE_NUMBER_E164 = "+918037126014"

    fun ensureSaved(context: Context) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_SAVED, false)) return
        val ok = runCatching { insertContact(context) }.getOrDefault(false)
        if (ok) {
            prefs.edit().putBoolean(KEY_SAVED, true).apply()
            Log.i(TAG, "Saved $DISPLAY_NAME ($VOICE_NUMBER_E164)")
        } else {
            Log.w(TAG, "Could not save caller ID contact (permission or OEM)")
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun insertContact(context: Context): Boolean {
        // Skip if already present
        val existing = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone._ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?",
            arrayOf("%8037126014%"),
            null
        )
        existing?.use {
            if (it.moveToFirst()) return true
        }

        val ops = ArrayList<ContentProviderOperation>()
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, DISPLAY_NAME)
                .build()
        )
        ops.add(
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, VOICE_NUMBER_E164)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                )
                .build()
        )
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        return true
    }
}
