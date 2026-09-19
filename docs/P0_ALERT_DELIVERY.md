# P0: High Alert delivery architecture

## Problem
SOS fires on server, but trusted contact's **PUKAAR High Alert** does not grab the screen unless they open the app. Unacceptable for a safety product.

## Correct architecture (P0)

```
Elder triggers SOS
    → PUKAAR server
        → FCM high-priority push (instant, battery-safe)  ← PRIMARY for High Alert
        → WhatsApp template (parallel, separate app)
        → 60s poll fallback if push missed (Oppo/Xiaomi)
    → High Alert FirebaseMessagingService
        → AlertFireHelper (alarm + full-screen + lock screen)
        → POST /acknowledge DELIVERED when UI opens
```

**No always-on background service.** Push + short fallback poll only.

## What is implemented

| Layer | Status |
|-------|--------|
| Server FCM send on SOS | ✅ `AlertDeliveryService` — FCM **before** WhatsApp |
| Server `FCM_SERVER_KEY` | ✅ Set on production VPS |
| High Alert `FirebaseMessagingService` | ✅ `HighAlertFirebaseMessagingService.kt` |
| FCM token → `/alert-devices/register` | ✅ `FcmRegistrar.kt` on login + token refresh |
| 60s poll fallback | ✅ `MonitorWatchdogReceiver` |
| Delivery ack on UI open | ✅ `AlertActivity` (not on poll) |
| Battery opt prompt (Oppo) | ✅ `MainActivity` |

## One-time setup required (blocker for instant push)

1. Create Firebase project → add Android apps:
   - `com.pukaar.highalert` (release)
   - `com.pukaar.highalert.debug` (debug APK)
2. Download `google-services.json` → place at:
   `frontend/highalert/google-services.json`
3. Rebuild High Alert APK (`FCM_ENABLED=true` auto-detected).
4. Confirm server `FCM_SERVER_KEY` is the **Legacy server key** from Firebase Console → Project settings → Cloud Messaging.

Without step 2–3, High Alert falls back to **60-second polling** only.

## Backend fallback escalation

If no `DELIVERED` ack within **30 seconds** → SMS to trusted contact (`DeliveryRetryScheduler`).

AuthKey.io voice IVR **~25 seconds** after SOS (if contact has not acked) — `AuthKeyVoiceSender` + `DeliveryRetryScheduler`.

Configure on the VPS: `AUTHKEY_AUTH_KEY`, `AUTHKEY_VOICE_ENABLED=true`, `VOICE_ESCALATION=true`.

## iOS (future)

APNs with Time Sensitive / Critical Alerts + same `/acknowledge` API.

## Testing checklist

1. Contact installs High Alert, logs in with **same number** as SOS trusted contact.
2. Tap **Allow background alerts** → unrestricted battery (Oppo: Autostart ON).
3. Force-close High Alert (swipe away, do not force-stop).
4. Trigger SOS from main app.
5. **Expected:** Full-screen alert within ~5 seconds (FCM) or ≤60s (poll fallback).
