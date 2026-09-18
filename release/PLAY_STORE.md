# Play Store release — PUKAAR + High Alert

## Built artifacts (`release/`)

| File | App | Version |
|------|-----|---------|
| `pukaar-1.18.3-40.aab` | PUKAAR (`pukaar.com`) | 1.18.3 (40) |
| `highalert-1.13.5-35.aab` | High Alert (`com.pukaar.highalert`) | 1.13.5 (35) |

Signed with `deploy/secrets/pukaar-upload.jks`.

## PUKAAR — internal testing

- **Internal testing:** completed versionCode **40** (`1.18.3`)
- One WhatsApp SOS per event+phone (dedup table); contact add/edit fixes
- Deployed with backend `e7db7bd` (2026-09-18)

## High Alert — internal testing

- **Internal testing:** completed versionCode **35** (`1.13.5`)
- Fix: Stop alert no longer loops — silences locally + server READ ack
- Fix: watchdog/FCM skip dismissed events; no re-arm on data enrich

## Re-upload later

```bash
cd frontend && ./gradlew :app:bundleRelease :highalert:bundleRelease
python3 scripts/upload_play_aab.py --package pukaar.com --aab release/pukaar-VERSION.aab --track internal --status completed
python3 scripts/upload_play_aab.py --package com.pukaar.highalert --aab release/highalert-VERSION.aab --track internal --status completed
```
