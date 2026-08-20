# MinwonON API 명세서

최종 ERD 및 와이어프레임 반영본 · 첨부파일 포함 · 보완 요청/답변 마감기한 제외

| 항목 | 내용 |
|---|---|
| **문서 버전** | v0.3 (최종 ERD/와이어프레임 반영 DOCX) |
| **작성 기준일** | 2026.08.14 |
| **Base URL** | `/api/v1` |
| **인증 방식** | JWT Bearer Token |
| **대상 구조** | user-service, assignment-service, complaint-service, notification-service, statistics-service |
| **반영 자료** | 최종 ERD 이미지, Wireframe |

> **참고:** 이번 버전에서는 다음 요소를 API 범위에서 제외합니다:
> 1) 민원 답변 페이지의 보완요청편집, 제출기한, 요청파일, 내부 메모 컨테이너
> 2) 답변 마감기한.

## 문서 범위
* 인증·사용자, 민원 신청/조회, 첨부파일, 공개 답변, 공무원 처리, 알림, 관리자 통계 API의 MVP 계약을 정의합니다.
* 공개 회원가입은 CITIZEN만 허용하며, OFFICER와 ADMIN 계정은 내부 운영 절차로 생성합니다.
* 민원 본문 첨부파일은 complaint-service의 COMPLAINT_ATTACHMENT 메타데이터로 관리합니다.
* 보완 요청 편집, 요청 파일 제출, 내부 메모, 답변 마감기한 관련 필드는 본 명세에서 정의하지 않습니다.

---

## 1. 공통 규칙

### 1.1 요청 기본값
| 항목 | 규칙 |
|---|---|
| **Base URL** | `/api/v1` |
| **기본 Content-Type** | `application/json; charset=UTF-8` |
| **파일 업로드** | `multipart/form-data` 사용 |
| **인증 헤더** | `Authorization: Bearer {accessToken}` |
| **시간 형식** | ISO 8601, Asia/Seoul 예: `2026-08-14T10:30:00+09:00` |
| **ID 타입** | long 정수 |
| **요청 추적** | Gateway가 `X-Request-Id`를 생성·전달하며 오류 응답에도 포함 |

### 1.1.1 JWT Payload 규약
Access Token은 아래 최소 클레임을 포함합니다.

| Claim | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sub` | string | Y | 사용자 식별자 문자열, `userId`와 동일 의미 |
| `userId` | long | Y | 내부 사용자 PK |
| `loginId` | string | Y | 로그인 ID |
| `role` | string | Y | `CITIZEN`, `OFFICER`, `ADMIN` |
| `departmentId` | long\|null | Y | 공무원 소속 부서 ID, 민원인은 null |
| `tokenType` | string | Y | `ACCESS` |
| `iat` | epoch-seconds | Y | 발급 시각 |
| `exp` | epoch-seconds | Y | 만료 시각 |
| `iss` | string | Y | 발급자 식별자, `minwonon-auth` |

Refresh Token은 아래 최소 클레임을 포함합니다.

| Claim | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sub` | string | Y | 사용자 식별자 문자열 |
| `userId` | long | Y | 내부 사용자 PK |
| `tokenType` | string | Y | `REFRESH` |
| `iat` | epoch-seconds | Y | 발급 시각 |
| `exp` | epoch-seconds | Y | 만료 시각 |
| `iss` | string | Y | 발급자 식별자, `minwonon-auth` |

### 1.1.2 Gateway 내부 전달 헤더 규약
API Gateway는 외부 인입 요청의 JWT를 검증한 뒤, 외부에서 들어온 동일 이름 헤더를 제거하고 아래 헤더를 내부 서비스로 전달합니다. 각 마이크로서비스는 JWT를 재검증하지 않고 Gateway가 전달한 사용자 메타데이터와 내부 비즈니스 데이터를 결합해 접근 권한을 판단합니다.

| Header | 타입 | 필수 | 설명 |
|---|---|---|---|
| `X-User-Id` | string | Y | JWT의 `userId` |
| `X-Login-Id` | string | Y | JWT의 `loginId` |
| `X-User-Role` | string | Y | JWT의 `role` |
| `X-Department-Id` | string | N | JWT의 `departmentId`, 값이 없으면 헤더 미전달 |
| `X-Request-Id` | string | Y | Gateway 생성 요청 추적 ID |

### 1.1.3 JWT 서명 및 검증 계약

| 항목 | 확정값 |
|---|---|
| 서명 알고리즘 | `HS256`만 허용 |
| 서명 키 | `JWT_SECRET` 환경변수의 UTF-8 바이트 |
| 키 요구사항 | 암호학적으로 안전한 임의 문자열, UTF-8 기준 최소 32바이트 |
| 발급자 | `JWT_ISSUER`, 기본값 `minwonon-auth` |
| Access Token 유효시간 | `JWT_ACCESS_TOKEN_TTL_SECONDS`, 기본값 3,600초 |
| Refresh Token 유효시간 | `JWT_REFRESH_TOKEN_TTL_SECONDS`, 기본값 604,800초 |
| 허용 시간 오차 | 최대 30초 |

