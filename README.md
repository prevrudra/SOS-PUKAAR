# PUKAAR

Personal emergency communication and elderly HELP system (BRD v1.1).

## Structure

| Folder | Stack |
|--------|--------|
| `backend/` | Java 17 · Spring Boot 3.4 · PostgreSQL 17 · Flyway · JWT · Actuator |
| `frontend/` | Kotlin · Jetpack Compose · Android 8+ (minSdk 26, targetSdk 35) |
| `docker-compose.yml` | Postgres + Redis + backend |

## Quick start — backend

```bash
# Start database (and optional Redis)
docker compose up -d postgres redis

# Run API
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`

Dev OTP is enabled (`OTP_MOCK_ENABLED=true`). Default code: `123456`.

### Core API groups

- `/api/v1/auth/otp/*` — phone OTP login
- `/api/v1/me` — profile, home mode, consent
- `/api/v1/contacts` — trusted SOS / HELP contacts
- `/api/v1/emergencies/*` — SOS/HELP engine, location, audio segments, I'm Safe
- `/api/v1/subscription` — Individual ₹499 / Family ₹699 / referral upgrade
- `/api/v1/elderly/*` — HELP settings + heartbeat / inactivity

Emergency state machine:

`TRIGGERED → LOCATION_ACQUIRED → CONTACTS_NOTIFIED → AUDIO_RECORDING_ACTIVE → SEGMENTS_UPLOADING → CALL_112_INITIATED → LIVE_LOCATION_ACTIVE → WAITING_SAFE → CLOSED`

## Quick start — Android (`frontend/`)

1. Open `frontend/` in Android Studio (Ladybug+ / AGP 8.9).
2. Set SDK in `local.properties` (`sdk.dir=...`) if needed.
3. Emulator uses `http://10.0.2.2:8080/` (see `BuildConfig.API_BASE_URL`).
4. Run the `app` configuration.

### UX (approved BRD)

- **Only two primary screens:** Home (large SOS or HELP) + Menu (2-tile grid).
- Home mode chosen in onboarding (not a persistent toggle on Home).
- Black, high-contrast safety UI.

### Android rule overrides for emergency reliability

Manifest and runtime helpers intentionally request / guide:

- Background location, mic, phone, notifications, full-screen intents
- Foreground services: `location | microphone | specialUse | dataSync`
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- OEM autostart / battery whitelist intents (Xiaomi, Huawei, Oppo, Vivo, Samsung, …)
- Overlay permission, boot completed re-arm, screen on/off cadence fallback for hardware trigger
- Partial wake lock during active emergency
- Show-when-locked / turn-screen-on on `MainActivity`

**Important:** Power-button sequences, always-on voice hotword, automatic 112 dialing, and unrestricted background mic are **OS/OEM dependent**. The app includes best-effort paths and must be validated on each OEM. Product copy must not promise guaranteed rescue.

## Commercial model (backend-enforced)

- Individual ₹499/year · Family ₹699/year (up to 5)
- 3 successful **paid/activated** referrals → Family at ₹499
- Abuse checks: self-referral / device reuse flags
- Mock drill required before protection activation

## Disclaimer

PUKAAR alerts trusted people and attempts emergency pathways. It does **not** replace police, ambulance, or government response, and does not guarantee network, GPS, or device availability.
