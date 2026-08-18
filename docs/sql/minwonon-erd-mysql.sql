-- MinwonON ERD DDL
-- 기준일: 2026-08-18
-- 기준 문서:
-- 1) docs/contracts/backend-a-domain-contract-freeze.md
-- 2) docs/api-spec.md
--
-- DB 엔진: MySQL 8.0+
-- 원칙:
-- - 서비스별 DB 분리
-- - 같은 서비스 내부에서만 FK 사용
-- - 다른 서비스 엔티티는 ID 논리 참조만 사용
-- - 첨부파일 원본은 S3, DB에는 메타데이터만 저장

-- =====================================================================
-- 0. DATABASE 생성
-- =====================================================================

CREATE DATABASE IF NOT EXISTS user_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS assignment_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS complaint_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS notification_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS statistics_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- 1. user_db
-- =====================================================================

USE user_db;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    role VARCHAR(20) NOT NULL,
    department_id BIGINT NULL,
    email_notify_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_login_id (login_id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_department_id (department_id),
    KEY idx_users_role (role),
    CONSTRAINT chk_users_role
        CHECK (role IN ('CITIZEN', 'OFFICER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 2. assignment_db
-- =====================================================================

USE assignment_db;

CREATE TABLE IF NOT EXISTS departments (
    department_id BIGINT NOT NULL AUTO_INCREMENT,
    department_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_complaint_categories_category_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department_categories (
    department_category_id BIGINT NOT NULL AUTO_INCREMENT,
    department_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    officer_user_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (department_category_id),
    UNIQUE KEY uq_department_categories_mapping (department_id, category_id, officer_user_id),
    KEY idx_department_categories_department_id (department_id),
    KEY idx_department_categories_category_id (category_id),
    KEY idx_department_categories_officer_user_id (officer_user_id),
    CONSTRAINT fk_department_categories_department
        FOREIGN KEY (department_id) REFERENCES departments (department_id),
    CONSTRAINT fk_department_categories_category
        FOREIGN KEY (category_id) REFERENCES complaint_categories (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 3. complaint_db
-- =====================================================================

USE complaint_db;

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_no VARCHAR(50) NOT NULL,
    applicant_user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    assigned_department_id BIGINT NULL,
    assigned_officer_user_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    current_status VARCHAR(20) NOT NULL,
    submitted_at DATETIME NOT NULL,
    assigned_at DATETIME NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (complaint_id),
    UNIQUE KEY uq_complaints_complaint_no (complaint_no),
    KEY idx_complaints_applicant_user_id (applicant_user_id),
    KEY idx_complaints_category_id (category_id),
    KEY idx_complaints_assigned_department_id (assigned_department_id),
    KEY idx_complaints_assigned_officer_user_id (assigned_officer_user_id),
    KEY idx_complaints_current_status (current_status),
    KEY idx_complaints_submitted_at (submitted_at),
    CONSTRAINT chk_complaints_current_status
        CHECK (current_status IN ('RECEIVED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_attachments (
    attachment_id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id),
    KEY idx_complaint_attachments_complaint_id (complaint_id),
    CONSTRAINT fk_complaint_attachments_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_status_history (
    status_history_id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,
    changed_by_user_id BIGINT NULL,
    change_memo VARCHAR(255) NULL,
    changed_at DATETIME NOT NULL,
    PRIMARY KEY (status_history_id),
    KEY idx_complaint_status_history_complaint_id (complaint_id),
    KEY idx_complaint_status_history_changed_at (changed_at),
    CONSTRAINT fk_complaint_status_history_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT chk_complaint_status_history_previous_status
        CHECK (
            previous_status IS NULL
            OR previous_status IN ('RECEIVED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED')
        ),
    CONSTRAINT chk_complaint_status_history_new_status
        CHECK (new_status IN ('RECEIVED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS complaint_responses (
    response_id BIGINT NOT NULL AUTO_INCREMENT,
    complaint_id BIGINT NOT NULL,
    responder_user_id BIGINT NOT NULL,
    response_content TEXT NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    responded_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (response_id),
    KEY idx_complaint_responses_complaint_id (complaint_id),
    KEY idx_complaint_responses_is_public (is_public),
    KEY idx_complaint_responses_responded_at (responded_at),
    CONSTRAINT fk_complaint_responses_complaint
        FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 4. notification_db
-- =====================================================================

USE notification_db;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_user_id BIGINT NOT NULL,
    complaint_id BIGINT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    KEY idx_notifications_receiver_user_id (receiver_user_id),
    KEY idx_notifications_complaint_id (complaint_id),
    KEY idx_notifications_is_read (is_read),
    KEY idx_notifications_created_at (created_at),
    CONSTRAINT chk_notifications_notification_type
        CHECK (notification_type IN ('ASSIGNED', 'STATUS_CHANGED', 'RESPONSE_REGISTERED')),
    CONSTRAINT chk_notifications_channel
        CHECK (channel IN ('IN_APP', 'EMAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 5. statistics_db
-- =====================================================================

USE statistics_db;

CREATE TABLE IF NOT EXISTS daily_complaint_statistics (
    statistics_id BIGINT NOT NULL AUTO_INCREMENT,
    statistics_date DATE NOT NULL,
    department_id BIGINT NOT NULL,
    received_count BIGINT NOT NULL DEFAULT 0,
    assigned_count BIGINT NOT NULL DEFAULT 0,
    in_progress_count BIGINT NOT NULL DEFAULT 0,
    completed_count BIGINT NOT NULL DEFAULT 0,
    avg_processing_time DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (statistics_id),
    UNIQUE KEY uq_daily_complaint_statistics (statistics_date, department_id),
    KEY idx_daily_complaint_statistics_date (statistics_date),
    KEY idx_daily_complaint_statistics_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================================
-- 6. 참고 메모
-- =====================================================================
-- 1) complaint_db.category_id 는 assignment_db.complaint_categories.category_id 를 논리 참조합니다.
-- 2) complaint_db.assigned_department_id 는 assignment_db.departments.department_id 를 논리 참조합니다.
-- 3) complaint_db.applicant_user_id, assigned_officer_user_id, responder_user_id 는 user_db.users.user_id 를 논리 참조합니다.
-- 4) notification_db.receiver_user_id 도 user_db.users.user_id 를 논리 참조합니다.
-- 5) statistics_db.department_id 는 assignment_db.departments.department_id 를 논리 참조합니다.