- User Service와 Gateway는 동일한 `JWT_SECRET` 및 `JWT_ISSUER`를 배포 환경의 Secret으로 주입받습니다. 실제 키를 저장소나 문서에 기록하지 않습니다.
- `JWT_SECRET`을 Base64로 재해석하지 않고 환경변수 문자열의 UTF-8 바이트를 그대로 사용합니다.
- Gateway는 JWT 헤더의 `alg`가 `HS256`이 아니면 검증 전에 거부합니다.
- Gateway는 서명, `iss`, `iat`, `exp`, `tokenType` 및 Access Token 필수 claim을 검증합니다.
- API 요청에는 `tokenType=ACCESS`만 허용하며 Refresh Token이 전달되면 `INVALID_TOKEN`으로 응답합니다.
- 인증이 필요 없는 경로는 `/api/v1/auth/signup`, `/api/v1/auth/login`, `/api/v1/complaint-categories`, `/api/v1/public-responses/**`입니다. 운영 health 경로의 공개 범위는 배포 정책에서 별도로 제한합니다.

### 1.1.4 Gateway CORS 계약

| 항목 | 로컬 기본값 |
|---|---|
| 허용 Origin | `http://localhost:5173` |
| 환경변수 | `CORS_ALLOWED_ORIGINS` (쉼표로 복수 Origin 구분) |
| 허용 Method | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| 허용 요청 Header | `Authorization`, `Content-Type`, `Accept`, `X-Request-Id` |
| 노출 응답 Header | `X-Request-Id` |
| Credential | `false` |

- Origin에 와일드카드(`*`)를 사용하지 않습니다.
- 개발 프론트엔드 배포 주소가 확정되면 코드 변경 없이 `CORS_ALLOWED_ORIGINS`에 추가합니다.
- 외부 클라이언트가 보낸 `X-User-Id`, `X-Login-Id`, `X-User-Role`, `X-Department-Id`는 CORS 허용 Header에 포함하지 않으며 Gateway가 제거 후 재생성합니다.

### 1.2 공통 성공 응답
```json
{
  "success": true,
  "data": {},
  "message": "요청이 성공했습니다."
}
```

### 1.3 공통 실패 응답
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "로그인이 필요합니다.",
    "details": null
  },
  "requestId": "req-20260814-0001"
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `success` | boolean | 항상 false |
| `error.code` | string | 표준 에러 코드 |
| `error.message` | string | 사용자 또는 호출자에게 전달할 메시지 |
| `error.details` | object\|null | 검증 오류 또는 문맥 정보 |
| `requestId` | string | 요청 추적 ID |

### 1.4 공통 Enum
| 구분 | 허용 값 | 설명 |
|---|---|---|
| **Role** | CITIZEN, OFFICER, ADMIN | 민원인, 공무원, 관리자 |
| **ComplaintStatus** | RECEIVED, ASSIGNED, IN_PROGRESS, COMPLETED | 접수 → 배정 → 처리중 → 완료 |
| **NotificationType** | ASSIGNED, STATUS_CHANGED, RESPONSE_REGISTERED | 배정, 상태 변경, 답변 등록 알림 |
| **NotificationChannel** | IN_APP, EMAIL | 알림센터, 이메일 |

### 1.5 에러 코드
| error.code | HTTP | 설명 |
|---|---|---|
| INVALID_REQUEST | 400 | 잘못된 요청 형식 |
| VALIDATION_ERROR | 400 | 필수값 누락 또는 요청값 검증 실패 |
| UNAUTHORIZED | 401 | 인증 필요 |
| INVALID_CREDENTIALS | 401 | 아이디 또는 비밀번호 불일치 |
| INVALID_TOKEN | 401 | 유효하지 않은 토큰 |
| TOKEN_EXPIRED | 401 | 만료된 토큰 |
| FORBIDDEN | 403 | 역할 또는 리소스 접근 권한 없음 |
| RESOURCE_NOT_FOUND | 404 | 리소스를 찾을 수 없음 |
| DUPLICATE_RESOURCE | 409 | 중복 리소스 |
| INVALID_STATUS_TRANSITION | 409 | 허용되지 않은 상태 전이 |
| SERVICE_UNAVAILABLE | 503 | 하위 서비스 장애 또는 시간 초과 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류 |

### 1.6 상태 머신 공통 규칙
- `complaint-service`가 민원 상태의 source of truth입니다.
- 허용 상태 전이는 `RECEIVED -> ASSIGNED -> IN_PROGRESS -> COMPLETED` 입니다.
- 자동 배정 실패 또는 타임아웃 시 민원은 삭제하지 않고 `RECEIVED` 상태를 유지합니다.
- `COMPLETED` 전이는 반드시 공식 답변(`RESPONSE`)이 먼저 생성되어 있어야만 허용합니다.
- 상태 변경 이력(`COMPLAINT_STATUS_HISTORY`)은 모든 전이에 대해 남기며, 자동 배정으로 인한 전이는 `changedByUserId=null`을 허용합니다.

---

## 2. Auth API
로그인 API는 와이어프레임 기준으로 아이디(loginId) + 비밀번호 조합을 사용합니다. 일반 회원가입으로는 CITIZEN만 생성할 수 있습니다.

