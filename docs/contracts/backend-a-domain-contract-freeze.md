# Backend A Domain Contract Freeze

기준일: 2026-08-15  
범위: TASK 1 ~ TASK 3  
목적: 백엔드 A가 백엔드 B와 바로 맞물리는 도메인 계약을 먼저 동결하고, 구현 전 꼭 필요한 결정만 분리한다.

## 1. 표준 명칭과 서비스 경계

### 1.1 표준 서비스 명칭
이번 계약 문서에서는 아래 명칭을 표준으로 사용한다.

| 표준 명칭 | 현재 저장소 디렉터리 | 설명 |
|---|---|---|
| `user-service` | 없음 | 회원가입, 로그인, JWT 발급, 사용자/권한 정보 |
| `complaint-service` | `complaint-service` | 민원 원본, 상태 전이, 첨부파일 메타데이터, 답변 |
| `assignment-service` | `assignment-service` | 부서, 카테고리, 카테고리-부서-담당자 매핑 규칙 |
| `notification-service` | `notification-service` | 상태 변경/답변 등록 기반 알림 |
| `statistics-service` | 없음 | Kafka 이벤트 기반 집계 |

### 1.2 서비스 소유권 최종안
서비스 간 DB 직접 접근은 금지한다. 다른 서비스 데이터는 오직 `ID 논리 참조`, `Feign`, `Kafka 이벤트`로만 사용한다.

| 계약 엔티티 | 소유 서비스 | 실제 기준 테이블/집합 | 비고 |
|---|---|---|---|
| `USER` | `user-service` | `USER` | 인증/인가 원본 |
| `COMPLAINT` | `complaint-service` | `COMPLAINT` | 민원 원본과 현재 상태의 source of truth |
| `ASSIGNMENT` | `assignment-service` | `DEPARTMENT`, `COMPLAINT_CATEGORY`, `DEPARTMENT_CATEGORY` | 현재 ERD에는 별도 `ASSIGNMENT` 테이블이 없으므로 "배정 규칙 aggregate"로 정의 |
| `RESPONSE` | `complaint-service` | `COMPLAINT_RESPONSE` | 공식 민원 답변 |
| `ATTACHMENT` | `complaint-service` | `COMPLAINT_ATTACHMENT` | 첨부파일 메타데이터만 저장, 원본은 S3 |
| `STATUS` | `complaint-service` | `COMPLAINT_STATUS_HISTORY` | 상태 변경 이력 |

### 1.3 책임 분리 원칙
- `complaint-service`가 민원 상태의 source of truth다.
- `assignment-service`는 배정 후보와 결과를 계산해 반환하지만, `COMPLAINT.currentStatus`를 직접 변경하지 않는다.
- `notification-service`와 `statistics-service`는 `complaint-service`가 발행한 이벤트를 소비할 뿐, 민원 원본을 수정하지 않는다.
- `RESPONSE`와 `STATUS`는 모두 `complaint-service` 내부에서 함께 관리한다.

## 2. 상태 머신 최종안

### 2.1 ComplaintStatus enum

| 값 | 의미 | 기록 주체 |
|---|---|---|
| `RECEIVED` | 민원 본문 저장 완료, 아직 자동 배정 전 또는 배정 실패 상태 | `complaint-service` |
| `ASSIGNED` | 담당 부서/담당 공무원 배정 완료 | `complaint-service` |
| `IN_PROGRESS` | 담당 공무원이 처리 착수 | `complaint-service` |
| `COMPLETED` | 공식 답변이 등록되고 민원 처리가 종료됨 | `complaint-service` |

### 2.2 상태 전이 규칙

| From | To | 허용 여부 | 전이 주체 | 조건 |
|---|---|---|---|---|
| 없음 | `RECEIVED` | 허용 | `complaint-service` | 시민 민원 접수 성공 |
| `RECEIVED` | `ASSIGNED` | 허용 | `complaint-service` | `assignment-service` 응답 성공 |
| `RECEIVED` | `IN_PROGRESS` | 금지 | - | 자동 배정 없이 바로 처리 시작 불가 |
| `RECEIVED` | `COMPLETED` | 금지 | - | 접수 직후 완료 불가 |
| `ASSIGNED` | `IN_PROGRESS` | 허용 | `complaint-service` | 담당 공무원 또는 관리자 처리 착수 |
| `ASSIGNED` | `COMPLETED` | 금지 | - | 처리중 단계 없이 바로 완료 불가 |
| `IN_PROGRESS` | `COMPLETED` | 허용 | `complaint-service` | 공식 답변 존재 필요 |
| `IN_PROGRESS` | `ASSIGNED` | 금지 | - | 역전이 금지 |
| `COMPLETED` | 그 외 모든 상태 | 금지 | - | 완료 후 재오픈 미지원 |

