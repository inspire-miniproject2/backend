ALTER TABLE complaints
    ADD COLUMN email_notification_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER current_status;
