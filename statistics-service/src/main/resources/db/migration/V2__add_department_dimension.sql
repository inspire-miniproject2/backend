ALTER TABLE complaint_statistics
    DROP PRIMARY KEY,
    ADD COLUMN department_id BIGINT NOT NULL DEFAULT 0 AFTER statistic_date,
    ADD PRIMARY KEY (statistic_date, department_id, category_code, status);

ALTER TABLE complaint_statistic_sources
    ADD COLUMN assigned_department_id BIGINT NOT NULL DEFAULT 0 AFTER statistic_date;
