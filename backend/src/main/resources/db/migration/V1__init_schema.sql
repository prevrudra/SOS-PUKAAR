-- PUKAAR V1 schema — PostgreSQL optimized for emergency throughput
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_e164      VARCHAR(20) NOT NULL UNIQUE,
    phone_hash      VARCHAR(64) NOT NULL UNIQUE,
    full_name       VARCHAR(120),
    language_code   VARCHAR(10) NOT NULL DEFAULT 'en',
    home_mode       VARCHAR(20) NOT NULL DEFAULT 'SOS',
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    fcm_token       TEXT,
    device_id       VARCHAR(128),
    referral_code   VARCHAR(16) NOT NULL UNIQUE,
    referred_by_id  UUID REFERENCES users(id),
    onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE,
    mock_drill_passed   BOOLEAN NOT NULL DEFAULT FALSE,
    protection_ready    BOOLEAN NOT NULL DEFAULT FALSE,
    consent_location    BOOLEAN NOT NULL DEFAULT FALSE,
    consent_audio       BOOLEAN NOT NULL DEFAULT FALSE,
    consent_terms_at    TIMESTAMPTZ,
    last_activity_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_phone_hash ON users(phone_hash);
CREATE INDEX idx_users_referral ON users(referral_code);
CREATE INDEX idx_users_last_activity ON users(last_activity_at) WHERE home_mode = 'HELP';

CREATE TABLE otp_challenges (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_e164  VARCHAR(20) NOT NULL,
    code_hash   VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    attempts    INT NOT NULL DEFAULT 0,
    consumed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_otp_phone ON otp_challenges(phone_e164, created_at DESC);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);

CREATE TABLE trusted_contacts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    phone_e164      VARCHAR(20) NOT NULL,
    contact_role    VARCHAR(30) NOT NULL DEFAULT 'SOS_TRUSTED',
    relationship    VARCHAR(60),
    priority_order  INT NOT NULL DEFAULT 1,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (owner_user_id, phone_e164, contact_role)
);
CREATE INDEX idx_contacts_owner ON trusted_contacts(owner_user_id) WHERE active = TRUE;

CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'NONE',
    price_inr       INT NOT NULL,
    family_slot_limit INT NOT NULL DEFAULT 1,
    starts_at       TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    grace_ends_at   TIMESTAMPTZ,
    store_purchase_token TEXT,
    store_platform  VARCHAR(20),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_sub_active_user ON subscriptions(user_id) WHERE status IN ('ACTIVE', 'GRACE');

CREATE TABLE family_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    member_user_id  UUID NOT NULL REFERENCES users(id),
    invited_phone   VARCHAR(20),
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (subscription_id, member_user_id)
);

CREATE TABLE referrals (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referrer_user_id    UUID NOT NULL REFERENCES users(id),
    referred_user_id    UUID NOT NULL REFERENCES users(id) UNIQUE,
    referred_phone_hash VARCHAR(64) NOT NULL,
    referred_device_id  VARCHAR(128),
    paid_activated      BOOLEAN NOT NULL DEFAULT FALSE,
    abuse_flagged       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    activated_at        TIMESTAMPTZ
);
CREATE INDEX idx_referrals_referrer ON referrals(referrer_user_id);

CREATE TABLE police_stations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    address         TEXT,
    phone_e164      VARCHAR(20),
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    source          VARCHAR(120) NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_police_geo ON police_stations(latitude, longitude) WHERE active = TRUE AND phone_verified = TRUE;

CREATE TABLE hospitals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    address         TEXT,
    phone_e164      VARCHAR(20),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE emergency_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    trigger_type        VARCHAR(20) NOT NULL,
    status              VARCHAR(40) NOT NULL DEFAULT 'TRIGGERED',
    is_mock_drill       BOOLEAN NOT NULL DEFAULT FALSE,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    location_accuracy_m DOUBLE PRECISION,
    location_acquired_at TIMESTAMPTZ,
    call_112_status     VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    police_station_id   UUID REFERENCES police_stations(id),
    closure_reason      VARCHAR(30),
    closed_at           TIMESTAMPTZ,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_emergency_user_active ON emergency_events(user_id, status) WHERE closed_at IS NULL;
CREATE INDEX idx_emergency_started ON emergency_events(started_at DESC);

CREATE TABLE emergency_locations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    accuracy_m  DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_em_loc_event ON emergency_locations(event_id, recorded_at DESC);

CREATE TABLE emergency_contact_deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    contact_id          UUID REFERENCES trusted_contacts(id),
    contact_name        VARCHAR(120) NOT NULL,
    contact_phone       VARCHAR(20) NOT NULL,
    channel             VARCHAR(20) NOT NULL DEFAULT 'PUSH',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts            INT NOT NULL DEFAULT 0,
    last_error          TEXT,
    acknowledged_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_delivery_event ON emergency_contact_deliveries(event_id);

CREATE TABLE audio_evidence_segments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    segment_index   INT NOT NULL,
    duration_sec    INT NOT NULL DEFAULT 60,
    storage_key     TEXT,
    content_type    VARCHAR(80) DEFAULT 'audio/mp4',
    byte_size       BIGINT,
    checksum_sha256 VARCHAR(64),
    upload_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    uploaded_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (event_id, segment_index)
);
CREATE INDEX idx_audio_pending ON audio_evidence_segments(upload_status) WHERE upload_status IN ('PENDING', 'FAILED');

CREATE TABLE emergency_audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id    UUID NOT NULL REFERENCES emergency_events(id) ON DELETE CASCADE,
    actor_user_id UUID,
    action      VARCHAR(80) NOT NULL,
    detail      JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_event ON emergency_audit_logs(event_id, created_at);

CREATE TABLE mock_drills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    event_id        UUID REFERENCES emergency_events(id),
    result          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    location_ok     BOOLEAN,
    contacts_ok     BOOLEAN,
    permissions_ok  BOOLEAN,
    failure_notes   TEXT,
    confirmed_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ
);

CREATE TABLE elderly_settings (
    user_id                     UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    soft_hours                  INT NOT NULL DEFAULT 6,
    medium_hours                INT NOT NULL DEFAULT 10,
    urgent_hours                INT NOT NULL DEFAULT 12,
    escalation_minutes          INT NOT NULL DEFAULT 5,
    inactivity_monitoring_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ambulance_number            VARCHAR(20) DEFAULT '108',
    doctor_name                 VARCHAR(120),
    doctor_phone                VARCHAR(20),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE inactivity_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    level           VARCHAR(20) NOT NULL,
    message         TEXT NOT NULL,
    acknowledged    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    phone_e164      VARCHAR(20),
    fcm_token       TEXT,
    channel         VARCHAR(20) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,
    payload         JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts        INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_outbox_retry ON notification_outbox(status, next_retry_at) WHERE status IN ('PENDING', 'FAILED');

CREATE TABLE app_config (
    key         VARCHAR(80) PRIMARY KEY,
    value       JSONB NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO app_config(key, value) VALUES
('subscription.prices', '{"individual":499,"family":699,"referralFamily":499,"referralsRequired":3,"graceDays":7}'::jsonb),
('emergency.defaults', '{"audioSegmentSeconds":60,"sessionTimeoutHours":4}'::jsonb);
