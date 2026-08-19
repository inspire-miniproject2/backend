INSERT INTO departments (department_id, department_name, is_active, created_at, updated_at)
VALUES
    (10, '교통정책과', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (20, '도로관리과', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO complaint_categories (category_id, category_name, category_code, created_at, updated_at)
VALUES
    (1, '도로·교통', 'TRAFFIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '환경', 'ENVIRONMENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, '건설·시설', 'FACILITY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, '복지', 'WELFARE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, '기타', 'ETC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO department_categories (
    department_category_id,
    department_id,
    category_id,
    officer_user_id,
    is_active,
    created_at,
    updated_at
)
VALUES
    (31, 10, 1, 201, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (32, 20, 3, 202, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