### 2.3 실패/예외 규칙
- 민원 접수는 항상 `COMPLAINT` 저장을 우선한다.
- 자동 배정 Feign 호출이 실패하거나 타임아웃되면 민원은 삭제하지 않고 `RECEIVED`를 유지한다.
- 배정 성공 시에만 `assignedDepartmentId`, `assignedOfficerUserId`, `assignedAt`를 채우고 상태를 `ASSIGNED`로 변경한다.
- 상태 변경이 일어나면 항상 `STATUS` 이력을 한 건 남긴다.
- Kafka 이벤트는 상태 변경이 실제 commit된 이후에만 발행한다.

### 2.4 STATUS 이력 최소 규칙

| 필드 | 의미 |
|---|---|
| `statusHistoryId` | 상태 이력 PK |
| `complaintId` | 대상 민원 ID |
| `previousStatus` | 이전 상태, 최초 접수는 `null` 허용 |
| `newStatus` | 변경된 상태 |
| `changedByUserId` | 상태 변경 사용자, 시스템 자동 배정이면 `null` 허용 |
| `changeMemo` | 수동 상태 변경 메모 |
| `changedAt` | 변경 시각 |

## 3. 핵심 엔티티 필드 최종안

### 3.1 USER

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `userId` | `long` | Y | `user-service` | 내부 사용자 PK |
| `loginId` | `string` | Y | `user-service` | 로그인 ID, unique |
| `passwordHash` | `string` | Y | `user-service` | 해시 저장 |
| `name` | `string` | Y | `user-service` | 사용자 이름 |
| `email` | `string` | Y | `user-service` | 이메일, unique |
| `phone` | `string` | Y | `user-service` | 휴대폰 번호 |
| `role` | `enum(Role)` | Y | `user-service` | `CITIZEN`, `OFFICER`, `ADMIN` |
| `departmentId` | `long \| null` | N | `user-service` | 공무원 소속 부서 ID, 논리 참조 |
| `emailNotifyAgreed` | `boolean` | Y | `user-service` | 이메일 수신 동의 |
| `isActive` | `boolean` | Y | `user-service` | 계정 활성 여부 |
| `createdAt` | `datetime` | Y | `user-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `user-service` | 수정 시각 |

### 3.2 COMPLAINT

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `complaintId` | `long` | Y | `complaint-service` | 민원 PK |
| `complaintNo` | `string` | Y | `complaint-service` | 사용자 노출용 민원 번호, unique |
| `applicantUserId` | `long` | Y | `complaint-service` | 민원 신청자 ID, `USER.userId` 논리 참조 |
| `categoryId` | `long` | Y | `complaint-service` | 민원 카테고리 ID, 논리 참조 |
| `assignedDepartmentId` | `long \| null` | N | `complaint-service` | 배정 부서 ID, 논리 참조 |
| `assignedOfficerUserId` | `long \| null` | N | `complaint-service` | 담당 공무원 ID, 논리 참조 |
| `title` | `string` | Y | `complaint-service` | 민원 제목 |
| `content` | `text` | Y | `complaint-service` | 민원 본문 |
| `currentStatus` | `enum(ComplaintStatus)` | Y | `complaint-service` | 현재 상태 |
| `submittedAt` | `datetime` | Y | `complaint-service` | 제출 시각 |
| `assignedAt` | `datetime \| null` | N | `complaint-service` | 배정 완료 시각 |
| `completedAt` | `datetime \| null` | N | `complaint-service` | 처리 완료 시각 |
| `createdAt` | `datetime` | Y | `complaint-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `complaint-service` | 수정 시각 |

### 3.3 ASSIGNMENT
현재 ERD에는 별도 `ASSIGNMENT` 테이블이 없으므로, 이번 계약에서 `ASSIGNMENT`는 배정 규칙 aggregate로 정의한다.

#### 3.3.1 Department

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `departmentId` | `long` | Y | `assignment-service` | 부서 PK |
| `departmentName` | `string` | Y | `assignment-service` | 부서명 |
| `isActive` | `boolean` | Y | `assignment-service` | 활성 여부 |
| `createdAt` | `datetime` | Y | `assignment-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `assignment-service` | 수정 시각 |

#### 3.3.2 ComplaintCategory

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `categoryId` | `long` | Y | `assignment-service` | 카테고리 PK |
| `categoryName` | `string` | Y | `assignment-service` | 카테고리명 |
| `categoryCode` | `string` | Y | `assignment-service` | 외부 계약용 unique code |
| `createdAt` | `datetime` | Y | `assignment-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `assignment-service` | 수정 시각 |

