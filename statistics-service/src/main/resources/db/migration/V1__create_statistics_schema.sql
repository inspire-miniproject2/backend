CREATE TABLE complaint_statistics (
    statistic_date DATE NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    complaint_count BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (statistic_date, category_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE complaint_statistic_sources (
    complaint_id BIGINT NOT NULL,
    statistic_date DATE NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    current_status VARCHAR(30) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (complaint_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE processed_events (
    event_id CHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
