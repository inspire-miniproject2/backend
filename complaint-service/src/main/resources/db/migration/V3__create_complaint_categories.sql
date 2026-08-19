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

INSERT INTO complaint_categories (category_name, category_code, is_active, created_at, updated_at)
VALUES
    ('도로·교통', 'TRAFFIC', true, NOW(6), NOW(6)),
    ('환경', 'ENVIRONMENT', true, NOW(6), NOW(6)),
    ('건설·시설', 'FACILITY', true, NOW(6), NOW(6)),
    ('복지', 'WELFARE', true, NOW(6), NOW(6)),
    ('기타', 'ETC', false, NOW(6), NOW(6));
