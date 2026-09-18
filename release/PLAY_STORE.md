# Play Store release — PUKAAR + High Alert

## Built artifacts (`release/`)

| File | App | Version |
|------|-----|---------|
| `pukaar-1.18.3-40.aab` | PUKAAR (`pukaar.com`) | 1.18.3 (40) |
| `highalert-1.13.4-34.aab` | High Alert (`com.pukaar.highalert`) | 1.13.4 (34) |

Signed with `deploy/secrets/pukaar-upload.jks`.

## PUKAAR — internal testing

- **Internal testing:** completed versionCode **40** (`1.18.3`)
- One WhatsApp SOS per event+phone (dedup table); contact add/edit fixes

## High Alert — internal testing

- **Internal testing:** versionCode **34** (`1.13.4`) — ready to upload
- Fix: full contact/police/hospital data no longer blocked by ring debounce
- Fix: snapshot fetch retries 5× before showing thin FCM payload
- Fix: pending poll keeps working after DELIVERED ack (server-side)

## Re-upload later

```bash
cd frontend && ./gradlew :app:bundleRelease :highalert:bundleRelease
python3 scripts/upload_play_aab.py --package pukaar.com --aab release/pukaar-VERSION.aab --track internal --status completed
python3 scripts/upload_play_aab.py --package com.pukaar.highalert --aab release/highalert-VERSION.aab --track internal --status completed
```