#### 3.3.3 DepartmentCategoryRule

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `departmentCategoryId` | `long` | Y | `assignment-service` | 매핑 PK |
| `departmentId` | `long` | Y | `assignment-service` | 부서 ID |
| `categoryId` | `long` | Y | `assignment-service` | 카테고리 ID |
| `officerUserId` | `long` | Y | `assignment-service` | 기본 담당자 ID, `USER.userId` 논리 참조 |
| `isActive` | `boolean` | Y | `assignment-service` | 활성 여부 |
| `createdAt` | `datetime` | Y | `assignment-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `assignment-service` | 수정 시각 |

### 3.4 RESPONSE

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `responseId` | `long` | Y | `complaint-service` | 답변 PK |
| `complaintId` | `long` | Y | `complaint-service` | 대상 민원 ID |
| `responderUserId` | `long` | Y | `complaint-service` | 답변 작성 공무원 ID |
| `responseContent` | `text` | Y | `complaint-service` | 공식 답변 본문 |
| `isPublic` | `boolean` | Y | `complaint-service` | 공개 답변 여부 |
| `respondedAt` | `datetime` | Y | `complaint-service` | 답변 등록 시각 |
| `updatedAt` | `datetime` | Y | `complaint-service` | 수정 시각 |

### 3.5 ATTACHMENT

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `attachmentId` | `long` | Y | `complaint-service` | 첨부파일 PK |
| `complaintId` | `long` | Y | `complaint-service` | 대상 민원 ID |
| `originalFilename` | `string` | Y | `complaint-service` | 사용자 업로드 원본명 |
| `storedFilename` | `string` | Y | `complaint-service` | 내부 저장 파일명 또는 object key |
| `filePath` | `string` | Y | `complaint-service` | 저장 경로 또는 S3 key |
| `contentType` | `string` | Y | `complaint-service` | MIME type |
| `fileSize` | `long` | Y | `complaint-service` | 파일 크기 byte |
| `uploadedAt` | `datetime` | Y | `complaint-service` | 업로드 시각 |
| `createdAt` | `datetime` | Y | `complaint-service` | 생성 시각 |
| `updatedAt` | `datetime` | Y | `complaint-service` | 수정 시각 |

### 3.6 STATUS

| 필드 | 타입 | 필수 | 소유 서비스 | 설명 |
|---|---|---|---|---|
| `statusHistoryId` | `long` | Y | `complaint-service` | 상태 이력 PK |
| `complaintId` | `long` | Y | `complaint-service` | 대상 민원 ID |
| `previousStatus` | `enum(ComplaintStatus) \| null` | N | `complaint-service` | 이전 상태 |
| `newStatus` | `enum(ComplaintStatus)` | Y | `complaint-service` | 신규 상태 |
| `changedByUserId` | `long \| null` | N | `complaint-service` | 상태 변경 사용자 ID |
| `changeMemo` | `string \| null` | N | `complaint-service` | 변경 메모 |
| `changedAt` | `datetime` | Y | `complaint-service` | 변경 시각 |

## 4. enum 최종안

### 4.1 Role

| 값 | 설명 |
|---|---|
| `CITIZEN` | 민원 신청자 |
| `OFFICER` | 민원 처리 공무원 |
| `ADMIN` | 관리자 |

### 4.2 ComplaintStatus

| 값 | 설명 |
|---|---|
| `RECEIVED` | 접수 완료, 자동 배정 전/실패 |
| `ASSIGNED` | 담당 부서/공무원 배정 완료 |
| `IN_PROGRESS` | 처리 착수 |
| `COMPLETED` | 공식 답변 등록 후 처리 완료 |

## 5. 확정된 결정 사항

2026-08-15 기준으로 TASK 1~3 범위의 아래 항목을 최종 확정한다.

| 항목 | 최종 결정 |
|---|---|
| 표준 명칭 | 계약 문서와 API에서는 `assignment-service`를 사용한다 |
| 저장소 디렉터리 명칭 | `assignment-service`로 통일한다 |
| `ASSIGNMENT` 모델 형태 | 별도 `ASSIGNMENT` 테이블을 만들지 않고 `DEPARTMENT + COMPLAINT_CATEGORY + DEPARTMENT_CATEGORY` aggregate를 유지한다 |
| `COMPLETED` 진입 조건 | `RESPONSE`가 먼저 생성되어 있어야만 `IN_PROGRESS -> COMPLETED`를 허용한다 |
| 자동 배정 상태 이력의 `changedByUserId` | `null` 허용 |
| `complaintNo` | 사용자 노출용 민원 번호로 유지한다 |

## 6. TASK 1~3 완료 상태

아래 범위는 이번 문서 기준으로 동결되었다.

1. 서비스 경계와 엔티티 소유권
2. `ComplaintStatus` enum과 상태 전이 규칙
3. `USER`, `COMPLAINT`, `ASSIGNMENT`, `RESPONSE`, `ATTACHMENT`, `STATUS` 핵심 필드

다음 단계인 TASK 4~5에서는 이 문서를 기준으로 `JWT payload`, `Feign DTO`, `공통 에러 코드`를 정의한다.

## 7. JWT Payload and Gateway Auth Contract

### 7.1 토큰 설계 원칙
- 외부 클라이언트는 `Authorization: Bearer {accessToken}` 형식으로 요청한다.
- JWT 서명 검증은 API Gateway가 담당한다.
- 내부 마이크로서비스는 JWT를 재검증하지 않고, Gateway가 전달한 신뢰 가능한 헤더를 사용한다.
- JWT에는 인가에 필요한 최소 클레임만 넣고, 자주 변하거나 민감한 값은 DB 재조회 대상으로 남긴다.

### 7.2 Access Token Payload 최종안

| Claim | 타입 | 필수 | 예시 | 설명 |
|---|---|---|---|---|
| `sub` | `string` | Y | `"101"` | 사용자 식별자 문자열, `userId`와 동일 의미 |
| `userId` | `long` | Y | `101` | 내부 사용자 PK |
| `loginId` | `string` | Y | `"citizen01"` | 로그인 ID |
| `role` | `string` | Y | `"CITIZEN"` | `Role` enum 값 |
| `departmentId` | `long \| null` | Y | `null` | 공무원 소속 부서 ID, 시민은 `null` |
| `tokenType` | `string` | Y | `"ACCESS"` | 액세스 토큰 구분값 |
| `iat` | `epoch-seconds` | Y | `1786747800` | 발급 시각 |
| `exp` | `epoch-seconds` | Y | `1786751400` | 만료 시각 |
| `iss` | `string` | N | `"minwonon-auth"` | 발급자 식별자 |

### 7.3 Refresh Token Payload 최종안

| Claim | 타입 | 필수 | 예시 | 설명 |
|---|---|---|---|---|
| `sub` | `string` | Y | `"101"` | 사용자 식별자 문자열 |
| `userId` | `long` | Y | `101` | 내부 사용자 PK |
| `tokenType` | `string` | Y | `"REFRESH"` | 리프레시 토큰 구분값 |
| `iat` | `epoch-seconds` | Y | `1786747800` | 발급 시각 |
| `exp` | `epoch-seconds` | Y | `1787352600` | 만료 시각 |
| `iss` | `string` | N | `"minwonon-auth"` | 발급자 식별자 |

### 7.4 서비스가 신뢰하는 토큰 정보

| 필드 | 신뢰 여부 | 이유 |
|---|---|---|
| `userId` | 신뢰 | 리소스 소유자 판별에 즉시 사용 |
| `loginId` | 신뢰 | 로깅/감사용 보조 식별자 |
| `role` | 신뢰 | 역할 기반 접근 제어에 즉시 사용 |
| `departmentId` | 조건부 신뢰 | 권한 분기에는 사용 가능하나, 상세 부서 정책은 필요 시 재조회 가능 |
| `emailNotifyAgreed` | 비포함 | 자주 변하고 알림 비즈니스 데이터이므로 토큰에 넣지 않음 |
| `isActive` | 비포함 | 비활성화 즉시 반영 요구 가능성이 있어 토큰보다 사용자 조회가 안전 |

### 7.5 Gateway 내부 전달 헤더 최종안
Gateway는 외부에서 들어온 동일 이름 헤더를 모두 제거한 뒤 아래 헤더를 새로 생성한다.

| Header | 타입 | 필수 | 설명 |
|---|---|---|---|
| `X-User-Id` | `string` | Y | JWT의 `userId` |
| `X-Login-Id` | `string` | Y | JWT의 `loginId`, access token에만 존재 |
| `X-User-Role` | `string` | Y | JWT의 `role` |
| `X-Department-Id` | `string` | N | JWT의 `departmentId`, 값이 없으면 헤더를 전달하지 않음 |
| `X-Request-Id` | `string` | Y | Gateway 생성 요청 추적 ID |

### 7.6 Access Token Example

```json
{
  "sub": "201",
  "userId": 201,
  "loginId": "officer01",
  "role": "OFFICER",
  "departmentId": 10,
  "tokenType": "ACCESS",
  "iat": 1786747800,
  "exp": 1786751400,
  "iss": "minwonon-auth"
}
```

## 8. Feign DTO Contract Freeze

### 8.1 동기 통신 원칙
- `complaint-service`는 민원 접수 직후 `assignment-service`를 동기 호출한다.
- 내부 API는 Gateway 외부 라우팅에 노출하지 않는다.
- 내부 통신도 공통 응답 envelope를 사용하되, DTO는 비즈니스 필드 중심으로 고정한다.
- 배정 실패는 "비즈니스 실패"와 "통신 실패"를 구분한다.

### 8.2 Complaint -> Assignment 요청 DTO

**Endpoint**
- `POST /api/v1/internal/assignments`

**Headers**
- `X-Request-Id`
- `X-Internal-Caller: complaint-service`

**Request DTO**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | `long` | Y | 생성된 민원 ID |
| `complaintNo` | `string` | Y | 사용자 노출용 민원 번호 |
| `categoryId` | `long` | Y | 카테고리 ID |
| `categoryCode` | `string` | Y | 카테고리 코드 |
| `applicantUserId` | `long` | Y | 신청자 ID |
| `submittedAt` | `datetime` | Y | 민원 제출 시각 |

**Request Example**
```json
{
  "complaintId": 5001,
  "complaintNo": "CIV-2026-000184",
  "categoryId": 1,
  "categoryCode": "TRAFFIC",
  "applicantUserId": 101,
  "submittedAt": "2026-08-15T10:30:00+09:00"
}
```

### 8.3 Assignment 성공 응답 DTO

**Response DTO**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `assignmentFound` | `boolean` | Y | 배정 성공 여부 |
| `departmentId` | `long` | Y | 배정 부서 ID |
| `departmentName` | `string` | Y | 배정 부서명 |
| `officerUserId` | `long` | Y | 담당 공무원 ID |
| `officerName` | `string` | N | 담당 공무원명, 표시/로그용 |
| `assignmentRuleId` | `long` | Y | 적용된 `departmentCategoryId` |
| `assignedAt` | `datetime` | Y | 배정 완료 시각 |

**Success Example**
```json
{
  "success": true,
  "data": {
    "assignmentFound": true,
    "departmentId": 10,
    "departmentName": "교통정책과",
    "officerUserId": 201,
    "officerName": "김담당",
    "assignmentRuleId": 31,
    "assignedAt": "2026-08-15T10:30:02+09:00"
  },
  "message": "담당 부서와 담당 공무원을 배정했습니다."
}
```

### 8.4 Assignment 비즈니스 실패 응답 DTO
배정 규칙이 없어서 담당자를 찾지 못한 경우는 통신 성공이지만 비즈니스 실패로 본다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `assignmentFound` | `boolean` | Y | 항상 `false` |
| `reasonCode` | `string` | Y | `NO_MATCHING_RULE`, `NO_ACTIVE_OFFICER` |
| `reasonMessage` | `string` | Y | 실패 이유 설명 |

**Business Failure Example**
```json
{
  "success": true,
  "data": {
    "assignmentFound": false,
    "reasonCode": "NO_MATCHING_RULE",
    "reasonMessage": "활성화된 카테고리-부서 매핑 규칙이 없습니다."
  },
  "message": "자동 배정 대상을 찾지 못했습니다."
}
```

### 8.5 Complaint Service 처리 규칙

| Assignment 응답 결과 | Complaint Service 처리 |
|---|---|
| HTTP 200 + `assignmentFound=true` | `assignedDepartmentId`, `assignedOfficerUserId`, `assignedAt` 저장 후 `ASSIGNED` 전이 |
| HTTP 200 + `assignmentFound=false` | 민원 유지, 상태는 `RECEIVED` 유지 |
| HTTP 4xx/5xx 또는 timeout | 민원 유지, 상태는 `RECEIVED` 유지, 내부 로그와 모니터링 남김 |

### 8.6 내부 사용자 조회 DTO 최소안
이번 48시간 범위에서는 B와 즉시 맞물리는 최소 내부 사용자 조회 응답만 고정한다. 대상은 공무원 담당자 표시나 권한 확인 보조용이다.

**Endpoint**
- `GET /api/v1/internal/users/{userId}`

**Response DTO**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `userId` | `long` | Y | 사용자 PK |
| `loginId` | `string` | Y | 로그인 ID |
| `name` | `string` | Y | 사용자명 |
| `role` | `string` | Y | 역할 |
| `departmentId` | `long \| null` | Y | 소속 부서 ID |
| `isActive` | `boolean` | Y | 활성 여부 |

## 9. Common Error Contract Freeze

### 9.1 공통 실패 응답 최종안

```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "담당 부서가 아닌 사용자입니다.",
    "details": {
      "resource": "COMPLAINT",
      "complaintId": 5001
    }
  },
  "requestId": "req-20260815-0001"
}
```

### 9.2 공통 에러 응답 필드

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `success` | `boolean` | Y | 항상 `false` |
| `error.code` | `string` | Y | 표준 에러 코드 |
| `error.message` | `string` | Y | 사용자 또는 호출자에 전달할 메시지 |
| `error.details` | `object \| null` | N | 검증 오류/문맥 정보 |
| `requestId` | `string` | Y | Gateway 또는 서비스 생성 추적 ID |

### 9.3 공통 에러 코드 최종안

| error.code | HTTP | 재시도 | 설명 |
|---|---|---|---|
| `INVALID_REQUEST` | 400 | N | JSON 형식, multipart 형식 등 요청 구조 오류 |
| `VALIDATION_ERROR` | 400 | N | 필수값 누락, 길이/형식 검증 실패 |
| `UNAUTHORIZED` | 401 | 조건부 | 인증 헤더 누락 |
| `INVALID_CREDENTIALS` | 401 | N | 로그인 자격 증명 불일치 |
| `INVALID_TOKEN` | 401 | N | 토큰 서명/형식 오류 |
| `TOKEN_EXPIRED` | 401 | 조건부 | 재로그인 또는 리프레시 필요 |
| `FORBIDDEN` | 403 | N | 역할 또는 리소스 접근 권한 없음 |
| `RESOURCE_NOT_FOUND` | 404 | N | 조회 대상 없음 |
| `DUPLICATE_RESOURCE` | 409 | N | unique 제약 충돌 |
| `INVALID_STATUS_TRANSITION` | 409 | N | 허용되지 않은 상태 전이 |
| `SERVICE_UNAVAILABLE` | 503 | Y | 하위 서비스 장애, 타임아웃, 일시 장애 |
| `INTERNAL_SERVER_ERROR` | 500 | 조건부 | 서버 내부 오류 |

### 9.4 Feign/내부통신 오류 매핑

| 상황 | Assignment Service 응답 | Complaint Service 외부 응답 | 비고 |
|---|---|---|---|
| 배정 성공 | 200 | 201 (`ASSIGNED`) | 정상 |
| 배정 규칙 없음 | 200 + `assignmentFound=false` | 201 (`RECEIVED`) | 민원 저장 성공이 우선 |
| Assignment 5xx | 503 또는 timeout | 201 (`RECEIVED`) | 내부 로그에 `SERVICE_UNAVAILABLE` 남김 |
| Assignment 4xx 계약 위반 | 400/404/409 | 201 (`RECEIVED`) 또는 내부 알림 | 외부 민원 접수 실패로 번지지 않음 |

### 9.5 검증 오류 details 예시

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "요청값 검증에 실패했습니다.",
    "details": {
      "fieldErrors": [
        {
          "field": "title",
          "reason": "size must be between 5 and 100"
        }
      ]
    }
  },
  "requestId": "req-20260815-0002"
}
```

