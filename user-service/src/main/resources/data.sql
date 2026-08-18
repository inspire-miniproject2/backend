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
    (201, 'officer01', '{noop}Officer!2026#', '김담당', 'officer01@gcivil.local', '010-1111-1111', 'OFFICER', 10, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (202, 'officer02', '{noop}Officer!2026#', '이담당', 'officer02@gcivil.local', '010-2222-2222', 'OFFICER', 20, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (900, 'admin01', '{noop}Admin!2026#', '관리자', 'admin01@gcivil.local', '010-9999-9999', 'ADMIN', NULL, FALSE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
