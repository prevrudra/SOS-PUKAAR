# Android / OEM feasibility notes (PUKAAR V1)

These behaviors are **best-effort** and must be validated on Samsung, Xiaomi/Redmi, Motorola, OnePlus, Vivo, Oppo, Realme, and Pixel before launch claims.

| Feature | Status | Notes |
|---------|--------|-------|
| App SOS / HELP button | Core | Reliable when app can run |
| Background location | Partial | Requires grant + FGS; OEM may still throttle |
| Emergency mic + 1-min segments | Partial | Foreground service + user consent; not continuous always-on |
| Cloud evidence | Required | Only mark cloud-safe after upload ACK |
| 112 call pathway | Partial | `CALL_PHONE` / dialer; auto-connect not guaranteed |
| Power-button SOS | Fallback | Screen on/off cadence heuristic — not true key intercept |
| Voice phrase | Placeholder | Needs on-device hotword product; OS limits background mic |
| Battery unrestricted | Guided | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` + OEM autostart intents |
| Lock-screen emergency UI | Best-effort | `showWhenLocked` / `turnScreenOn` |
| Push to trusted contacts | Server | FCM/SMS wiring to be connected in production |

Do not promise guaranteed emergency response in store listings or in-app copy.