## 10. TASK 4~5 확정 사항

2026-08-15 기준으로 TASK 4~5 범위의 아래 항목을 최종 확정한다.

| 항목 | 최종 결정 |
|---|---|
| `departmentId` JWT 포함 여부 | Access Token에 포함한다 |
| Gateway의 `X-Department-Id` 빈값 처리 | 값이 없으면 헤더를 전달하지 않는다 |
| 내부 서비스 인증 방식 | `X-Internal-Caller` 헤더 + 내부 네트워크 격리로 처리한다 |
| 배정 규칙 없음의 error code | 별도 공통 에러 코드로 추가하지 않는다 |
| 내부 사용자 조회 DTO 범위 | 최소 필드만 고정하고 확장하지 않는다 |

## 11. TASK 4~5 완료 상태

아래 범위는 이번 문서 기준으로 동결되었다.

1. Access Token / Refresh Token payload
2. Gateway 내부 전달 헤더 규칙
3. `complaint-service -> assignment-service` Feign 요청/응답 DTO
4. 최소 내부 사용자 조회 DTO
5. 공통 실패 응답 포맷과 공통 에러 코드

## 12. Kafka Event Contract Freeze

### 12.1 이벤트 설계 원칙
- Kafka producer는 `complaint-service`다.
- 이벤트는 DB transaction commit 이후에만 발행한다.
- 이벤트는 "현재 상태 스냅샷"과 "전이 문맥"을 함께 담아 consumer가 complaint DB를 직접 조회하지 않아도 되게 한다.
- 같은 민원에서 같은 상태 전이가 중복 발행되더라도 consumer는 `eventId` 기준으로 멱등 처리한다.
- Notification과 Statistics는 같은 이벤트를 소비하되, 각자 필요한 필드만 사용한다.

