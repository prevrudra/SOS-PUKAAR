# PUKAAR Inactivity Feature — How It Works

**Version:** V1 (shipped in PUKAAR 1.20.0+)  
**Purpose:** Welfare check if the user has had no qualifying phone activity for a chosen period. This is **not** an SOS emergency alert.

---

## 1. User setup (in app)

| Step | What the user does | Limits / defaults |
|------|--------------------|-------------------|
| 1 | Turns on **Inactivity Protection** | Monitoring enabled in elderly settings |
| 2 | Selects **duration** | **12 / 18 / 24 / 30 / 36 hours** (default **12**) |
| 3 | Adds **trusted contacts** | Max **2** (`HELP_BACKUP`) — these receive WhatsApp |
| 4 | Adds **pre-saved help numbers** | Max **2** — **not** auto-alerted; shown on View More |
| 5 | Optionally adds **medical info** | Conditions, medications, allergies, blood group |

Contacts must be **OTP-verified**. High Alert app link can be shared after verify.

India plan → contacts `+91` only. Global plan → international numbers allowed.

---

## 2. What counts as “activity”

Qualifying activity = phone usage signals the app can detect (unlock / usage access), then a **heartbeat** to the server:

```
Device activity detected
        ↓
POST /api/v1/elderly/heartbeat
        ↓
users.last_activity_at = now
        ↓
Any open inactivity episode is resolved (timer resets)
```

**Important**

- Activity is **not** “opened WhatsApp” or “made a call.”
- Any qualifying activity **before** the duration expires **resets the timer to zero**.
- There is **one threshold only** — no 6h / 10h warning levels.

---

## 3. Timer logic (core rule)

```
last_qualifying_activity_timestamp = users.last_activity_at
duration_hours                    = elderly_settings.duration_hours   (12|18|24|30|36)

IF  (now − last_activity_at)  ≥  duration_hours
AND monitoring is enabled
AND we have not already alerted this cycle
THEN  fire ONE inactivity alert
```

### Example (12 hours)

| Time | Event | Result |
|------|--------|--------|
| 08:00 | User active | Window: 08:00 → 20:00 |
| 15:00 | User active again | Timer **resets**; new window 15:00 → 03:00 next day |
| 02:57 next day | Still quiet | Still waiting |
| 03:00 | No activity for 12h | **Alert fires** |
| After alert | User unlocks phone | Episode closes; new cycle starts from that activity |

After an alert is sent, the system **does not spam**. A new cycle starts only after the user is active again.

---

## 4. When the alert fires

1. Backend scheduler scans users with inactivity monitoring on.  
2. Creates / uses an **inactivity episode** with a secret **view token**.  
3. Creates an emergency event with trigger type `INACTIVITY`.  
4. Notifies up to **2 verified trusted contacts** (`HELP_BACKUP`) via:
   - WhatsApp welfare text (preferred)
   - FCM to High Alert (if installed)
   - SMS fallback if needed  

**Message tone (welfare, not emergency):**

```
PUKAAR INACTIVITY ALERT

[Name] has had no activity or response for [N] hours.
Last active: [date/time IST]
Please check on them.

📍 Last available location: [maps link if known]
📍 Current location: [maps link only if obtainable — never fake]

📞 Call [Name]

If you cannot reach them and need additional information:
https://pukaaralert.com/pukaar/view/{token}
```

The message must **not** say the user is in danger, or that police/ambulance are required.

---

## 5. View More Information (trusted contact)

URL: `https://pukaaralert.com/pukaar/view/{token}`  
(Public — no login. Token is unguessable.)

Trusted contact sees (only what exists):

1. User name + last active time  
2. Location (if available; India plan blocks enrichment outside India)  
3. **Trusted contacts** (coordinate)  
4. **Pre-saved help numbers** (call if needed — not auto-notified)  
5. **Nearest police / ambulance / hospital** (region-gated)  
6. **Medical info** (only fields the user filled)  
7. **112 — National Emergency**

---

## 6. End-to-end flow (steps)

```
USER ENABLES INACTIVITY
        ↓
SELECT DURATION (12 / 18 / 24 / 30 / 36)
        ↓
ADD UP TO 2 TRUSTED CONTACTS (verify + share High Alert)
        ↓
ADD UP TO 2 PRE-SAVED HELP NUMBERS
        ↓
OPTIONAL MEDICAL INFO
        ↓
MONITOR: last_activity_at updated on heartbeats
        ↓
    ┌── Qualifying activity? ── YES → RESET TIMER (resolve episode)
    │
    NO → keep waiting
        ↓
Duration reached with no activity?
        ↓ YES
ONE INACTIVITY ALERT → WhatsApp (+ FCM/SMS) to trusted contacts
        ↓
Trusted contact calls / checks on user
        ↓
    ┌── User responds / becomes active → cycle ends; timer restarts
    │
    └── Cannot reach user → open View More link → use help numbers /
        nearby services / medical / 112 as appropriate
```

---

## 7. Technical map (for developers)

| Piece | Location |
|-------|----------|
| Device activity → heartbeat | `PhoneUsageTracker` → `POST /api/v1/elderly/heartbeat` |
| Settings (duration, medical) | `GET/PUT /api/v1/elderly/settings` |
| Scheduler scan | `InactivityScheduler` → `InactivityService.scanAll()` |
| Single-threshold logic | `InactivityService.processUser()` |
| Deliver to contacts | `EmergencyOrchestrator.deliverInactivityAlertToTrusted()` |
| WhatsApp welfare copy | `AlertDeliveryService.sendInactivityWelfareWhatsApp()` |
| View More HTML | `GET /view/{token}` → `InactivityViewMoreService` |
| Onboarding UI | `InactivityOnboardingScreen` |
| Settings UI | `ElderlyHelpScreen` |

### DB anchors

- `users.last_activity_at` — last qualifying activity  
- `elderly_settings.duration_hours` — single threshold  
- `inactivity_episodes` — open cycle, `alerted`, `view_token`, `resolved_at`  
- Trusted recipients: `trusted_contacts.contact_role = HELP_BACKUP` (max 2 active)

---

## 8. What this is / is not

| Is | Is not |
|----|--------|
| Welfare / check-in alert | SOS or HELP emergency |
| One timer, one alert per quiet cycle | Soft → medium → urgent escalation |
| Trusted contacts notified | Pre-saved help numbers auto-notified |
| View More for extra context | Proof that the user is in danger |

---

## 9. Quick QA checklist

- [ ] Set duration to 12h (or shorter test via DB if needed)  
- [ ] Add 2 verified inactivity trusted contacts  
- [ ] Add 1–2 help numbers + optional medical  
- [ ] Confirm heartbeat updates `last_activity_at` after using the phone  
- [ ] Force quiet past duration → one WhatsApp welfare message  
- [ ] Open View More link → contacts / help / nearby / medical / 112  
- [ ] User opens app again → episode resolves; no repeat alert until another full quiet window  
- [ ] India plan abroad: nearby on View More limited; Global: works worldwide  
