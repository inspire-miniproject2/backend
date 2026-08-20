-- V1 stored read_at using the application-local Asia/Seoul wall clock.
-- Normalize existing values to the UTC convention used by notification timestamps.
UPDATE notifications
SET read_at = DATE_SUB(read_at, INTERVAL 9 HOUR)
WHERE read_at IS NOT NULL;
