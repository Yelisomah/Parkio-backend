-- Email-only and phone-only accounts are both valid registration paths.
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;