-- V2 was already applied to some development databases when only two
-- officers existed. Add the officers referenced by the remaining assignment
-- rules while keeping this migration safe for newly created databases.

INSERT INTO users (
    user_id,
    login_id,
    password_hash,
    name,
    email,
    phone,
    role,
    department_id,
    email_notify_agreed,
    is_active,
    created_at,
    updated_at
)
VALUES
    (203, 'officer03', '{noop}Officer!2026#', '박담당', 'officer03@gcivil.local', '010-3333-3333', 'OFFICER', 30, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (204, 'officer04', '{noop}Officer!2026#', '최담당', 'officer04@gcivil.local', '010-4444-4444', 'OFFICER', 40, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (205, 'officer05', '{noop}Officer!2026#', '정담당', 'officer05@gcivil.local', '010-5555-5555', 'OFFICER', 50, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    login_id = VALUES(login_id),
    password_hash = VALUES(password_hash),
    name = VALUES(name),
    email = VALUES(email),
    phone = VALUES(phone),
    role = VALUES(role),
    department_id = VALUES(department_id),
    email_notify_agreed = VALUES(email_notify_agreed),
    is_active = VALUES(is_active),
    updated_at = VALUES(updated_at);
