package com.pukaar.app.integration

import android.content.Context
import com.pukaar.app.PukaarApp
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

    fun normalizePhone(mobile: String, dialCode: String = "+91"): String =
        PhoneNumbers.fromParts(dialCode, mobile)

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
            ContactUiModel(
                id = c.id ?: "",
                name = c.name ?: "",
                phoneNumber = c.phone ?: "",
                type = typeFromRole(c.role),
                relationship = c.relationship.orEmpty(),
                notes = c.notes.orEmpty(),
                priorityOrder = c.priorityOrder ?: 1,
                verified = c.verified == true
            )
        }
    }

    suspend fun saveContact(
        context: Context,
        draft: ContactDraft,
        senderName: String?
    ): Result<String> {
        val req = toRequest(draft)
        return try {
            val contact = if (draft.id != null) {
                PukaarApp.instance.repository.updateContact(draft.id, req)
            } else {
                PukaarApp.instance.repository.addContact(req)
            }
            val id = contact.id ?: return Result.failure(Exception("Contact not saved"))
            sendVerificationSms(context, draft.name, contact.phone ?: req.phone, id, senderName)
            Result.success(id)
        } catch (e: HttpException) {
            if (e.code() == 409 && draft.id == null) {
                val phone = normalizePhone(draft.mobile)
                val existing = PukaarApp.instance.repository.contacts()
                    .firstOrNull { it.phone == phone }
                existing?.id?.let { id ->
                    val updated = PukaarApp.instance.repository.updateContact(id, req)
                    sendVerificationSms(context, draft.name, updated.phone ?: phone, id, senderName)
                    return Result.success(id)
                }
            }
            Result.failure(Exception(e.userMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteContact(id: String): Result<Unit> = try {
        PukaarApp.instance.repository.deleteContact(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun resendVerification(
        context: Context,
        contact: ContactUiModel,
        senderName: String?
    ) {
        sendVerificationSms(context, contact.name, contact.phoneNumber, contact.id, senderName)
        runCatching {
            PukaarApp.instance.repository.verifyContact(contact.id)
        }
    }

    private suspend fun sendVerificationSms(
        context: Context,
        name: String,
        phone: String,
        id: String,
        senderName: String?
    ) {
        val code = SmsHelper.generateVerificationCode()
        val message = SmsHelper.buildVerificationMessage(name, code, senderName)
        withContext(Dispatchers.IO) {
            SmsHelper.sendSmsInBackground(context, phone, message)
        }
        runCatching { PukaarApp.instance.repository.verifyContact(id, code) }
    }
}
