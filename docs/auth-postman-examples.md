# Auth API Postman Examples

작성일: 2026-08-18

## 공통 안내
- Base URL: `http://localhost:8084/api/v1/auth`
- Content-Type: `application/json`
- 선택 헤더:
  - `X-Request-Id`

## 1. 회원가입

### Method / URL
- `POST http://localhost:8084/api/v1/auth/signup`

### Headers
- `Content-Type: application/json`
- `X-Request-Id: req-signup-citizen01`

### Body
```json
{
  "loginId": "citizen01",
  "password": "Civil!2026#",
  "name": "홍길동",
  "email": "citizen01@email.com",
  "phone": "010-1234-5678",
  "emailNotifyAgreed": true
}
```

### Success Response Example
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "loginId": "citizen01",
    "role": "CITIZEN",
    "createdAt": "2026-08-18T19:30:00"
  },
  "message": "회원가입이 완료되었습니다."
}
```

### 실패 포인트
- `loginId`: 영문/숫자 6~20자
- `password`: 10자 이상, 영문/숫자/특수문자 포함
- `phone`: `010-0000-0000`
- 중복 `loginId` 또는 `email`이면 `409 DUPLICATE_RESOURCE`

## 2. 로그인

### Method / URL
- `POST http://localhost:8084/api/v1/auth/login`

### Headers
- `Content-Type: application/json`
- `X-Request-Id: req-login-citizen01`

### Body
```json
{
  "loginId": "citizen01",
  "password": "Civil!2026#"
}
```

### Success Response Example
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "userId": 101,
      "loginId": "citizen01",
      "role": "CITIZEN",
      "departmentId": null
    }
  },
  "message": "로그인에 성공했습니다."
}
```

### 로컬 seed 계정 예시
- 공무원
```json
{
  "loginId": "officer01",
  "password": "Officer!2026#"
}
```

- 관리자
```json
{
  "loginId": "admin01",
  "password": "Admin!2026#"
}
```

## 3. 로그아웃

### Method / URL
- `POST http://localhost:8084/api/v1/auth/logout`

### Headers
- `X-Request-Id: req-logout-citizen01`

### Body
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### Success Response Example
```json
{
  "success": true,
  "data": {
    "loggedOut": true
  },
  "message": "로그아웃이 완료되었습니다."
}
```

## 4. Postman 테스트 순서
1. `POST /signup`으로 시민 계정 생성
2. `POST /login`으로 accessToken / refreshToken 확인
3. refreshToken을 Postman 환경변수 `refreshToken`에 저장
4. `POST /logout` body에 `{{refreshToken}}`를 넣어 호출

## 5. 자주 나오는 실패 응답
- `400 VALIDATION_ERROR`
  - 회원가입 필수값 누락
  - 형식 오류
  - 로그아웃 시 `refreshToken` 누락
- `401 INVALID_CREDENTIALS`
  - 로그인 아이디/비밀번호 불일치
- `401 INVALID_TOKEN`
  - 만료/변조 토큰
  - Access Token을 로그아웃 body에 넣은 경우
