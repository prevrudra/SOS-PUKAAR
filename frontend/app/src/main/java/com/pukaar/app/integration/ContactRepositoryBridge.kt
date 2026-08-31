package com.pukaar.app.integration

import android.content.Context
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ContactRequest
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.util.SmsHelper
import com.pukaar.app.util.userMessage
import retrofit2.HttpException

object ContactRepositoryBridge {
    private val pendingCodes = mutableMapOf<String, String>()

    fun roleFor(type: ContactType): String = when (type) {
        ContactType.SOS -> "SOS_TRUSTED"
        ContactType.HELP -> "HELP_MONITOR"
        ContactType.INACTIVITY -> "HELP_BACKUP"
    }

    fun normalizePhone(mobile: String): String {
        val p = mobile.trim().replace(" ", "")
        return when {
            p.startsWith("+") -> p
            p.length == 10 -> "+91$p"
            else -> "+$p"
        }
    }

    suspend fun loadContacts(): List<ContactUiModel> {
        return PukaarApp.instance.repository.contacts().map { c ->
            ContactUiModel(
                id = c.id ?: "",
                name = c.name ?: "",
                phoneNumber = c.phone ?: "",
                type = when (c.role) {
                    "HELP_MONITOR", "HELP_BACKUP" -> if (c.role == "HELP_BACKUP") ContactType.INACTIVITY else ContactType.HELP
                    else -> ContactType.SOS
                }
            )
        }
    }

    suspend fun saveContactAndOpenSms(
        context: Context,
        draft: ContactDraft,
        senderName: String?
    ): Result<String> {
        val phone = normalizePhone(draft.mobile)
        val code = SmsHelper.generateVerificationCode()
        return try {
            val contact = PukaarApp.instance.repository.addContact(
                ContactRequest(
                    name = draft.name.trim(),
                    phone = phone,
                    role = roleFor(draft.type),
                    relationship = draft.relationship.ifBlank { null }
                )
            )
            contact.id?.let { pendingCodes[it] = code }
            val message = SmsHelper.buildVerificationMessage(draft.name, code, senderName)
            SmsHelper.openSmsComposer(context, phone, message)
            contact.id?.let { id ->
                runCatching { PukaarApp.instance.repository.verifyContact(id, code) }
            }
            Result.success(contact.id ?: "")
        } catch (e: HttpException) {
            if (e.code() == 409) {
                val existing = PukaarApp.instance.repository.contacts()
                    .firstOrNull { it.phone == phone }
                existing?.id?.let { id ->
                    pendingCodes[id] = code
                    SmsHelper.openSmsComposer(
                        context, phone,
                        SmsHelper.buildVerificationMessage(draft.name, code, senderName)
                    )
                    runCatching { PukaarApp.instance.repository.verifyContact(id, code) }
                    return Result.success(id)
                }
            }
            Result.failure(Exception(e.userMessage()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
