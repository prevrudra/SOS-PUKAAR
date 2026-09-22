-- India/Global plan region on subscriptions
ALTER TABLE subscriptions
    ADD COLUMN IF NOT EXISTS region VARCHAR(20) NOT NULL DEFAULT 'INDIA';

ALTER TABLE payment_orders
    ADD COLUMN IF NOT EXISTS region VARCHAR(20) NOT NULL DEFAULT 'INDIA';

-- Inactivity V1: single duration threshold + medications + view-more token
ALTER TABLE elderly_settings
    ADD COLUMN IF NOT EXISTS duration_hours INT NOT NULL DEFAULT 12,
    ADD COLUMN IF NOT EXISTS medications TEXT;

UPDATE elderly_settings
SET duration_hours = GREATEST(12, LEAST(36, COALESCE(urgent_hours, medium_hours, soft_hours, 12)))
WHERE duration_hours = 12 AND (urgent_hours IS NOT NULL OR medium_hours IS NOT NULL);

ALTER TABLE inactivity_episodes
    ADD COLUMN IF NOT EXISTS view_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS alerted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_inactivity_episode_view_token
    ON inactivity_episodes(view_token)
    WHERE view_token IS NOT NULL;

-- Contact verification OTP
ALTER TABLE trusted_contacts
    ADD COLUMN IF NOT EXISTS verify_code_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS verify_code_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verify_sent_at TIMESTAMPTZ;
