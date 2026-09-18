-- Tracks an inactivity episode from first soft threshold until user responds or contacts escalate.
CREATE TABLE inactivity_episodes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_anchor         TIMESTAMPTZ NOT NULL,
    last_level              VARCHAR(20) NOT NULL DEFAULT 'NONE',
    last_contact_priority   INT NOT NULL DEFAULT 0,
    event_id                UUID REFERENCES emergency_events(id),
    last_alert_at           TIMESTAMPTZ,
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_inactivity_episode_active
    ON inactivity_episodes(user_id)
    WHERE resolved_at IS NULL;

CREATE INDEX idx_inactivity_episode_user ON inactivity_episodes(user_id, created_at DESC);

ALTER TABLE inactivity_alerts
    ADD COLUMN IF NOT EXISTS episode_id UUID REFERENCES inactivity_episodes(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS contact_id UUID REFERENCES trusted_contacts(id) ON DELETE SET NULL;
