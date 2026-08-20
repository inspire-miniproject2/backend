-- V2 was already applied to some development databases when only two
-- assignment rules existed. Complete the remaining category mappings without
-- changing the checksum of the applied migration.

INSERT INTO departments (department_id, department_name, is_active, created_at, updated_at)
VALUES
    (30, '환경관리과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (40, '복지지원과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (50, '민원총괄과', TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    department_name = VALUES(department_name),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);

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
    (33, 30, 2, 203, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (34, 40, 4, 204, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (35, 50, 5, 205, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    category_id = VALUES(category_id),
    officer_user_id = VALUES(officer_user_id),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);
