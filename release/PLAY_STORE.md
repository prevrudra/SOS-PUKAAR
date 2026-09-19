# Play Store release — PUKAAR + High Alert

## Built artifacts (`release/`)

| File | App | Version |
|------|-----|---------|
| `pukaar-1.18.5-42.aab` | PUKAAR (`pukaar.com`) | 1.18.5 (42) |
| `pukaar-1.18.4-41.aab` | PUKAAR (`pukaar.com`) | 1.18.4 (41) |
| `pukaar-1.18.3-40.aab` | PUKAAR (`pukaar.com`) | 1.18.3 (40) |
| `highalert-1.13.7-37.aab` | High Alert (`com.pukaar.highalert`) | 1.13.7 (37) |

Signed with `deploy/secrets/pukaar-upload.jks`.

## PUKAAR — internal testing

- **Internal testing:** completed versionCode **42** (`1.18.5`) — 2026-09-19
- Fix: Motorola/background SOS — guard service retries after boot; hardware triggers registered dynamically
- Prior **41** (`1.18.4`): login/splash no longer stuck on "Starting…"
- Prior **40** (`1.18.3`): WhatsApp SOS dedup; contact add/edit fixes

## High Alert — internal testing

- **Internal testing:** completed versionCode **37** (`1.13.7`) — verified on Play 2026-09-19
- Fix: login no longer stuck on "Starting…" (FCM timeout + background register)
- Fix: phone numbers match WhatsApp E.164 format

## Backend (VPS)

- **Deployed:** `de24472` on `pukaaralert.com/pukaar/` — 2026-09-19
- AuthKey voice IVR escalation enabled (60s if no ack)
- WhatsApp dedup, phone normalization, stop-loop fixes

## Re-upload later

```bash
cd frontend && ./gradlew :app:bundleRelease :highalert:bundleRelease
python3 scripts/upload_play_aab.py --package pukaar.com --aab release/pukaar-VERSION.aab --track internal --status completed
python3 scripts/upload_play_aab.py --package com.pukaar.highalert --aab release/highalert-VERSION.aab --track internal --status completed
```