### 2.1 회원가입
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/auth/signup` | 없음 | Public | 201 Created |

민원인 계정을 생성합니다. 회원가입 화면의 아이디, 비밀번호, 이름, 이메일, 휴대전화번호, 이메일 알림 동의를 기준으로 합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| loginId | string | Y | 회원 아이디 | 영문·숫자 6~20자 |
| password | string | Y | 비밀번호 | 10자 이상, 영문·숫자·특수문자 포함 |
| name | string | Y | 회원 이름 | 2~50자 |
| email | string | Y | 이메일 | 이메일 형식 |
| phone | string | Y | 휴대전화번호 | 010-0000-0000 형식 |
| emailNotifyAgreed | boolean | N | 이메일 추가 알림 동의 | 미입력 시 false; IN_APP 알림과 무관 |

**Request Example**
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

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.userId | long | 생성된 사용자 ID |
| data.loginId | string | 생성된 로그인 아이디 |
| data.role | string | 항상 CITIZEN |
| data.createdAt | datetime | 가입 시각 |

**Success Example**
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

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 필수값 누락 또는 형식 오류 |
| 409 | DUPLICATE_RESOURCE | 이미 사용 중인 loginId 또는 email |

### 2.2 로그인
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | 없음 | Public | 200 OK |

아이디와 비밀번호를 검증하고 Access/Refresh Token을 발급합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| loginId | string | Y | 로그인 아이디 | 공백 불가 |
| password | string | Y | 비밀번호 | 공백 불가 |

**Request Example**
```json
{
  "loginId": "citizen01",
  "password": "Civil!2026#"
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.accessToken | string | API 인증용 JWT |
| data.refreshToken | string | 토큰 재발급용 JWT |
| data.user.departmentId | long\|null | 공무원이면 부서 ID, 민원인은 null |

**Success Example**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
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

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 아이디 또는 비밀번호 누락 |
| 401 | INVALID_CREDENTIALS | 로그인 정보 불일치 |

### 2.3 로그아웃
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/auth/logout` | 없음 | 로그인 사용자 | 200 OK |

현재 사용자의 세션을 종료합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| refreshToken | string | Y | 폐기할 Refresh Token | 본인 소유 토큰 |

**Request Example**
```json
{
  "refreshToken": "eyJhbGciOi..."
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.loggedOut | boolean | 로그아웃 성공 여부 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "loggedOut": true
  },
  "message": "로그아웃이 완료되었습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | refreshToken 누락 |
| 401 | INVALID_TOKEN | 유효하지 않은 토큰 |

---

## 3. Complaint API
민원 신청 플로우는 1) 신청서 작성 2) 내용 확인 3) 접수 완료 화면을 기준으로 하며, 첨부파일은 complaint-service의 COMPLAINT_ATTACHMENT에 메타데이터로 저장됩니다.

### 3.1 민원 카테고리 목록 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/complaint-categories` | 없음 | Public | 200 OK |

민원 신청 화면의 카테고리 선택 목록을 조회합니다.

최종 고정 카테고리 기준은 아래와 같습니다.
- `1 TRAFFIC` = `도로·교통`
- `2 ENVIRONMENT` = `환경`
- `3 FACILITY` = `건설·시설`
- `4 WELFARE` = `복지`
- `5 ETC` = `기타`

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| activeOnly | boolean | N | 활성 카테고리만 조회 | 기본값 true |

