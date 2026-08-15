# Backend A-B Contract Summary

기준일: 2026-08-15  
목적: 백엔드 A와 백엔드 B가 즉시 구현에 착수할 수 있도록, 현재까지 확정된 공통 계약만 빠르게 공유한다.  
기준 문서:
- `/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-domain-contract-freeze.md`
- `/Users/imhyeon/Projects/minwonon/backend/docs/api-spec.md`

## 1. 합의 범위

이번 문서는 아래 항목의 A/B 간 공통 계약만 요약한다.

- JWT payload와 Gateway 내부 헤더 전달 규칙
- `POST /api/v1/internal/assignments` Feign DTO
- Kafka 이벤트 `complaint.created.v1`, `complaint.status.changed.v1`, `complaint.response.registered.v1`
- `COMPLETED` 전 `RESPONSE` 필수 규칙

## 2. JWT Payload / Gateway 헤더 규칙

### 2.1 Access Token payload

| Claim | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sub` | string | Y | 사용자 식별자 문자열 |
| `userId` | long | Y | 내부 사용자 PK |
| `loginId` | string | Y | 로그인 ID |
| `role` | string | Y | `CITIZEN`, `OFFICER`, `ADMIN` |
| `departmentId` | long \| null | Y | 공무원 소속 부서 ID, 민원인은 `null` |
| `tokenType` | string | Y | `ACCESS` |
| `iat` | epoch-seconds | Y | 발급 시각 |
| `exp` | epoch-seconds | Y | 만료 시각 |

### 2.2 Refresh Token payload

| Claim | 타입 | 필수 | 설명 |
|---|---|---|---|
| `sub` | string | Y | 사용자 식별자 문자열 |
| `userId` | long | Y | 내부 사용자 PK |
| `tokenType` | string | Y | `REFRESH` |
| `iat` | epoch-seconds | Y | 발급 시각 |
| `exp` | epoch-seconds | Y | 만료 시각 |

### 2.3 Gateway 내부 전달 헤더

Gateway는 외부 JWT를 검증한 뒤 아래 헤더만 내부 서비스로 전달한다.

| Header | 타입 | 필수 | 설명 |
|---|---|---|---|
| `X-User-Id` | string | Y | JWT의 `userId` |
| `X-Login-Id` | string | Y | JWT의 `loginId` |
| `X-User-Role` | string | Y | JWT의 `role` |
| `X-Department-Id` | string | N | JWT의 `departmentId`, 값이 없으면 헤더 미전달 |
| `X-Request-Id` | string | Y | 요청 추적 ID |

### 2.4 내부 서비스 호출 규칙

- 내부 서비스 간 호출 인증은 `X-Internal-Caller` 헤더 + 내부 네트워크 격리를 전제로 한다.
- `X-Department-Id` 값이 없으면 빈 문자열을 보내지 않고, 헤더 자체를 생략한다.

## 3. Feign 계약

### 3.1 대상 서비스

- 표준 서비스명: `assignment-service`
- 별도 `ASSIGNMENT` 테이블은 만들지 않는다.
- `assignment-service`는 `DEPARTMENT`, `COMPLAINT_CATEGORY`, `DEPARTMENT_CATEGORY` 기준으로 자동 배정을 판단한다.

### 3.2 내부 자동 배정 API

| 항목 | 값 |
|---|---|
| Method | `POST` |
| Endpoint | `/api/v1/internal/assignments` |
| Caller | `complaint-service` |
| Callee | `assignment-service` |

### 3.3 Request DTO

```json
{
  "complaintId": 1001,
  "complaintNo": "CIVIL-20260815-0001",
  "categoryId": 10,
  "categoryCode": "ROAD",
  "applicantUserId": 501,
  "submittedAt": "2026-08-15T09:30:00Z"
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `complaintId` | long | Y | 민원 PK |
| `complaintNo` | string | Y | 사용자 노출용 민원 번호 |
| `categoryId` | long | Y | 민원 카테고리 ID |
| `categoryCode` | string | Y | 카테고리 코드 |
| `applicantUserId` | long | Y | 민원 신청자 ID |
| `submittedAt` | datetime | Y | 민원 접수 시각 |

### 3.4 Success Response DTO

```json
{
  "assignmentFound": true,
  "departmentId": 21,
  "departmentName": "도로관리과",
  "officerUserId": 9001,
  "officerName": "홍길동",
  "assignmentRuleId": 301,
  "assignedAt": "2026-08-15T09:30:02Z"
}
```

### 3.5 매핑 실패 Response DTO

```json
{
  "assignmentFound": false,
  "reasonCode": "NO_MATCHED_DEPARTMENT",
  "reasonMessage": "카테고리에 매핑된 활성 부서가 없습니다."
}
```

### 3.6 Feign 처리 정책

- 자동 배정 실패는 예외가 아니라 business miss로 처리한다.
- 이 경우 `complaint-service`는 민원을 삭제하지 않고 `RECEIVED` 상태를 유지한다.
- 배정 성공 시에만 `assignedDepartmentId`, `assignedOfficerUserId`, `assignedAt`를 채우고 `ASSIGNED` 상태로 전이한다.

## 4. Kafka 이벤트 계약

### 4.1 사용 이벤트

- `complaint.created.v1`
- `complaint.status.changed.v1`
- `complaint.response.registered.v1`

### 4.2 공통 Envelope

```json
{
  "eventId": "uuid",
  "eventType": "complaint.status.changed.v1",
  "eventVersion": "v1",
  "occurredAt": "2026-08-15T09:31:00Z",
  "producer": "complaint-service",
  "partitionKey": "1001",
  "payload": {}
}
```

| 필드 | 설명 |
|---|---|
| `eventId` | 이벤트 유일 식별자 |
| `eventType` | 이벤트 이름 |
| `eventVersion` | 스키마 버전 |
| `occurredAt` | 이벤트 발생 시각 |
| `producer` | 발행 서비스명 |
| `partitionKey` | Kafka key와 동일, `complaintId` 문자열 |
| `payload` | 이벤트별 실제 데이터 |

### 4.3 complaint.created.v1 payload

```json
{
  "complaintId": 1001,
  "complaintNo": "CIVIL-20260815-0001",
  "applicantUserId": 501,
  "categoryId": 10,
  "categoryCode": "ROAD",
  "currentStatus": "RECEIVED",
  "submittedAt": "2026-08-15T09:30:00Z",
  "notifyChannels": [
    "IN_APP",
    "EMAIL"
  ]
}
```

### 4.4 complaint.status.changed.v1 payload

```json
{
  "complaintId": 1001,
  "complaintNo": "CIVIL-20260815-0001",
  "previousStatus": "RECEIVED",
  "newStatus": "ASSIGNED",
  "changedByUserId": null,
  "departmentId": 21,
  "officerUserId": 9001,
  "changedAt": "2026-08-15T09:31:00Z"
}
```

### 4.5 complaint.response.registered.v1 payload

```json
{
  "complaintId": 1001,
  "complaintNo": "CIVIL-20260815-0001",
  "responseId": 7001,
  "responderUserId": 9001,
  "isPublic": true,
  "respondedAt": "2026-08-15T10:00:00Z"
}
```

### 4.6 이벤트 처리 정책

- 신규 민원 접수 시 `complaint.created.v1`를 발행한다.
- 상태 변경 이벤트는 `complaint.status.changed.v1` 하나로 통합한다.
- 답변 등록은 `complaint.response.registered.v1` 별도 이벤트로 발행한다.
- `isPublic` 변경 이벤트는 이번 범위에서 다루지 않는다.
- 이메일 전송 여부는 `notification-service`가 `notifyChannels` 정책과 사용자 수신 동의를 기준으로 판단한다.
- `statistics-service`는 상태 전이 이벤트를 기준으로 건수 증감 처리한다.
- Kafka key는 `complaintId` 문자열을 사용한다.
- 자동 처리인 경우 `changedByUserId`는 `null` 허용이다.

## 5. COMPLETED 전 RESPONSE 필수 규칙

### 5.1 상태 전이 규칙

허용 상태 전이는 아래와 같다.

- `RECEIVED -> ASSIGNED`
- `ASSIGNED -> IN_PROGRESS`
- `IN_PROGRESS -> COMPLETED`

### 5.2 완료 조건

- `COMPLETED` 전이는 반드시 공식 답변 `RESPONSE`가 먼저 등록되어 있어야 한다.
- 답변 없이 `COMPLETED` 처리하는 것은 허용하지 않는다.
- 이 검증의 최종 책임은 `complaint-service`가 가진다.

### 5.3 자동 배정 실패 시 처리

- 민원 접수 저장은 성공했지만 자동 배정이 실패하거나 타임아웃되면 상태는 `RECEIVED`를 유지한다.
- 즉, 자동 배정은 "민원 접수 직후 카테고리 매핑 규칙에 따라 담당 부서/공무원을 찾는 내부 처리"를 뜻한다.

## 6. 확정된 추가 결정

| 항목 | 최종 결정 |
|---|---|
| 표준 서비스명 | `assignment-service` |
| 별도 `ASSIGNMENT` 테이블 | 생성하지 않음 |
| `complaintNo` | 유지 |
| `departmentId` | JWT payload 및 내부 헤더에 포함 |
| `changedByUserId` | 자동 상태 전이에서 `null` 허용 |

## 7. 현재 시점 메모

- 지금 구현 착수 기준으로 필수 추가 결정사항은 없다.
- 세부 `reasonCode` 확장, timestamp 세부 표기 고정, 코드 상수 위치 같은 항목은 구현 단계에서 정리해도 된다.