### 12.2 공통 이벤트 envelope 최종안

Kafka topic은 소문자 버전형 이름(예: `complaint.status.changed.v1`)을 사용하고, `eventType`은 PascalCase 비즈니스 이벤트명(예: `ComplaintStatusChanged`)을 사용한다. 두 값은 동일 문자열로 취급하지 않는다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `eventId` | `string` | Y | 전역 유일 이벤트 ID(UUID 권장) |
| `eventType` | `string` | Y | 이벤트 타입 식별자 |
| `eventVersion` | `string` | Y | 스키마 버전, 초기값 `v1` |
| `occurredAt` | `datetime` | Y | 상태 변경 또는 생성 시각 |
| `producer` | `string` | Y | 항상 `complaint-service` |
| `partitionKey` | `string` | Y | Kafka key와 동일, `complaintId` 문자열 |
| `payload` | `object` | Y | 실제 비즈니스 데이터 |

### 12.3 공통 payload 기본 필드
아래 필드는 모든 complaint 계열 이벤트 payload에 공통 포함한다.

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | `long` | Y | 민원 PK |
| `complaintNo` | `string` | Y | 사용자 노출용 민원 번호 |
| `applicantUserId` | `long` | Y | 신청자 ID |
| `categoryId` | `long` | Y | 카테고리 ID |
| `categoryCode` | `string` | Y | 카테고리 코드 |
| `currentStatus` | `string` | Y | 이벤트 발생 시점의 현재 상태 |
| `assignedDepartmentId` | `long \| null` | N | 배정 부서 ID |
| `assignedOfficerUserId` | `long \| null` | N | 담당 공무원 ID |
| `statusChangedByUserId` | `long \| null` | N | 상태 변경 사용자 ID |
| `notifyChannels` | `string[]` | Y | 요청 당시 선택된 알림 채널, 최소 `IN_APP` 포함 |

