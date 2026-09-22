# Play Store release — PUKAAR + High Alert

## Built artifacts (`release/`)

| File | App | Version |
|------|-----|---------|
| `pukaar-1.19.3-50.aab` | PUKAAR (`pukaar.com`) | 1.19.3 (50) |
| `pukaar-1.19.2-49.aab` | PUKAAR (`pukaar.com`) | 1.19.2 (49) |
| `pukaar-1.19.1-48.aab` | PUKAAR (`pukaar.com`) | 1.19.1 (48) |
| `pukaar-1.19.0-47.aab` | PUKAAR (`pukaar.com`) | 1.19.0 (47) |
| `pukaar-1.18.7-44.aab` | PUKAAR (`pukaar.com`) | 1.18.7 (44) |
| `pukaar-1.18.6-43.aab` | PUKAAR (`pukaar.com`) | 1.18.6 (43) |
| `pukaar-1.18.5-42.aab` | PUKAAR (`pukaar.com`) | 1.18.5 (42) |
| `pukaar-1.18.4-41.aab` | PUKAAR (`pukaar.com`) | 1.18.4 (41) |
| `pukaar-1.18.3-40.aab` | PUKAAR (`pukaar.com`) | 1.18.3 (40) |
| `highalert-1.13.19-49.aab` | High Alert (`com.pukaar.highalert`) | 1.13.19 (49) |
| `highalert-1.13.16-46.aab` | High Alert (`com.pukaar.highalert`) | 1.13.16 (46) |
| `highalert-1.13.12-42.aab` | High Alert (`com.pukaar.highalert`) | 1.13.12 (42) |
| `highalert-1.13.11-41.aab` | High Alert (`com.pukaar.highalert`) | 1.13.11 (41) |
| `highalert-1.13.10-40.aab` | High Alert (`com.pukaar.highalert`) | 1.13.10 (40) |
| `highalert-1.13.9-39.aab` | High Alert (`com.pukaar.highalert`) | 1.13.9 (39) |
| `highalert-1.13.7-37.aab` | High Alert (`com.pukaar.highalert`) | 1.13.7 (37) |

Signed with `deploy/secrets/pukaar-upload.jks`.

## PUKAAR — internal testing

- **Internal testing:** completed versionCode **47** (`1.19.0`) — 2026-09-21
- Fix: Motorola crash loop — FGS only with notification permission; GPS backup for location updates
- Prior **44** (`1.18.7`): crash loop hardening — foreground-only FGS, resumeIfNeeded, receiver safety, contacts max 3
- Prior **43** (`1.18.6`): "Pukaar keeps stopping" — no FGS from background
- Prior **42** (`1.18.5`): Motorola/background SOS guard retries
- Prior **41** (`1.18.4`): login/splash no longer stuck on "Starting…"
- Prior **40** (`1.18.3`): WhatsApp SOS dedup; contact add/edit fixes

## High Alert — internal testing

- **Internal testing:** completed versionCode **47** (`1.13.17`) — 2026-09-21
- Fix: Motorola crash — proper notification icon, FGS gated on POST_NOTIFICATIONS
- Prior **42** (`1.13.12`): INACTIVITY alert template mapping + inactivity message copy
- Prior **41** (`1.13.11`): caller ID contact save with manual save button + toast
- Prior **40** (`1.13.10`): same caller ID fix (superseded by 41)
- Prior **39** (`1.13.9`): auto-save **PUKAAR High Alert** (+918037126014) on login (caller ID name)
- Prior **38** (`1.13.8`): stop alert loop fix
- Prior **37** (`1.13.7`) — verified on Play 2026-09-19
- Fix: login no longer stuck on "Starting…" (FCM timeout + background register)
- Fix: phone numbers match WhatsApp E.164 format

## Backend (VPS)

- **Deployed:** latest on `pukaaralert.com/pukaar/` — 2026-09-21
- WhatsApp pukaar_sos template, I'm Safe template, live location every 2 min, Meta webhook
- Contact limit 3 per role, admin recordings IST, AuthKey voice ~25s after SOS

## Re-upload later

```bash
cd frontend && ./gradlew :app:bundleRelease :highalert:bundleRelease
python3 scripts/upload_play_aab.py --package pukaar.com --aab release/pukaar-VERSION.aab --track internal --status completed
python3 scripts/upload_play_aab.py --package com.pukaar.highalert --aab release/highalert-VERSION.aab --track internal --status completed
```
