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
    (201, 'officer01', '{noop}Officer!2026#', '김담당', 'officer01@gcivil.local', '010-1111-1111', 'OFFICER', 10, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (202, 'officer02', '{noop}Officer!2026#', '이담당', 'officer02@gcivil.local', '010-2222-2222', 'OFFICER', 20, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (203, 'officer03', '{noop}Officer!2026#', '박담당', 'officer03@gcivil.local', '010-3333-3333', 'OFFICER', 30, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (204, 'officer04', '{noop}Officer!2026#', '최담당', 'officer04@gcivil.local', '010-4444-4444', 'OFFICER', 40, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (205, 'officer05', '{noop}Officer!2026#', '정담당', 'officer05@gcivil.local', '010-5555-5555', 'OFFICER', 50, TRUE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (900, 'admin01', '{noop}Admin!2026#', '관리자', 'admin01@gcivil.local', '010-9999-9999', 'ADMIN', NULL, FALSE, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
