-- Admin/test override: exact inactivity threshold in minutes (0 = use duration_hours).
ALTER TABLE elderly_settings
    ADD COLUMN IF NOT EXISTS duration_minutes INT NOT NULL DEFAULT 0;