### 12.4 Topic 최종안

| Topic | Producer | Consumer | 용도 |
|---|---|---|---|
| `complaint.created.v1` | `complaint-service` | `statistics-service` | 신규 접수 집계 |
| `complaint.status.changed.v1` | `complaint-service` | `notification-service`, `statistics-service` | 배정/처리중/완료 상태 전이 |
| `complaint.response.registered.v1` | `complaint-service` | `notification-service` | 공식 답변 등록 알림 |

### 12.5 complaint.created.v1

**발행 시점**
- 시민 민원 접수 후 `COMPLAINT`가 `RECEIVED`로 저장되고 commit된 직후

**목적**
- Statistics가 신규 접수 건수를 집계한다.
- Notification은 이 이벤트를 소비하지 않는다.

**Payload**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | `long` | Y | 민원 PK |
| `complaintNo` | `string` | Y | 민원 번호 |
| `applicantUserId` | `long` | Y | 신청자 ID |
| `categoryId` | `long` | Y | 카테고리 ID |
| `categoryCode` | `string` | Y | 카테고리 코드 |
| `currentStatus` | `string` | Y | 항상 `RECEIVED` |
| `submittedAt` | `datetime` | Y | 민원 제출 시각 |
| `notifyChannels` | `string[]` | Y | 요청 알림 채널 |

