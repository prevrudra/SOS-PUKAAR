ALTER TABLE emergency_events
    ADD COLUMN IF NOT EXISTS view_token VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS ux_emergency_events_view_token
    ON emergency_events (view_token)
    WHERE view_token IS NOT NULL;
