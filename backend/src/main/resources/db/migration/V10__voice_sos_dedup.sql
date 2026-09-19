-- One AuthKey voice call per event + recipient phone.
CREATE TABLE IF NOT EXISTS voice_sos_sent (
    event_id    UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    phone_e164  VARCHAR(20) NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, phone_e164)
);
