CREATE TABLE complaint_categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_complaint_categories_code (category_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO complaint_categories (id, category_name, category_code, is_active, created_at, updated_at)
VALUES
    (1, '도로·교통', 'TRAFFIC', true, NOW(6), NOW(6)),
    (2, '환경', 'ENVIRONMENT', true, NOW(6), NOW(6)),
    (3, '건설·시설', 'FACILITY', true, NOW(6), NOW(6)),
    (4, '복지', 'WELFARE', true, NOW(6), NOW(6)),
    (5, '기타', 'ETC', true, NOW(6), NOW(6));
