package com.pukaar.app.integration

import android.content.Context
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ElderlySettingsDto
import com.pukaar.app.data.api.ProfileUpdateRequest
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.emergency.OemBatteryHelper
import com.pukaar.app.ui.navigation.PukaarActions
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.helpvideo.HelpTopic
import com.pukaar.app.ui.screen.language.AppLanguage
import com.pukaar.app.ui.screen.notifications.NotificationPreferences
import com.pukaar.app.ui.screen.sossettings.SosSettingsForm
import com.pukaar.app.util.SmsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class PukaarActionsImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onEmergency: (String) -> Unit,
    private val onError: (String) -> Unit
) : PukaarActions {

    override fun triggerSos() = triggerEmergency(isSos = true)
    override fun triggerHelp() = triggerEmergency(isSos = false)

    private fun triggerEmergency(isSos: Boolean) {
        scope.launch {
            try {
                val loc = runCatching {
                    val client = LocationServices.getFusedLocationProviderClient(context)
                    client.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                }.getOrNull()
                val event = PukaarApp.instance.repository.trigger(
                    TriggerRequest(
                        triggerType = if (isSos) "APP" else "HELP",
                        latitude = loc?.latitude,
                        longitude = loc?.longitude,
                        accuracyM = loc?.accuracy?.toDouble(),
                        mockDrill = false
                    )
                )
                val id = event.id ?: return@launch
                EmergencyForegroundService.start(context, id, isSos)
                onEmergency(id)
            } catch (e: Exception) {
                onError(e.message ?: "Could not start emergency")
            }
        }
    }

    override fun saveContact(draft: ContactDraft) {
        scope.launch {
            val name = runCatching { PukaarApp.instance.repository.me().fullName }.getOrNull()
            ContactRepositoryBridge.saveContactAndOpenSms(context, draft, name)
                .onFailure { onError(it.message ?: "Could not save contact") }
        }
    }

    override fun saveSosSettings(form: SosSettingsForm) = Unit
    override fun startMockDrill() = Unit

    override fun loadContacts(): List<ContactUiModel> = emptyList()

    override fun openContact(contact: ContactUiModel) {
        val code = SmsHelper.generateVerificationCode()
        val name = runCatching {
            kotlinx.coroutines.runBlocking { PukaarApp.instance.repository.me().fullName }
        }.getOrNull()
        val message = SmsHelper.buildVerificationMessage(contact.name, code, name)
        SmsHelper.openSmsComposer(context, contact.phoneNumber, message)
        scope.launch {
            runCatching { PukaarApp.instance.repository.verifyContact(contact.id, code) }
        }
    }

    override fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean) {
        scope.launch {
            runCatching {
                PukaarApp.instance.repository.updateElderlySettings(
                    ElderlySettingsDto(
                        softHours = 6,
                        mediumHours = window.hours,
                        urgentHours = 12,
                        inactivityMonitoringEnabled = true
                    )
                )
            }.onFailure { onError(it.message ?: "Could not save elderly settings") }
        }
    }

    override fun editBloodGroup() = Unit
    override fun editAllergies() = Unit
    override fun editConditions() = Unit
    override fun saveEmergencyInfo(doctorContact: String) = Unit

    override fun upgradePlan() {
        scope.launch {
            runCatching {
                PukaarApp.instance.repository.activate("INDIVIDUAL")
                PukaarApp.instance.repository.completeOnboarding()
            }.onFailure { onError(it.message ?: "Activation failed") }
        }
    }

    override fun viewPaymentHistory() = Unit
    override fun shareReferralCode() = Unit
    override fun playIntroVideo() = Unit
    override fun playTopic(topic: HelpTopic) = Unit

    override fun saveLanguage(language: AppLanguage) {
        scope.launch {
            runCatching {
                PukaarApp.instance.repository.updateProfile(
                    ProfileUpdateRequest(languageCode = language.tag)
                )
            }
        }
    }

    override fun saveNotificationPreferences(preferences: NotificationPreferences) = Unit
    override fun openFaqEntry(entry: FaqEntry) = Unit
    override fun openSettings() = Unit

    init {
        OemBatteryHelper.requestUnrestrictedBattery(context)
    }
}
