ALTER TABLE emergency_events
    ADD COLUMN IF NOT EXISTS battery_pct INT,
    ADD COLUMN IF NOT EXISTS network_type VARCHAR(32);

ALTER TABLE emergency_contact_deliveries
    ADD COLUMN IF NOT EXISTS channel_used VARCHAR(32);

CREATE TABLE IF NOT EXISTS contact_alert_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_e164 VARCHAR(20) NOT NULL,
    fcm_token VARCHAR(512),
    device_id VARCHAR(128),
    platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_device_phone ON contact_alert_devices(phone_e164) WHERE active = TRUE;
CREATE INDEX IF NOT EXISTS idx_alert_device_fcm ON contact_alert_devices(fcm_token) WHERE fcm_token IS NOT NULL;
