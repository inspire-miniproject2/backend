ALTER TABLE complaints
    ADD COLUMN title VARCHAR(100) NULL AFTER category_code,
    ADD COLUMN content TEXT NULL AFTER title,
    CHANGE COLUMN status current_status VARCHAR(30) NOT NULL,
    ADD COLUMN completed_at DATETIME(6) NULL AFTER assigned_at,
    ADD COLUMN created_at DATETIME(6) NULL AFTER completed_at,
    ADD KEY idx_complaints_category (category_id);

UPDATE complaints
SET title = complaint_no,
    content = '',
    created_at = submitted_at
WHERE title IS NULL
   OR content IS NULL
   OR created_at IS NULL;

ALTER TABLE complaints
    MODIFY COLUMN title VARCHAR(100) NOT NULL,
    MODIFY COLUMN content TEXT NOT NULL,
    MODIFY COLUMN created_at DATETIME(6) NOT NULL;

CREATE TABLE complaint_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_attachments_complaint (complaint_id),
    CONSTRAINT fk_attachments_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NULL,
    change_memo VARCHAR(255) NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_status_history_complaint (complaint_id),
    KEY idx_status_history_changed_at (changed_at),
    CONSTRAINT fk_status_history_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE complaint_responses
    CHANGE COLUMN content response_content TEXT NOT NULL,
    ADD COLUMN updated_at DATETIME(6) NULL AFTER responded_at;

UPDATE complaint_responses
SET updated_at = responded_at
WHERE updated_at IS NULL;

ALTER TABLE complaint_responses
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL;
