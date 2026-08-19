INSERT INTO departments (department_id, department_name, is_active, created_at, updated_at)
VALUES
    (10, '교통정책과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (20, '도로관리과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (30, '환경관리과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (40, '복지지원과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (50, '민원총괄과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO complaint_categories (category_id, category_name, category_code, created_at, updated_at)
VALUES
    (1, '도로·교통', 'TRAFFIC', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (2, '환경', 'ENVIRONMENT', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (3, '건설·시설', 'FACILITY', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (4, '복지', 'WELFARE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (5, '기타', 'ETC', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

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
    (31, 10, 1, 201, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (32, 20, 3, 202, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (33, 30, 2, 203, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (34, 40, 4, 204, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (35, 50, 5, 205, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
