-- One WhatsApp SOS template per event + recipient phone (prevents duplicate sends
-- when multiple delivery rows share the same number or retries race).
CREATE TABLE IF NOT EXISTS whatsapp_sos_sent (
    event_id    UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    phone_e164  VARCHAR(20) NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, phone_e164)
);
