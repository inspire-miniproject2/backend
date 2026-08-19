CREATE TABLE complaints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_no VARCHAR(32) NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    current_status VARCHAR(30) NOT NULL,
    assigned_department_id BIGINT NULL,
    assigned_officer_user_id BIGINT NULL,
    submitted_at DATETIME(6) NOT NULL,
    assigned_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_complaints_complaint_no (complaint_no),
    KEY idx_complaints_applicant (applicant_user_id),
    KEY idx_complaints_status (current_status),
    KEY idx_complaints_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE complaint_responses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    responder_user_id BIGINT NOT NULL,
    response_content TEXT NOT NULL,
    is_public BOOLEAN NOT NULL,
    responded_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_responses_complaint (complaint_id),
    CONSTRAINT fk_responses_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
