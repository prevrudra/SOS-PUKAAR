package com.pukaar.app.integration

import android.content.Context
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ContactDto
import com.pukaar.app.data.api.ContactRequest
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.util.PhoneNumbers
import com.pukaar.app.util.SmsHelper
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

object ContactRepositoryBridge {

    fun roleFor(type: ContactType): String = type.apiRole

    fun typeFromRole(role: String?): ContactType =
        ContactType.entries.firstOrNull { it.apiRole == role } ?: ContactType.SOS

    fun normalizePhone(mobile: String, dialCode: String = "+91"): String {
        val trimmed = mobile.trim()
        // Full E.164 pasted into the mobile field — don't prepend dial again.
        if (trimmed.startsWith("+")) {
            return PhoneNumbers.toE164(trimmed)
        }
        val digits = trimmed.filter { it.isDigit() }
        // "91XXXXXXXXXX" / "9198…" without plus while dial is +91
        if (digits.length > 10 && dialCode.filter { it.isDigit() }.let { digits.startsWith(it) }) {
            return PhoneNumbers.toE164("+$digits")
        }
        return PhoneNumbers.fromParts(dialCode, digits.ifBlank { trimmed })
    }

    fun phonesMatch(a: String?, b: String?): Boolean {
        if (a.isNullOrBlank() || b.isNullOrBlank()) return false
        val na = runCatching { PhoneNumbers.toE164(a) }.getOrElse { a.filter { it.isDigit() } }
        val nb = runCatching { PhoneNumbers.toE164(b) }.getOrElse { b.filter { it.isDigit() } }
        if (na == nb) return true
        val da = na.filter { it.isDigit() }
        val db = nb.filter { it.isDigit() }
        if (da.length < 8 || db.length < 8) return false
        val len = minOf(10, da.length, db.length)
        return da.takeLast(len) == db.takeLast(len)
    }

    fun toRequest(draft: ContactDraft): ContactRequest = ContactRequest(
        name = draft.name.trim(),
        phone = normalizePhone(draft.mobile, draft.dialCode),
        role = roleFor(draft.type),
        relationship = draft.relationship.ifBlank { null },
        notes = draft.notes.ifBlank { null },
        priorityOrder = draft.priorityOrder
    )

    suspend fun loadContacts(): List<ContactUiModel> {
        return PukaarApp.instance.repository.contacts().map { c ->
            val relationship = c.relationship.orEmpty()
            ContactUiModel(
                id = c.id ?: "",
                name = c.name ?: "",
                phoneNumber = c.phone ?: "",
                type = typeFromRole(c.role),
                relation = relationFromLabel(relationship),
                relationship = relationship,
                notes = c.notes.orEmpty(),
                priorityOrder = c.priorityOrder ?: 1,
                verified = c.verified == true
            )
        }
    }

    private fun relationFromLabel(raw: String): com.pukaar.app.ui.screen.contacts.ContactRelation? {
        if (raw.isBlank()) return null
        val key = raw.trim().uppercase().replace(' ', '_')
        return com.pukaar.app.ui.screen.contacts.ContactRelation.entries.firstOrNull {
            it.name.equals(key, ignoreCase = true) ||
                it.name.equals(raw.trim(), ignoreCase = true)
        }
    }

    /**
     * Creates/updates a contact. Server issues the OTP on create/update.
     * Does **not** auto-verify — the user must enter the code via [verifyContact].
     */
    suspend fun saveContact(
        context: Context,
        draft: ContactDraft,
        senderName: String?,
        shareHighAlert: Boolean = true
    ): Result<String> {
        val req = toRequest(draft)
        return try {
            val contact = if (!draft.id.isNullOrBlank()) {
                PukaarApp.instance.repository.updateContact(draft.id, req)
            } else {
                PukaarApp.instance.repository.addContact(req)
            }
            val id = contact.id ?: return Result.failure(Exception("Contact not saved"))
            // Optional High Alert install invite — not used as the OTP source.
            if (shareHighAlert) {
                sendHighAlertInvite(context, draft.name, contact.phone ?: req.phone, senderName)
            }
            Result.success(id)
        } catch (e: HttpException) {
            if (e.code() == 409) {
                resolveConflict(context, draft, req, senderName, shareHighAlert)?.let {
                    return Result.success(it)
                }
            }
            Result.failure(Exception(e.userMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * On 409: find the conflicting row by phone (+ role) and PUT instead of failing.
     */
    private suspend fun resolveConflict(
        context: Context,
        draft: ContactDraft,
        req: ContactRequest,
        senderName: String?,
        shareHighAlert: Boolean
    ): String? {
        val wantedRole = roleFor(draft.type)
        val existing = PukaarApp.instance.repository.contacts()
            .firstOrNull { phonesMatch(it.phone, req.phone) && (it.role == null || it.role == wantedRole) }
            ?: PukaarApp.instance.repository.contacts()
                .firstOrNull { phonesMatch(it.phone, req.phone) }
        val id = existing?.id ?: draft.id?.takeIf { it.isNotBlank() } ?: return null
        return try {
            PukaarApp.instance.repository.updateContact(id, req)
            if (shareHighAlert) {
                sendHighAlertInvite(context, draft.name, req.phone, senderName)
            }
            id
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Onboarding batch save: create/update without auto-verifying.
     * Trusted contacts must already be verified via OTP, or will remain pending.
     */
    suspend fun saveQuiet(draft: ContactDraft): Result<String> =
        saveContact(context = PukaarApp.instance, draft = draft, senderName = null, shareHighAlert = false)

    @Deprecated("Use saveQuiet — contacts must be verified with the user-entered OTP")
    suspend fun saveVerifiedQuiet(draft: ContactDraft): Result<String> = saveQuiet(draft)

    suspend fun deleteContact(id: String): Result<Unit> = try {
        PukaarApp.instance.repository.deleteContact(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Asks the server to re-issue and send the OTP. Optionally shares High Alert install SMS. */
    suspend fun resendVerification(
        context: Context,
        contact: ContactUiModel,
        senderName: String?,
        shareHighAlert: Boolean = true
    ): Result<ContactDto> = try {
        val updated = PukaarApp.instance.repository.resendVerification(contact.id)
        if (shareHighAlert) {
            sendHighAlertInvite(context, contact.name, contact.phoneNumber, senderName)
        }
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun verifyContact(id: String, code: String): Result<ContactDto> = try {
        if (code.isBlank()) {
            Result.failure(Exception("Enter the verification code"))
        } else {
            Result.success(PukaarApp.instance.repository.verifyContact(id, code.trim()))
        }
    } catch (e: Exception) {
        Result.failure(Exception(e.userMessage()))
    }

    /** High Alert Play Store invite — not the server OTP. */
    private suspend fun sendHighAlertInvite(
        context: Context,
        name: String,
        phone: String,
        senderName: String?
    ) {
        val from = senderName?.takeIf { it.isNotBlank() } ?: "I"
        val message = "$from added you as a PUKAAR emergency contact ($name). " +
            "Install PUKAAR High Alert to receive their safety alerts: " +
            "https://play.google.com/store/apps/details?id=com.pukaar.highalert"
        withContext(Dispatchers.IO) {
            SmsHelper.sendSmsInBackground(context, phone, message)
        }
    }
}
