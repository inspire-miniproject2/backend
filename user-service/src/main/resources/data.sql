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
    is_active
)
VALUES
    (201, 'officer01', 'hashed-password-1', '김담당', 'officer01@gcivil.local', '010-1111-1111', 'OFFICER', 10, TRUE, TRUE),
    (202, 'officer02', 'hashed-password-2', '이담당', 'officer02@gcivil.local', '010-2222-2222', 'OFFICER', 20, TRUE, TRUE),
    (900, 'admin01', 'hashed-password-3', '관리자', 'admin01@gcivil.local', '010-9999-9999', 'ADMIN', NULL, FALSE, TRUE);
