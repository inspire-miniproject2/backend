CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    complaint_id BIGINT NOT NULL,
    complaint_no VARCHAR(32) NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    read_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_event_user_type (event_id, user_id, type),
    KEY idx_notifications_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE processed_events (
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