**Request Example**
```json
{
  "activeOnly": true
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data[].categoryId | long | 카테고리 ID |
| data[].categoryName | string | 카테고리명 |
| data[].categoryCode | string | 카테고리 코드 |

**Success Example**
```json
{
  "success": true,
  "data": [
    { "categoryId": 1, "categoryName": "도로·교통", "categoryCode": "TRAFFIC" },
    { "categoryId": 2, "categoryName": "환경", "categoryCode": "ENVIRONMENT" },
    { "categoryId": 3, "categoryName": "건설·시설", "categoryCode": "FACILITY" },
    { "categoryId": 4, "categoryName": "복지", "categoryCode": "WELFARE" },
    { "categoryId": 5, "categoryName": "기타", "categoryCode": "ETC" }
  ],
  "message": "카테고리 목록을 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 500 | INTERNAL_SERVER_ERROR | 카테고리 조회 실패 |

### 3.2 민원 접수
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/complaints` | Bearer | CITIZEN | 201 Created |

민원 데이터는 최초 RECEIVED 상태로 영속화되며, OpenFeign을 통해 Assignment Service를 호출하여 카테고리 코드 기반의 자동 배정 로직을 수행합니다. 정상 배정 시 ASSIGNED 상태로 전이되나, 배정 서비스 장애 혹은 타임아웃 시에도 데이터 보존을 위해 RECEIVED 상태를 유지합니다. 첨부파일 포함 시에는 `multipart/form-data` 프로토콜을 준수합니다. 알림 서비스는 IN_APP 채널 생성을 원칙으로 하되, EMAIL 채널은 이용자 선택 및 수신 동의 여부를 식별하여 선별적으로 전송합니다.

현재 초기 자동 배정 규칙은 아래 5건을 활성화합니다.
- `assignmentRuleId=31`: `TRAFFIC(1)` -> `departmentId=10` `교통정책과` -> `officerUserId=201`
- `assignmentRuleId=32`: `FACILITY(3)` -> `departmentId=20` `도로관리과` -> `officerUserId=202`
- `assignmentRuleId=33`: `ENVIRONMENT(2)` -> `departmentId=30` `환경관리과` -> `officerUserId=203`
- `assignmentRuleId=34`: `WELFARE(4)` -> `departmentId=40` `복지지원과` -> `officerUserId=204`
- `assignmentRuleId=35`: `ETC(5)` -> `departmentId=50` `민원총괄과` -> `officerUserId=205`

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| categoryId | long | Y | 민원 카테고리 ID | 활성 카테고리만 허용 |
| title | string | Y | 민원 제목 | 5~100자 |
| content | string | Y | 민원 내용 | 20~3000자 |
| attachmentFiles[] | file[] | N | 첨부파일 | 허용 확장자/용량 정책 적용 |
| notifyChannels[] | string[] | N | 추가 알림 채널 | IN_APP은 항상 생성되며, EMAIL만 선택 가능 |

**Request Example**
```text
multipart/form-data
- categoryId: 1
- title: 어린이보호구역 신호시간 조정 요청
- content: 출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.
- attachmentFiles[0]: 현장사진.jpg
- notifyChannels[0]: EMAIL 
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.complaintId | long | 생성된 민원 ID |
| data.complaintNo | string | 민원 번호 |
| data.currentStatus | string | ASSIGNED: 자동 배정 성공 / RECEIVED: 접수 성공, 자동 배정 대기 |
| data.submittedAt | datetime | 접수 시각 |
| data.assignedDepartmentId | long\|null | 배정 부서 ID, 미배정시 null |
| data.assignedOfficerUserId | long\|null | 배정 공무원 ID, 미배정 시 null |

**Success Example**
```json
{
  "success": true,
  "data": {
    "complaintId": 5001,
    "complaintNo": "CIV-2026-000184",
    "categoryCode": "TRAFFIC",
    "currentStatus": "ASSIGNED",
    "assignedDepartmentId": 10,
    "assignedOfficerUserId": 201,
    "submittedAt": "2026-08-14T14:42:00+09:00"
  },
  "message": "민원이 접수되고 담당 부서 및 공무원에게 배정되었습니다."
}
```
> ※ Assignment Service 장애/시간 초과 시에도 201 Created를 반환하며 `currentStatus=RECEIVED`, `assignedDepartmentId=null`, `assignedOfficerUserId=null` 상태로 민원을 유지합니다.

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 제목/내용/파일 형식 오류 |

### 3.3 내 민원 목록 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/complaints/my` | Bearer | CITIZEN | 200 OK |

내 민원 목록 화면의 상태별 카드와 민원 목록을 조회합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| page | number | N | 페이지 번호 | 기본값 0 |
| size | number | N | 페이지 크기 | 기본값 20 |
| keyword | string | N | 민원 제목/번호 검색어 | 최대 50자 |
| categoryCode | String | N | 민원 분야 코드 필터 | 활성 CategoryCode |
| status | string | N | 처리 상태 필터 | ComplaintStatus 중 1개 |

**Request Example**
```json
{
  "page": 0,
  "size": 20,
  "keyword": "신호시간",
  "categoryCode": "TRAFFIC",
  "status": "IN_PROGRESS"
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.summary.total | number | 전체 건수 |
| data.summary.received | number | 접수됨 건수 |
| data.content[].complaintNo | string | 민원 번호 |
| data.content[].currentStatus | string | 현재 상태 |
| data.content[].assignedDepartmentName | string | 담당 부서명 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "summary": {
      "total": 24,
      "received": 3,
      "assigned": 4,
      "inProgress": 7
    },
    "content": [
      {
        "complaintNo": "CIV-2026-000184",
        "title": "어린이보호구역 신호시간 조정 요청",
        "currentStatus": "IN_PROGRESS",
        "assignedDepartmentName": "교통정책과"
      }
    ]
  },
  "message": "내 민원 목록을 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 401 | UNAUTHORIZED | 민원인 로그인 필요 |

### 3.4 민원 상세 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/complaints/{complaintId}` | Bearer | CITIZEN / OFFICER / ADMIN | 200 OK |

민원 상세 화면의 신청 정보, 본문, 첨부파일, 처리 이력을 조회합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| complaintId | path(long) | Y | 민원 ID | 본인 또는 권한 있는 사용자만 조회 |

**Request Example**
```json
{
  "complaintId": 5001
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.complaintNo | string | 민원 번호 |
| data.currentStatus | string | 현재 상태 |
| data.attachments[] | array | 첨부파일 메타데이터 목록 |
| data.statusHistories[] | array | 처리 이력 목록 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "complaintNo": "CIV-2026-000184",
    "currentStatus": "IN_PROGRESS",
    "attachments": [
      {
        "attachmentId": 8101,
        "originalFilename": "현장사진.jpg"
      }
    ],
    "statusHistories": [
      {
        "previousStatus": "RECEIVED",
        "newStatus": "ASSIGNED",
        "changedAt": "2026-08-14T14:43:00+09:00"
      }
    ]
  },
  "message": "민원 상세를 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 403 | FORBIDDEN | 본인 민원이 아니고 권한도 없음 |
| 404 | RESOURCE_NOT_FOUND | 존재하지 않는 민원 |

### 3.5 첨부파일 다운로드
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/complaints/{complaintId}/attachments/{attachmentId}` | Bearer | CITIZEN / OFFICER / ADMIN | 200 OK |

민원 상세 화면의 첨부파일 다운로드 링크를 처리합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| complaintId | path(long) | Y | 민원 ID | 본인 또는 권한 있는 사용자만 가능 |
| attachmentId | path(long) | Y | 첨부파일 ID | 해당 민원 소속 파일 |

**Request Example**
```json
{
  "complaintId": 5001,
  "attachmentId": 8101
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| Content-Disposition | header | attachment; filename=... |
| binary | file | 실제 파일 바이트 스트림 |

**Success Example**
```text
파일 다운로드 응답
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 403 | FORBIDDEN | 다운로드 권한 없음 |
| 404 | RESOURCE_NOT_FOUND | 첨부파일 없음 |

---

## 4. Public Response API
공개 답변 화면은 민원 본문을 숨기고 민원 제목, 분야, 신청일, 답변 부서, 완료일, 공식 답변, 공개 첨부파일만 노출합니다. 답변 마감기한은 정의하지 않습니다.

### 4.1 공개 민원 답변 목록 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/public-responses` | 없음 | Public | 200 OK |

공개 답변 목록 화면의 키워드, 민원 분야, 완료 기간 필터를 지원합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| keyword | string | N | 민원 제목 또는 답변 내용 검색어 | 최대 50자 |
| categoryCode | String | N | 민원 분야 코드 필터 | 활성 카테고리 코드 |
| completedFrom | date | N | 완료 시작일 | yyyy-MM-dd |
| completedTo | date | N | 완료 종료일 | yyyy-MM-dd |

**Request Example**
```json
{
  "keyword": "신호시간",
  "categoryCode": "TRAFFIC",
  "completedFrom": "2026-08-01",
  "completedTo": "2026-08-14"
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.content[].title | string | 공개된 민원 제목 |
| data.content[].departmentName | string | 답변 부서명 |
| data.content[].completedAt | date | 완료일 |
| data.content[].statusLabel | string | 항상 답변 완료 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "responseId": 7001,
        "title": "어린이보호구역 신호시간 조정 요청 처리 결과",
        "departmentName": "교통정책과",
        "completedAt": "2026-08-13",
        "statusLabel": "답변 완료"
      }
    ]
  },
  "message": "공개 답변 목록을 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 500 | INTERNAL_SERVER_ERROR | 공개 답변 목록 조회 실패 |

### 4.2 공개 민원 답변 상세 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/public-responses/{responseId}` | 없음 | Public | 200 OK |

공개 답변 상세 화면의 민원 사례, 공식 답변, 공개 첨부파일을 조회합니다. 민원 본문은 반환하지 않습니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| responseId | path(long) | Y | 답변 ID | 공개 상태인 답변만 가능 |

**Request Example**
```json
{
  "responseId": 7001
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.caseTitle | string | 민원 제목 |
| data.categoryName | string | 민원 분야 |
| data.departmentName | string | 답변 부서 |
| data.responseContent | string | 공식 답변 본문 |
| data.attachments[] | array | 공개 가능한 첨부파일 목록 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "caseTitle": "어린이보호구역 신호시간 조정 요청",
    "categoryName": "도로·교통",
    "appliedDate": "2026-08-01",
    "departmentName": "교통정책과",
    "completedAt": "2026-08-13",
    "responseContent": "현장 교통량과 보행량 조사를 실시한 결과, 등교 시간대 보행 신호를 8초 연장하기로 결정했습니다.",
    "attachments": [
      {
        "attachmentId": 8101,
        "originalFilename": "교통량조사결과.pdf"
      }
    ]
  },
  "message": "공개 답변 상세를 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 404 | RESOURCE_NOT_FOUND | 공개된 답변이 없음 |

---

## 5. Officer Workflow API
공무원 업무함은 신규 배정, 처리중, 완료 중심으로 정의합니다. 보완요청 편집, 제출기한, 요청파일, 내부 메모 컨테이너와 답변 마감기한은 이번 버전에서 제외합니다.

### 5.1 담당 민원 목록 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/officer/complaints` | Bearer | OFFICER / ADMIN | 200 OK |

공무원 업무함 목록을 조회합니다. 신규 배정, 처리중, 완료 요약값과 목록을 함께 반환합니다.
OFFICER는 자신에게 배정된 민원만 조회하며, ADMIN은 담당자 배정 여부와 관계없이 전체 민원을 조회합니다.
ADMIN 응답의 요약값도 전체 민원을 기준으로 계산합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| page | number | N | 페이지 번호 | 기본값 0 |
| size | number | N | 페이지 크기 | 기본값 20 |
| status | string | N | 처리 상태 필터 | RECEIVED, ASSIGNED, IN_PROGRESS, COMPLETED |
| keyword | string | N | 민원번호/제목 검색어 | 최대 50자 |

**Request Example**
```json
{
  "page": 0,
  "size": 20,
  "status": "IN_PROGRESS",
  "keyword": "신호시간"
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.summary.newAssigned | number | 신규 배정 건수 |
| data.summary.inProgress | number | 처리중 건수 |
| data.summary.completed | number | 완료 건수 |
| data.content[].assigneeName | string | 담당자명 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "summary": {
      "newAssigned": 12,
      "inProgress": 18,
      "completed": 86
    },
    "content": [
      {
        "complaintNo": "CIV-2026-000184",
        "title": "어린이보호구역 신호시간 조정 요청",
        "assigneeName": "김담당",
        "status": "IN_PROGRESS"
      }
    ]
  },
  "message": "담당 민원 목록을 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 403 | FORBIDDEN | 공무원 또는 관리자 권한 없음 |

### 5.2 민원 상태 변경
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| PATCH | `/api/v1/officer/complaints/{complaintId}/status` | Bearer | OFFICER / ADMIN | 200 OK |

담당 민원의 상태를 변경합니다. 허용 상태는 RECEIVED → ASSIGNED → IN_PROGRESS → COMPLETED 입니다. 단, `IN_PROGRESS -> COMPLETED` 전이는 공식 답변이 먼저 등록되어 있어야만 허용됩니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| complaintId | path(long) | Y | 민원 ID | 담당 부서 또는 관리자만 가능 |
| newStatus | string | Y | 변경할 상태 | ASSIGNED, IN_PROGRESS, COMPLETED |
| changeMemo | string | N | 변경 메모 | 최대 500자 |

**Request Example**
```json
{
  "newStatus": "IN_PROGRESS",
  "changeMemo": "현장 조사 착수"
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.previousStatus | string | 이전 상태 |
| data.newStatus | string | 변경된 상태 |
| data.changedAt | datetime | 변경 시각 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "previousStatus": "ASSIGNED",
    "newStatus": "IN_PROGRESS",
    "changedAt": "2026-08-14T15:00:00+09:00"
  },
  "message": "민원 상태가 변경되었습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 403 | FORBIDDEN | 담당 부서가 아닌 사용자 |
| 409 | INVALID_STATUS_TRANSITION | 허용되지 않은 상태 전이 |

### 5.3 민원 답변 등록
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/officer/complaints/{complaintId}/response` | Bearer | OFFICER / ADMIN | 201 Created |

민원 공식 답변을 등록합니다. 이번 버전에서는 보완요청 편집, 제출기한, 요청파일, 내부 메모, 답변 마감기한 관련 필드는 없습니다. 답변 등록은 `COMPLETED` 상태 전이의 선행 조건입니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| complaintId | path(long) | Y | 민원 ID | 담당 부서 또는 관리자만 가능 |
| responseContent | string | Y | 답변 본문 | 20~3000자 |
| isPublic | boolean | N | 공개 답변 여부 | 기본값 false |

**Request Example**
```json
{
  "responseContent": "현장 교통량과 보행량 조사를 실시한 결과, 등교 시간대 보행 신호를 8초 연장하기로 결정했습니다.",
  "isPublic": true
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.responseId | long | 답변 ID |
| data.isPublic | boolean | 공개 여부 |
| data.respondedAt | datetime | 답변 등록 시각 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "responseId": 7001,
    "isPublic": true,
    "respondedAt": "2026-08-13T16:20:00+09:00"
  },
  "message": "민원 답변이 등록되었습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 400 | VALIDATION_ERROR | 답변 본문 길이 부족 |
| 403 | FORBIDDEN | 담당 부서가 아닌 사용자 |

---

## 6. Notification API

### 6.1 알림 목록 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/notifications` | Bearer | 로그인 사용자 | 200 OK |

알림센터의 전체 리스트와 미읽음 카운트를 조회합니다. 민원 상태의 변동이나 답변 등록 등의 이벤트 발생 시 인앱(IN_APP) 알림은 상시 발행을 원칙으로 하되, 이메일(EMAIL)의 경우 사용자 설정 및 수신 동의 여부를 확인하여 보조적으로 전송합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| page | number | N | 페이지 번호 | 기본값 0 |
| size | number | N | 페이지 크기 | 기본값 20 |
| isRead | boolean | N | 읽음 여부 필터 | 미입력 시 전체 |

**Request Example**
```json
{
  "page": 0,
  "size": 20,
  "isRead": false
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.unreadCount | number | 미읽음 개수 |
| data.content[].notificationId | long | 알림 ID |
| data.content[].message | string | 알림 메시지 |
| data.content[].createdAt | datetime | 알림 생성 시각(ISO 8601, 한국 표준시 `+09:00`) |

**Success Example**
```json
{
  "success": true,
  "data": {
    "unreadCount": 4,
    "content": [
      {
        "notificationId": 9001,
        "message": "상태가 완료로 변경되었습니다.",
        "createdAt": "2026-08-20T14:33:00+09:00"
      }
    ]
  },
  "message": "알림 목록을 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 401 | UNAUTHORIZED | 로그인 필요 |

### 6.2 알림 읽음 처리
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| PATCH | `/api/v1/notifications/{notificationId}/read` | Bearer | 로그인 사용자 | 200 OK |

단건 알림을 읽음 상태로 변경합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| notificationId | path(long) | Y | 알림 ID | 본인 알림만 가능 |

**Request Example**
```json
{
  "notificationId": 9001
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.readAt | datetime | 읽음 처리 시각(ISO 8601, 한국 표준시 `+09:00`) |

**Success Example**
```json
{
  "success": true,
  "data": {
    "readAt": "2026-08-14T15:30:00+09:00"
  },
  "message": "알림을 읽음 처리했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 404 | RESOURCE_NOT_FOUND | 존재하지 않는 알림 |

---

## 7. Statistics API

### 7.1 일별 통계 조회
| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| GET | `/api/v1/admin/statistics/daily` | Bearer | ADMIN | 200 OK |

Statistics Service가 Kafka 이벤트를 활용하여 DAILY_COMPLAINT_STATISTICS 테이블에 산출한 부서별 일별 지표를 조회합니다. 성능을 위해 Complaint DB에 직접 접근하지 않고 집계된 통계 데이터를 사용합니다.

처리기한 지표는 Complaint 도메인의 개별 마감기한 필드가 아니라 접수 시각에
`STATISTICS_PROCESSING_DEADLINE_DAYS`를 더한 통계용 SLA를 기준으로 계산합니다.
기한 임박 범위는 `STATISTICS_DEADLINE_APPROACHING_DAYS`로 설정합니다.

**Request**
| 필드 | 타입 | 필수 | 설명 | 검증 |
|---|---|---|---|---|
| fromDate | date | Y | 조회 시작일 | yyyy-MM-dd |
| toDate | date | Y | 조회 종료일 | yyyy-MM-dd |
| departmentId | long | N | 부서 필터 | 미입력 시 전체 |

**Request Example**
```json
{
  "fromDate": "2026-08-01",
  "toDate": "2026-08-14",
  "departmentId": 10
}
```

**Response**
| 필드 | 타입 | 설명 |
|---|---|---|
| data.content[].statisticDate | date | 통계 기준일 |
| data.content[].newReceivedCount | number | 신규 접수 건수 |
| data.content[].newCompletedCount | number | 신규 완료 건수 |
| data.content[].receivedStatusCount | number | RECEIVED 상태 건수 |
| data.content[].assignedStatusCount | number | ASSIGNED 상태 건수 |
| data.content[].inProgressStatusCount | number | IN_PROGRESS 상태 건수 |
| data.content[].completedStatusCount | number | COMPLETED 상태 건수 |
| data.content[].deadlineApproachingCount | number | 미완료 민원 중 설정된 임박 기준 이내 도래 건수 |
| data.content[].overdueCount | number | 미완료 민원 중 처리기한 초과 건수 |
| data.content[].averageProcessingHours | decimal | submittedAt부터 COMPLETED statusChangedAt까지 평균 시간 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "statisticDate": "2026-08-14",
        "newReceivedCount": 12,
        "newCompletedCount": 4,
        "receivedStatusCount": 2,
        "assignedStatusCount": 3,
        "inProgressStatusCount": 7,
        "completedStatusCount": 40,
        "deadlineApproachingCount": 1,
        "overdueCount": 0,
        "averageProcessingHours": 18.5
      }
    ]
  },
  "message": "일별 통계를 조회했습니다."
}
```

**Error / 예외**
| HTTP | error.code | 발생 조건 |
|---|---|---|
| 403 | FORBIDDEN | 관리자 권한 없음 |
| 400 | VALIDATION_ERROR | 날짜 범위 형식 오류 |

---

## 8. 서비스 간 통신 계약

### 8.1 OpenFeign 자동 배정
Complaint Service는 민원 접수 후 Assignment Service의 내부 API를 OpenFeign으로 동기 호출합니다. 내부 API는 Gateway 외부 라우팅에서 제외하고 서비스 네트워크에서만 접근합니다.

초기 마스터 데이터 기준 카테고리/규칙 ID는 아래와 같이 고정합니다.
- 카테고리: `1 TRAFFIC`, `2 ENVIRONMENT`, `3 FACILITY`, `4 WELFARE`, `5 ETC`
- 부서: `10 교통정책과`, `20 도로관리과`, `30 환경관리과`, `40 복지지원과`, `50 민원총괄과`
- 공무원: `201`, `202`, `203`, `204`, `205`
- 배정 규칙: `31`, `32`, `33`, `34`, `35`

| Method | Endpoint | 인증 | 권한 | 성공 상태 |
|---|---|---|---|---|
| POST | `/api/v1/internal/assignments` | 내부 서비스 인증 | complaint-service | 200 OK |

내부 서비스 인증은 `X-Internal-Caller: complaint-service` 헤더와 내부 네트워크 격리를 기준으로 합니다.

**Request DTO**
| 필드 | 타입 | 설명 |
|---|---|---|
| complaintId | long | 생성된 민원 ID |
| complaintNo | string | 사용자 노출용 민원 번호 |
| categoryId | long | 카테고리 ID |
| categoryCode | string | COMPLAINT_CATEGORY.category_code |
| applicantUserId | long | 신청자 ID |
| submittedAt | datetime | 민원 제출 시각 |

**Success Response DTO**
| 필드 | 타입 | 설명 |
|---|---|---|
| assignmentFound | boolean | 항상 true |
| departmentId | long | 배정 부서 ID |
| departmentName | string | 배정 부서명 |
| officerUserId | long | 배정 공무원 사용자 ID |
| officerName | string | 담당 공무원명 |
| assignmentRuleId | long | 적용된 `departmentCategoryId` |
| assignedAt | datetime | 배정 완료 시각 |

**Business Failure Response DTO**
배정 규칙이 없어서 담당자를 찾지 못한 경우는 통신 성공이지만 비즈니스 실패로 처리합니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| assignmentFound | boolean | 항상 false |
| reasonCode | string | `NO_MATCHING_RULE`, `NO_ACTIVE_OFFICER` |
| reasonMessage | string | 실패 이유 설명 |

> 배정 성공 시 Complaint Service는 `assigned_department_id`, `assigned_officer_user_id`, `assigned_at`를 저장하고 상태를 ASSIGNED로 변경합니다. 배정 규칙이 없어 `assignmentFound=false`이거나 Assignment Service 장애 또는 시간 초과가 발생하면 민원은 삭제하지 않고 RECEIVED 상태로 유지합니다.
>
> 초기 활성 규칙은 `31(TRAFFIC -> 10 -> 201)`, `32(FACILITY -> 20 -> 202)`, `33(ENVIRONMENT -> 30 -> 203)`, `34(WELFARE -> 40 -> 204)`, `35(ETC -> 50 -> 205)`입니다.

### 8.2 Kafka 이벤트 및 실패 처리
민원의 생성, 상태 변경, 답변 등록 시 complaint-service는 Kafka 이벤트를 발행합니다. notification-service와 statistics-service는 complaint DB를 직접 조회하지 않고, 각자 필요한 이벤트를 구독하여 비동기로 처리합니다.

**공통 이벤트 envelope**
| 필드 | 타입 | 설명 |
|---|---|---|
| eventId | string | 전역 유일 이벤트 ID |
| eventType | string | 이벤트 타입 식별자: `ComplaintCreated`, `ComplaintStatusChanged`, `ComplaintResponseRegistered` |
| eventVersion | string | 스키마 버전, 초기값 `v1` |
| occurredAt | datetime | 이벤트 발생 시각 |
| producer | string | 항상 `complaint-service` |
| partitionKey | string | Kafka key, `complaintId` 문자열 |
| payload | object | 실제 비즈니스 데이터 |

**Topic 목록**
| Topic | Producer | Consumer | 용도 |
|---|---|---|---|
| `complaint.created.v1` | complaint-service | statistics-service | 신규 접수 집계 |
| `complaint.status.changed.v1` | complaint-service | notification-service, statistics-service | 배정/처리중/완료 상태 전이 |
| `complaint.response.registered.v1` | complaint-service | notification-service | 공식 답변 등록 알림 |

**`complaint.created.v1`**
- 발행 시점: 민원 접수 후 `COMPLAINT`가 `RECEIVED`로 저장되고 commit된 직후
- 핵심 payload: `complaintId`, `complaintNo`, `applicantUserId`, `categoryId`, `categoryCode`, `currentStatus=RECEIVED`, `submittedAt`, `notifyChannels`

**`complaint.status.changed.v1`**
- 발행 시점: `RECEIVED -> ASSIGNED`, `ASSIGNED -> IN_PROGRESS`, `IN_PROGRESS -> COMPLETED`
- 상태 변경 이벤트는 하나의 topic으로 통합하고 상태는 payload의 `previousStatus`, `currentStatus`로 구분
- 핵심 payload: `complaintId`, `complaintNo`, `applicantUserId`, `categoryId`, `categoryCode`, `previousStatus`, `currentStatus`, `assignedDepartmentId`, `assignedOfficerUserId`, `statusChangedByUserId`, `statusChangedAt`, `changeMemo`, `respondedAt`, `notifyChannels`

**`complaint.response.registered.v1`**
- 발행 시점: 공식 답변이 저장되고 commit된 직후
- 핵심 payload: `complaintId`, `complaintNo`, `responseId`, `applicantUserId`, `responderUserId`, `isPublic`, `respondedAt`, `assignedDepartmentId`, `notifyChannels`
- `RESPONSE.isPublic` 변경 전용 이벤트는 이번 버전 범위에서 제외

**Consumer 규칙**
- notification-service는 `notifyChannels`와 사용자 동의 여부를 기준으로 IN_APP은 기본 생성하고, EMAIL은 최종 발송 여부를 판단합니다.
- statistics-service는 complaint DB 직접 조회 없이 전이 이벤트 기반 증감 방식으로 상태별 건수와 완료 집계를 계산합니다.
- `complaint.created.v1`는 신규 접수 건수 증가에 사용합니다.
- `complaint.status.changed.v1`는 `assigned/in_progress/completed` 상태 증감과 완료 집계에 사용합니다.

**중복 방지 및 실패 정책**
| 항목 | 값 |
|---|---|
| Kafka message key | `complaintId` 문자열 |
| 중복 방지 | `eventId` 기반 멱등성 보장 |
| 실패 정책 | 발행/소비 단계 재시도 및 DLQ 처리 |
| Outbox | 이번 버전 범위에서는 미적용 |

**Canonical naming / format**
- Kafka topic은 소문자 버전형 이름(`complaint.status.changed.v1`)을 사용하고, envelope의 `eventType`은 PascalCase 비즈니스 이벤트명(`ComplaintStatusChanged`)을 사용합니다.
- 상태 이력 엔티티의 필드는 `changedByUserId`, Kafka 상태 변경 payload의 필드는 `statusChangedByUserId`로 구분합니다.
- 외부 노출 민원 번호 형식은 `CIV-{yyyy}-{6자리 순번}`으로 고정합니다. 예: `CIV-2026-000184`.
- API와 이벤트의 datetime 예시는 ISO 8601 `Asia/Seoul` 오프셋(`+09:00`)을 기준으로 합니다.

### 8.3 Gateway 인증 정보 전달
API Gateway에서 외부 인입 요청의 JWT를 검증합니다. Gateway는 외부 헤더를 제거한 후 `X-User-Id`, `X-Login-Id`, `X-User-Role`, `X-Department-Id`, `X-Request-Id`를 생성하여 내부 서비스로 전파합니다. `X-Department-Id`는 값이 없으면 전달하지 않습니다. 각 마이크로서비스는 토큰 재검증 없이 Gateway가 전달한 사용자 메타데이터와 내부 비즈니스 데이터를 결합하여 접근 권한을 판단합니다.

---

## 9. 서비스 소유 경계 요약

| 서비스 | 소유 DB | 핵심 책임 | 대표 테이블 |
|---|---|---|---|
| **user-service** | user_db | 사용자·권한·토큰 관리 | USER |
| **assignment-service** | assignment_db | 부서/카테고리/매핑 규칙 | DEPARTMENT, COMPLAINT_CATEGORY, DEPARTMENT_CATEGORY |
| **complaint-service** | complaint_db | 민원 원본·첨부파일·이력·답변 | COMPLAINT, COMPLAINT_ATTACHMENT, COMPLAINT_STATUS_HISTORY, COMPLAINT_RESPONSE |
| **notification-service** | notification_db | 알림 저장·읽음 처리 | NOTIFICATION |
| **statistics-service** | statistics_db | 관리자 집계 조회 | DAILY_COMPLAINT_STATISTICS |
