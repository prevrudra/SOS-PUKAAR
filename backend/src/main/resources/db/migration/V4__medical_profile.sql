ALTER TABLE elderly_settings ADD COLUMN IF NOT EXISTS blood_group VARCHAR(10);
ALTER TABLE elderly_settings ADD COLUMN IF NOT EXISTS allergies TEXT;
ALTER TABLE elderly_settings ADD COLUMN IF NOT EXISTS medical_conditions TEXT;
ALTER TABLE elderly_settings ADD COLUMN IF NOT EXISTS medication_reminder_enabled BOOLEAN DEFAULT TRUE;