**Example**
```json
{
  "eventId": "0d78f7e4-1cb6-4af3-ae25-114d87d6c731",
  "eventType": "ComplaintCreated",
  "eventVersion": "v1",
  "occurredAt": "2026-08-15T10:30:00+09:00",
  "producer": "complaint-service",
  "partitionKey": "5001",
  "payload": {
    "complaintId": 5001,
    "complaintNo": "CIV-2026-000184",
    "applicantUserId": 101,
    "categoryId": 1,
    "categoryCode": "TRAFFIC",
    "currentStatus": "RECEIVED",
    "submittedAt": "2026-08-15T10:30:00+09:00",
    "notifyChannels": ["IN_APP", "EMAIL"]
  }
}
```

### 12.6 complaint.status.changed.v1

**발행 시점**
- `RECEIVED -> ASSIGNED`
- `ASSIGNED -> IN_PROGRESS`
- `IN_PROGRESS -> COMPLETED`

**목적**
- Notification이 상태 변경 알림을 생성한다.
- Statistics가 상태별 건수와 완료 집계를 갱신한다.

**Payload**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | `long` | Y | 민원 PK |
| `complaintNo` | `string` | Y | 민원 번호 |
| `applicantUserId` | `long` | Y | 신청자 ID |
| `categoryId` | `long` | Y | 카테고리 ID |
| `categoryCode` | `string` | Y | 카테고리 코드 |
| `previousStatus` | `string \| null` | N | 이전 상태 |
| `currentStatus` | `string` | Y | 변경 후 상태 |
| `assignedDepartmentId` | `long \| null` | N | 배정 부서 ID |
| `assignedOfficerUserId` | `long \| null` | N | 담당 공무원 ID |
| `statusChangedByUserId` | `long \| null` | N | 변경 사용자 ID |
| `statusChangedAt` | `datetime` | Y | 상태 변경 시각 |
| `changeMemo` | `string \| null` | N | 수동 전이 메모 |
| `respondedAt` | `datetime \| null` | N | 완료 전이인 경우 답변 등록 시각 |
| `notifyChannels` | `string[]` | Y | 요청 알림 채널 |

**Example**
```json
{
  "eventId": "f167bfd8-f62d-4473-93dc-d931ed67d997",
  "eventType": "ComplaintStatusChanged",
  "eventVersion": "v1",
  "occurredAt": "2026-08-15T10:30:02+09:00",
  "producer": "complaint-service",
  "partitionKey": "5001",
  "payload": {
    "complaintId": 5001,
    "complaintNo": "CIV-2026-000184",
    "applicantUserId": 101,
    "categoryId": 1,
    "categoryCode": "TRAFFIC",
    "previousStatus": "RECEIVED",
    "currentStatus": "ASSIGNED",
    "assignedDepartmentId": 10,
    "assignedOfficerUserId": 201,
    "statusChangedByUserId": null,
    "statusChangedAt": "2026-08-15T10:30:02+09:00",
    "changeMemo": null,
    "respondedAt": null,
    "notifyChannels": ["IN_APP", "EMAIL"]
  }
}
```

### 12.7 complaint.response.registered.v1

**발행 시점**
- 공무원이 공식 답변을 등록하고 `RESPONSE` 저장이 commit된 직후

**목적**
- Notification이 답변 등록 알림을 생성한다.
- 상태 완료 전이와는 분리해서 "답변 등록" 자체를 독립적으로 식별한다.

**Payload**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | `long` | Y | 대상 민원 ID |
| `complaintNo` | `string` | Y | 민원 번호 |
| `responseId` | `long` | Y | 답변 ID |
| `applicantUserId` | `long` | Y | 신청자 ID |
| `responderUserId` | `long` | Y | 답변 작성 공무원 ID |
| `isPublic` | `boolean` | Y | 공개 여부 |
| `respondedAt` | `datetime` | Y | 답변 등록 시각 |
| `assignedDepartmentId` | `long \| null` | N | 답변 부서 ID |
| `notifyChannels` | `string[]` | Y | 요청 알림 채널 |

