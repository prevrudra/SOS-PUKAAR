package com.pukaar.app.integration

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ElderlySettingsDto
import com.pukaar.app.data.api.ProfileUpdateRequest
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.emergency.OemBatteryHelper
import com.pukaar.app.payment.RazorpayPaymentBridge
import com.pukaar.app.ui.navigation.PukaarActions
import com.pukaar.app.ui.navigation.SubscriptionUi
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow
import com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.language.AppLanguage
import com.pukaar.app.ui.screen.notifications.NotificationPreferences
import com.pukaar.app.ui.screen.sossettings.SosSettingsForm
import com.pukaar.app.util.DeviceTelemetry
import com.pukaar.app.util.EmergencyAlertHelper
import com.pukaar.app.util.SmsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class PukaarActionsImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onEmergency: (String, Boolean) -> Unit,
    private val onError: (String) -> Unit
) : PukaarActions {

    override fun triggerSos() = triggerEmergency(isSos = true, mockDrill = false)
    override fun triggerHelp() = triggerEmergency(isSos = false, mockDrill = false)

    override fun startMockDrill(isSos: Boolean) = triggerEmergency(isSos = isSos, mockDrill = true)

    private fun triggerEmergency(isSos: Boolean, mockDrill: Boolean) {
        scope.launch {
            try {
                val settings = PukaarApp.instance.sessionStore.sosSettings()
                val loc = if (settings.location) {
                    runCatching {
                        val client = LocationServices.getFusedLocationProviderClient(context)
                        client.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            CancellationTokenSource().token
                        ).await()
                    }.getOrNull()
                } else null

                val event = PukaarApp.instance.repository.trigger(
                    TriggerRequest(
                        triggerType = when {
                            mockDrill -> "MOCK_DRILL"
                            isSos -> "APP"
                            else -> "HELP"
                        },
                        latitude = loc?.latitude,
                        longitude = loc?.longitude,
                        accuracyM = loc?.accuracy?.toDouble(),
                        mockDrill = mockDrill,
                        batteryPct = DeviceTelemetry.batteryPercent(context),
                        networkType = DeviceTelemetry.networkType(context)
                    )
                )
                val id = event.id ?: return@launch
                if (!mockDrill) {
                    EmergencyForegroundService.start(
                        context, id, isSos = isSos, recordAudio = isSos && settings.audio
                    )
                }

                if (settings.alertContacts) {
                    val smsResult = withContext(Dispatchers.IO) {
                        EmergencyAlertHelper.sendSmsToContactsInBackground(
                            context, event, isSos, mockDrill
                        )
                    }
                    when {
                        smsResult.success -> {
                            android.util.Log.i("PUKAAR", "Emergency SMS sent to ${smsResult.sent} contact(s)")
                        }
                        !SmsHelper.hasSendSmsPermission(context) -> {
                            onError("SMS permission blocked — confirm send in your SMS app when it opens")
                        }
                        else -> {
                            onError("Could not send SMS to contacts. Check signal and SMS permission.")
                        }
                    }
                }

                if (!mockDrill && settings.autoCall && isSos) {
                    EmergencyAlertHelper.call112InBackground(context)
                }

                onEmergency(id, mockDrill)
            } catch (e: Exception) {
                onError(e.message ?: "Could not start emergency")
            }
        }
    }

    override fun updateHomeMode(mode: HomeMode) {
        scope.launch {
            val apiMode = if (mode == HomeMode.SOS) "SOS" else "HELP"
            runCatching {
                PukaarApp.instance.repository.updateProfile(ProfileUpdateRequest(homeMode = apiMode))
                PukaarApp.instance.sessionStore.setHomeMode(apiMode)
            }.onFailure { onError(it.message ?: "Could not update home mode") }
        }
    }

    override fun saveContact(draft: ContactDraft) {
        scope.launch {
            val name = runCatching { PukaarApp.instance.repository.me().fullName }.getOrNull()
            ContactRepositoryBridge.saveContact(context, draft, name)
                .onFailure { onError(it.message ?: "Could not save contact") }
        }
    }

    override fun saveSosSettings(form: SosSettingsForm) {
        scope.launch {
            runCatching {
                PukaarApp.instance.sessionStore.saveSosSettings(
                    form.autoCallEmergencyNumber,
                    form.shareLocation,
                    form.recordAudio,
                    form.alertContacts
                )
                PukaarApp.instance.repository.updateProfile(
                    ProfileUpdateRequest(
                        consentLocation = form.shareLocation,
                        consentAudio = form.recordAudio
                    )
                )
            }.onFailure { onError(it.message ?: "Could not save SOS settings") }
        }
    }

    override suspend fun loadSosSettings(): SosSettingsForm {
        val prefs = PukaarApp.instance.sessionStore.sosSettings()
        return SosSettingsForm(
            autoCallEmergencyNumber = prefs.autoCall,
            shareLocation = prefs.location,
            recordAudio = prefs.audio,
            alertContacts = prefs.alertContacts
        )
    }

    override fun loadContacts(): List<ContactUiModel> = emptyList()

    override fun openContact(contact: ContactUiModel) {
        val code = SmsHelper.generateVerificationCode()
        val name = runCatching {
            kotlinx.coroutines.runBlocking { PukaarApp.instance.repository.me().fullName }
        }.getOrNull()
        val message = SmsHelper.buildVerificationMessage(contact.name, code, name)
        scope.launch {
            SmsHelper.sendSmsInBackground(context, contact.phoneNumber, message)
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
                        inactivityMonitoringEnabled = true,
                        medicationReminderEnabled = medicationReminder
                    )
                )
            }.onFailure { onError(it.message ?: "Could not save elderly settings") }
        }
    }

    override suspend fun loadElderlyHelp(): Pair<InactivityWindow, Boolean> {
        val s = runCatching { PukaarApp.instance.repository.elderlySettings() }.getOrNull()
        val window = when (s?.mediumHours) {
            6 -> InactivityWindow.SIX
            12 -> InactivityWindow.TWELVE
            else -> InactivityWindow.TEN
        }
        return window to (s?.medicationReminderEnabled != false)
    }

    override fun saveEmergencyInfo(form: EmergencyInfoForm) {
        scope.launch {
            runCatching {
                PukaarApp.instance.sessionStore.saveDoctorPhone(form.doctorPhone)
                PukaarApp.instance.repository.updateElderlySettings(
                    ElderlySettingsDto(
                        doctorPhone = form.doctorPhone.ifBlank { null },
                        bloodGroup = form.bloodGroup.ifBlank { null },
                        allergies = form.allergies.ifBlank { null },
                        medicalConditions = form.conditions.ifBlank { null }
                    )
                )
            }.onFailure { onError(it.message ?: "Could not save emergency info") }
        }
    }

    override suspend fun loadEmergencyInfo(): EmergencyInfoForm {
        val s = runCatching { PukaarApp.instance.repository.elderlySettings() }.getOrNull()
        return EmergencyInfoForm(
            bloodGroup = s?.bloodGroup.orEmpty(),
            allergies = s?.allergies.orEmpty(),
            conditions = s?.medicalConditions.orEmpty(),
            doctorPhone = s?.doctorPhone ?: PukaarApp.instance.sessionStore.doctorPhone()
        )
    }

    override fun upgradePlan(plan: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        scope.launch {
            try {
                val config = runCatching { PukaarApp.instance.repository.paymentConfig() }.getOrNull()
                val razorpayEnabled = config?.enabled == true
                if (!razorpayEnabled) {
                    PukaarApp.instance.repository.activate(plan)
                    PukaarApp.instance.repository.completeOnboarding()
                    PukaarApp.instance.sessionStore.setProtectionReady(true)
                    onSuccess()
                    return@launch
                }
                val order = PukaarApp.instance.repository.createPaymentOrder(plan)
                val activity = context as? Activity
                if (activity == null) {
                    onFailure("Payment requires app activity")
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    RazorpayPaymentBridge.startCheckout(activity, order) { result ->
                        result.onSuccess { payment ->
                            scope.launch {
                                runCatching {
                                    PukaarApp.instance.repository.verifyPayment(
                                        payment.orderId,
                                        payment.paymentId,
                                        payment.signature
                                    )
                                    PukaarApp.instance.repository.completeOnboarding()
                                    PukaarApp.instance.sessionStore.setProtectionReady(true)
                                }.onSuccess { onSuccess() }
                                    .onFailure { onFailure(it.message ?: "Payment verification failed") }
                            }
                        }.onFailure { onFailure(it.message ?: "Payment cancelled") }
                    }
                }
            } catch (e: Exception) {
                onFailure(e.message ?: "Payment failed")
            }
        }
    }

    override fun viewPaymentHistory() {
        scope.launch {
            runCatching {
                val sub = PukaarApp.instance.repository.subscription()
                val plan = sub.subscription?.plan ?: "None"
                val status = sub.subscription?.status ?: "INACTIVE"
                onError("Plan: $plan — Status: $status")
            }.onFailure { onError(it.message ?: "Could not load subscription") }
        }
    }

    override fun shareReferralCode() {
        scope.launch {
            val code = runCatching {
                val stored = PukaarApp.instance.sessionStore.referralCode()
                if (stored.isNotBlank()) stored else PukaarApp.instance.repository.me().referralCode
            }.getOrNull() ?: "PUKAAR"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Join PUKAAR for family safety. Use my referral code: $code")
            }
            context.startActivity(Intent.createChooser(share, "Share referral code"))
        }
    }

    override suspend fun loadSubscriptionUi(): SubscriptionUi {
        val user = runCatching { PukaarApp.instance.repository.me() }.getOrNull()
        val sub = runCatching { PukaarApp.instance.repository.subscription() }.getOrNull()
        user?.referralCode?.let { PukaarApp.instance.sessionStore.saveReferralCode(it) }
        val referral = user?.referralCode
            ?: PukaarApp.instance.sessionStore.referralCode()
            .ifBlank { "PUKAAR" }
        val active = sub?.subscription?.status == "ACTIVE" || sub?.subscription?.status == "GRACE"
        return SubscriptionUi(
            planName = sub?.subscription?.plan ?: "Individual",
            validTill = sub?.subscription?.endsAt?.take(10) ?: "—",
            referralCode = referral,
            isActive = active,
            individualPrice = sub?.plans?.individual ?: 499,
            familyPrice = sub?.plans?.family ?: 699,
            referralCount = sub?.successfulReferrals ?: 0
        )
    }

    override fun playIntroVideo() {
        android.widget.Toast.makeText(context, "Help videos coming soon", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun playTopic(topic: com.pukaar.app.ui.screen.helpvideo.HelpTopic) {
        android.widget.Toast.makeText(context, topic.name, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun saveLanguage(language: AppLanguage) {
        scope.launch {
            runCatching {
                PukaarApp.instance.repository.updateProfile(
                    ProfileUpdateRequest(languageCode = language.tag)
                )
            }
        }
    }

    override fun saveNotificationPreferences(preferences: NotificationPreferences) {
        scope.launch {
            runCatching {
                PukaarApp.instance.sessionStore.saveNotificationPrefs(
                    preferences.alertNotifications,
                    preferences.inactivityAlerts,
                    preferences.promotions
                )
            }
        }
    }

    override suspend fun loadNotificationPreferences(): NotificationPreferences {
        val prefs = PukaarApp.instance.sessionStore.notificationPrefs()
        return NotificationPreferences(
            alertNotifications = prefs.emergency,
            inactivityAlerts = prefs.drill,
            medicationReminders = true,
            promotions = prefs.marketing
        )
    }

    override suspend fun loadLanguage(): AppLanguage {
        val tag = runCatching { PukaarApp.instance.repository.me().languageCode }.getOrNull()
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.ENGLISH
    }

    override fun openFaqEntry(entry: FaqEntry) = Unit

    override fun openSettings() = Unit

    init {
        OemBatteryHelper.requestUnrestrictedBattery(context)
    }
}
