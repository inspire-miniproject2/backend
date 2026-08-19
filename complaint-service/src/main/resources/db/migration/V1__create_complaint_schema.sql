CREATE TABLE complaints (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_no VARCHAR(32) NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    assigned_department_id BIGINT NULL,
    assigned_officer_user_id BIGINT NULL,
    submitted_at DATETIME(6) NOT NULL,
    assigned_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_complaints_complaint_no (complaint_no),
    KEY idx_complaints_applicant (applicant_user_id),
    KEY idx_complaints_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_responses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    responder_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_public BOOLEAN NOT NULL,
    responded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_responses_complaint (complaint_id),
    CONSTRAINT fk_responses_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