**Example**
```json
{
  "eventId": "d07081db-b1e8-4145-93b0-95f047d6fbd7",
  "eventType": "ComplaintResponseRegistered",
  "eventVersion": "v1",
  "occurredAt": "2026-08-15T16:20:00+09:00",
  "producer": "complaint-service",
  "partitionKey": "5001",
  "payload": {
    "complaintId": 5001,
    "complaintNo": "CIV-2026-000184",
    "responseId": 7001,
    "applicantUserId": 101,
    "responderUserId": 201,
    "isPublic": true,
    "respondedAt": "2026-08-15T16:20:00+09:00",
    "assignedDepartmentId": 10,
    "notifyChannels": ["IN_APP", "EMAIL"]
  }
}
```

### 12.8 Consumer 기대 동작

#### 12.8.1 notification-service

| Event | 필수 사용 필드 | 기대 동작 |
|---|---|---|
| `complaint.status.changed.v1` | `applicantUserId`, `currentStatus`, `complaintId`, `complaintNo`, `notifyChannels` | 상태 변경 인앱 알림 생성, `EMAIL` 포함 시 이메일 발송 검토 |
| `complaint.response.registered.v1` | `applicantUserId`, `responseId`, `complaintId`, `complaintNo`, `notifyChannels` | 답변 등록 인앱 알림 생성, 이메일 병행 검토 |

#### 12.8.2 statistics-service

| Event | 필수 사용 필드 | 기대 동작 |
|---|---|---|
| `complaint.created.v1` | `categoryId`, `categoryCode`, `submittedAt` | 신규 접수 건수 증가 |
| `complaint.status.changed.v1` | `previousStatus`, `currentStatus`, `assignedDepartmentId`, `statusChangedAt`, `respondedAt` | 상태별 건수 조정, 완료 건수 및 평균 처리 시간 집계 |

### 12.9 Statistics 집계 규칙 최소안

| 이벤트 | 집계 반영 규칙 |
|---|---|
| `complaint.created.v1` | 해당 날짜 `received_count` 증가 |
| `RECEIVED -> ASSIGNED` | 해당 부서 `assigned_count` 증가 |
| `ASSIGNED -> IN_PROGRESS` | 해당 부서 `in_progress_count` 증가, 필요 시 `assigned_count` 감소 |
| `IN_PROGRESS -> COMPLETED` | 해당 부서 `completed_count` 증가, `avg_processing_time` 계산 반영 |

### 12.10 멱등성과 실패 처리

| 항목 | 규칙 |
|---|---|
| Kafka message key | `complaintId` 문자열 사용 |
| Consumer 멱등 기준 | `eventId` 단위 중복 처리 방지 |
| 재처리 기준 | 동일 `eventId` 재수신 시 무시 |
| 실패 정책 | producer/consumer 재시도 후 DLQ 전송 |
| Outbox | 이번 48시간 범위에서는 미적용, 후속 개선 항목 |

## 13. TASK 6 확정 사항

2026-08-15 기준으로 TASK 6 범위의 아래 항목을 최종 확정한다.

| 항목 | 최종 결정 |
|---|---|
| 상태 변경 이벤트 분리 방식 | `complaint.status.changed.v1` 하나로 통합하고 상태는 payload로 구분한다 |
| 답변 등록 이벤트 별도 발행 여부 | `complaint.response.registered.v1`를 별도 발행한다 |
| `RESPONSE.isPublic` 변경 이벤트 | 이번 48시간 범위에서는 발행하지 않는다 |
| Notification의 `EMAIL` 판단 위치 | `notification-service`가 `notifyChannels`와 사용자 동의를 기준으로 최종 판단한다 |
| Statistics의 상태 카운트 방식 | 전이 이벤트 기반 증감 방식으로 처리한다 |
| Kafka topic / eventType | topic은 소문자 버전형, `eventType`은 PascalCase 이벤트명으로 구분한다 |
| 상태 변경자 필드명 | 이력은 `changedByUserId`, Kafka payload는 `statusChangedByUserId`를 사용한다 |
| `complaintNo` 형식 | `CIV-{yyyy}-{6자리 순번}`을 사용한다 |
| datetime 표기 | ISO 8601 Asia/Seoul 오프셋(`+09:00`)을 기준으로 한다 |

## 14. TASK 6 완료 상태

아래 범위는 이번 문서 기준으로 동결되었다.

1. Kafka 공통 이벤트 envelope
2. `complaint.created.v1` 스키마와 발행 시점
3. `complaint.status.changed.v1` 스키마와 발행 시점
4. `complaint.response.registered.v1` 스키마와 발행 시점
5. `notification-service`, `statistics-service` consumer 기대 동작
6. 멱등 기준, Kafka key, 재시도 및 DLQ 기준
