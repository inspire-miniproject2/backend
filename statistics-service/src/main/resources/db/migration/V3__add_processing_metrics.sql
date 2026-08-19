ALTER TABLE complaint_statistic_sources
    ADD COLUMN submitted_at DATETIME(6) NULL AFTER current_status,
    ADD COLUMN due_at DATETIME(6) NULL AFTER submitted_at,
    ADD COLUMN completed_at DATETIME(6) NULL AFTER due_at,
    ADD INDEX idx_statistic_sources_due_at (due_at),
    ADD INDEX idx_statistic_sources_completed_at (completed_at);

UPDATE complaint_statistic_sources
SET submitted_at = TIMESTAMP(statistic_date),
    due_at = DATE_ADD(TIMESTAMP(statistic_date), INTERVAL 7 DAY)
WHERE submitted_at IS NULL;
