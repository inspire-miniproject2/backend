# G-Civil MSA — Codex Repository Context

기준일: 2026-08-16  
대상 저장소: 이미 Git clone 되어 디렉터리 뼈대가 존재하는 `mini2` 백엔드 저장소  
목적: Codex가 기존 저장소 구조를 보존하면서, 확정 계약과 최신 ERD/API를 기준으로 실제 구현 작업을 수행하도록 안내한다.

---

## 0. 가장 중요한 작업 원칙

이 저장소는 **새 프로젝트를 생성하거나 새 뼈대를 만드는 작업이 아니다.** 이미 서비스 디렉터리, Dockerfile, Compose, CI, scripts, 계약 문서가 존재한다.

Codex는 작업을 시작할 때 다음을 지킨다.

1. 기존 디렉터리/파일을 먼저 읽고 재사용한다.
2. 기존 `docs/contracts/backend-a-b-contract-summary.md`를 A/B 공통 계약의 우선 기준으로 사용한다.
3. API 구현은 `docs/api-spec.md` 및 최신 API 명세 v0.4를 기준으로 하되, 아래 ERD와 충돌하는 항목은 임의로 구현하지 않는다.
4. 서비스 간 DB 직접 접근을 만들지 않는다.
5. 새로운 서비스 디렉터리, 공통 모듈, 테이블을 임의로 추가하기 전에 기존 계약과 저장소 구조를 대조한다.
6. 한 번에 전체 프로젝트를 구현하지 말고, 현재 담당 범위와 의존성을 분석한 뒤 작은 단위로 변경한다.
7. 코드 수정 전 항상 `git status`, 현재 브랜치, 기존 소스 유무를 확인한다.

---

## 1. 현재 저장소 구조

현재 clone 된 저장소에는 다음 뼈대가 이미 존재한다.

```text
mini2/
├─ .github/workflows/ci.yml
├─ docs/
│  ├─ api-spec.md
│  ├─ contracts/
│  │  ├─ backend-a-b-contract-summary.md
│  │  └─ backend-a-domain-contract-freeze.md
│  ├─ plans/
│  │  └─ backend-a-contract-plan.md
│  └─ git-rule.md
├─ discovery-service/
├─ config-service/
├─ gateway-service/
├─ complaint-service/
├─ assignment-service/
├─ notification-service/
├─ infra/
│  ├─ docker-compose.yml
│  └─ docker-compose.apps.yml
├─ scripts/
└─ README.md
```

현재 각 서비스 디렉터리는 Dockerfile/README 중심의 **자리만 준비된 상태**일 수 있으므로, 실제 `build.gradle`, `gradlew`, `src/` 존재 여부를 매 작업 전에 확인한다.

### API/도메인에는 있으나 현재 루트 디렉터리에 없는 서비스

- `user-service`
- `statistics-service`

이 두 서비스가 필요하다는 사실은 API/도메인 계약에 존재하지만, **Codex가 임의로 디렉터리를 생성해서는 안 된다.** 먼저 현재 브랜치와 팀 작업 상황을 확인하고, 생성 요청이 명시된 작업에서만 추가한다.

---

## 2. 문서 우선순위

서로 다른 자료가 충돌하면 아래 순서로 판단한다.

### 2.1 A/B 간 통신 및 공통 계약

최우선:

```text
docs/contracts/backend-a-b-contract-summary.md
```

이 문서에서 확정된 내용:

- Access/Refresh JWT payload
- Gateway 내부 전달 헤더
- `X-Internal-Caller` 내부 호출 규칙
- `complaint-service -> assignment-service` OpenFeign 계약
- Kafka 공통 envelope 및 세 이벤트 스키마
- `COMPLETED` 전 공식 RESPONSE 필수 규칙
- `assignment-service` 표준 명칭
- 별도 `ASSIGNMENT` 테이블 미생성

### 2.2 외부 REST API

기준:

```text
docs/api-spec.md
```

그리고 2026-08-15 기준 최신 API Specification v0.4를 참고한다.

주요 API 그룹:

- Auth API
- Complaint API
- Public Response API
- Officer Workflow API
- Notification API
- Statistics API
- 내부 Assignment API

### 2.3 DB/Entity 구조

아래 최신 ERD를 기준으로 한다.

---

## 3. 최신 ERD 계약

### 3.1 USER SERVICE

#### USER

| 컬럼 | 타입/제약 |
|---|---|
| user_id | bigint PK |
| login_id | string UK |
| password_hash | string |
| name | string |
| email | string UK |
| phone | string |
| role | enum |
| department_id | bigint, logical reference |
| email_notify_agreed | boolean |
| is_active | boolean |
| created_at | datetime |
| updated_at | datetime |

관계상 `DEPARTMENT -> USER`가 표현되어 있으나, MSA에서는 물리 FK가 아니라 서비스 간 **논리 참조 ID**로 취급한다.

### 3.2 ASSIGNMENT SERVICE

#### DEPARTMENT

| 컬럼 | 타입/제약 |
|---|---|
| department_id | bigint PK |
| department_name | string |
| is_active | boolean |
| created_at | datetime |
| updated_at | datetime |

#### COMPLAINT_CATEGORY

| 컬럼 | 타입/제약 |
|---|---|
| category_id | bigint PK |
| category_name | string |
| category_code | string UK |
| created_at | datetime |
| updated_at | datetime |

#### DEPARTMENT_CATEGORY

| 컬럼 | 타입/제약 |
|---|---|
| department_category_id | bigint PK |
| department_id | bigint FK — assignment DB 내부 |
| category_id | bigint FK — assignment DB 내부 |
| officer_user_id | bigint, user-service 논리 참조 |
| is_active | boolean |
| created_at | datetime |
| updated_at | datetime |

별도 `ASSIGNMENT` 테이블을 만들지 않는다.

### 3.3 COMPLAINT SERVICE

#### COMPLAINT

| 컬럼 | 타입/제약 |
|---|---|
| complaint_id | bigint PK |
| complaint_no | string UK |
| applicant_user_id | bigint, user-service 논리 참조 |
| category_id | bigint, assignment-service 논리 참조 |
| assigned_department_id | bigint, assignment-service 논리 참조 |
| assigned_officer_user_id | bigint, user-service 논리 참조 |
| title | string |
| content | text |
| current_status | enum |
| submitted_at | datetime |
| assigned_at | datetime |
| completed_at | datetime |
| created_at | datetime |
| updated_at | datetime |

#### COMPLAINT_ATTACHMENT

| 컬럼 | 타입/제약 |
|---|---|
| attachment_id | bigint PK |
| complaint_id | bigint FK — complaint DB 내부 |
| original_filename | string |
| stored_filename | string |
| file_path | string |
| content_type | string |
| file_size | bigint |
| uploaded_at | datetime |
| created_at | datetime |
| updated_at | datetime |

한 Complaint에는 0개 이상의 Attachment가 존재할 수 있다. 첨부파일 메타데이터는 `complaint-service`가 소유하며, `complaint_id`는 같은 complaint DB 내부의 실제 FK다.

#### COMPLAINT_STATUS_HISTORY

| 컬럼 | 타입/제약 |
|---|---|
| status_history_id | bigint PK |
| complaint_id | bigint FK — complaint DB 내부 |
| previous_status | enum |
| new_status | enum |
| changed_by_user_id | bigint, user-service 논리 참조, 자동 전이 시 null 허용 |
| change_memo | string |
| changed_at | datetime |

#### COMPLAINT_RESPONSE

| 컬럼 | 타입/제약 |
|---|---|
| response_id | bigint PK |
| complaint_id | bigint FK — complaint DB 내부 |
| is_public | boolean |
| responder_user_id | bigint, user-service 논리 참조 |
| response_content | text |
| responded_at | datetime |
| updated_at | datetime |

한 Complaint에는 공식 Response가 0 또는 1개 존재한다.

### 3.4 NOTIFICATION SERVICE

#### NOTIFICATION

| 컬럼 | 타입/제약 |
|---|---|
| notification_id | bigint PK |
| receiver_user_id | bigint, user-service 논리 참조 |
| complaint_id | bigint, complaint-service 논리 참조 |
| notification_type | enum |
| channel | string |
| title | string |
| message | string |
| is_read | boolean |
| read_at | datetime |
| created_at | datetime |

### 3.5 STATISTICS SERVICE

#### DAILY_COMPLAINT_STATISTICS

| 컬럼 | 타입/제약 |
|---|---|
| statistics_id | bigint PK |
| statistic_date | date |
| department_id | bigint, assignment-service 논리 참조 |
| received_count | bigint |
| assigned_count | bigint |
| in_progress_count | bigint |
| completed_count | bigint |
| avg_processing_time | decimal |
| created_at | datetime |
| updated_at | datetime |

Statistics Service는 Complaint DB를 직접 조회하지 않고 Kafka 이벤트 기반으로 집계한다.

---

## 4. 최신 ERD와 API v0.4 정합성

최신 최종 ERD 이미지에는 `COMPLAINT_ATTACHMENT`가 포함되어 있으며, API v0.4의 첨부파일 계약과 일치한다. 따라서 첨부파일은 더 이상 문서 간 충돌 항목이 아니다.

확정 사항:

- `COMPLAINT_ATTACHMENT`는 `complaint-service` 소유 테이블이다.
- `COMPLAINT_ATTACHMENT.complaint_id`는 complaint DB 내부의 실제 FK다.
- 민원 접수에서 `attachmentFiles[]`를 받을 수 있다.
- 민원 상세 조회에서 `attachments[]` 메타데이터를 반환한다.
- `/api/v1/complaints/{complaintId}/attachments/{attachmentId}` 다운로드 API를 지원한다.
- 공개 답변에서는 공개 가능한 첨부파일 목록을 반환할 수 있다.
- 파일의 실제 저장 방식(local volume/object storage 등)은 ERD/API만으로 확정되지 않았으므로 기존 저장소/인프라 계약을 확인하고 임의로 외부 스토리지를 도입하지 않는다.

### 4.1 물리 FK와 서비스 간 logical reference 구분

Codex는 ERD의 선을 그대로 JPA 연관관계/FK로 해석하지 않는다. **같은 서비스 DB 내부 관계만 물리 FK로 구현하고, 다른 서비스 소유 데이터는 ID 기반 logical reference로 유지한다.**

물리 FK 예시:

- `DEPARTMENT_CATEGORY.department_id -> DEPARTMENT.department_id`
- `DEPARTMENT_CATEGORY.category_id -> COMPLAINT_CATEGORY.category_id`
- `COMPLAINT_ATTACHMENT.complaint_id -> COMPLAINT.complaint_id`
- `COMPLAINT_STATUS_HISTORY.complaint_id -> COMPLAINT.complaint_id`
- `COMPLAINT_RESPONSE.complaint_id -> COMPLAINT.complaint_id`

서비스 간 logical reference 예시:

- `USER.department_id -> assignment-service`
- `DEPARTMENT_CATEGORY.officer_user_id -> user-service`
- `COMPLAINT.applicant_user_id -> user-service`
- `COMPLAINT.category_id -> assignment-service`
- `COMPLAINT.assigned_department_id -> assignment-service`
- `COMPLAINT.assigned_officer_user_id -> user-service`
- `COMPLAINT_STATUS_HISTORY.changed_by_user_id -> user-service`
- `COMPLAINT_RESPONSE.responder_user_id -> user-service`
- `NOTIFICATION.receiver_user_id -> user-service`
- `NOTIFICATION.complaint_id -> complaint-service`
- `DAILY_COMPLAINT_STATISTICS.department_id -> assignment-service`

다른 서비스의 Entity를 import하거나 cross-service JPA `@ManyToOne`/DB FK를 생성하지 않는다. 필요한 정보는 Gateway 전달 헤더, OpenFeign, Kafka 이벤트 또는 계약된 API를 사용한다.

---

## 5. 서비스 책임과 통신

### user-service

책임:

- CITIZEN 회원가입
- 로그인
- Access/Refresh JWT 발급
- 로그아웃/Refresh token 정책
- USER와 Role 관리

공개 회원가입은 `CITIZEN`만 허용한다. OFFICER/ADMIN은 내부 운영 방식으로 생성한다.

### gateway-service

책임:

- 외부 API 진입점
- JWT 검증
- 외부에서 임의 전달된 내부 사용자 헤더 제거
- 검증된 JWT에서 내부 헤더 재생성
- `X-Request-Id` 생성/전파
- 서비스 라우팅

내부 전달 헤더:

```text
X-User-Id
X-Login-Id
X-User-Role
X-Department-Id   # null이면 헤더 자체 생략
X-Request-Id
```

각 마이크로서비스는 외부 JWT를 다시 검증하지 않는다. Gateway가 전달한 사용자 메타데이터 + 서비스 자체 데이터로 리소스 권한을 검증한다.

### complaint-service

책임:

- 민원 원본 source of truth
- 접수/조회
- 첨부파일 메타데이터/다운로드
- 상태 머신
- 공식 Response
- Officer workflow
- OpenFeign Assignment 호출
- Kafka Producer

상태 전이:

```text
RECEIVED -> ASSIGNED -> IN_PROGRESS -> COMPLETED
```

규칙:

- 최초 민원 저장은 `RECEIVED`
- 배정 성공 시 `ASSIGNED`
- 배정 business miss/장애/timeout 시 삭제하지 않고 `RECEIVED` 유지
- 모든 실제 상태 변경은 `COMPLAINT_STATUS_HISTORY` 기록
- `IN_PROGRESS -> COMPLETED` 전에 공식 `COMPLAINT_RESPONSE`가 반드시 존재

### assignment-service

책임:

- Department
- ComplaintCategory
- DepartmentCategory mapping
- 자동 배정 판단

내부 API:

```http
POST /api/v1/internal/assignments
X-Internal-Caller: complaint-service
```

별도 Assignment Entity/Table을 만들지 않는다.

### notification-service

책임:

- Complaint 이벤트 consume
- NOTIFICATION 저장
- 알림 목록/읽음 처리 API
- `eventId` 기반 중복 소비 방지
- IN_APP 기본 알림 생성
- EMAIL은 채널 요청/동의 정책에 따라 판단

Complaint DB 직접 조회 금지.

### statistics-service

책임:

- Complaint Kafka 이벤트 consume
- 일별/부서별 집계
- DAILY_COMPLAINT_STATISTICS 관리
- 관리자 통계 조회

Complaint DB 직접 조회 금지.

---

## 6. OpenFeign 고정 계약

Caller:

```text
complaint-service
```

Callee:

```text
assignment-service
```

Endpoint:

```text
POST /api/v1/internal/assignments
```

Request 예시:

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

성공 응답:

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

배정 rule miss는 HTTP 통신 실패가 아닌 business miss다.

```json
{
  "assignmentFound": false,
  "reasonCode": "NO_MATCHED_DEPARTMENT",
  "reasonMessage": "카테고리에 매핑된 활성 부서가 없습니다."
}
```

이 경우 Complaint는 `RECEIVED` 상태를 유지한다.

---

## 7. Kafka 고정 계약

Topic:

```text
complaint.created.v1
complaint.status.changed.v1
complaint.response.registered.v1
```

공통 envelope:

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

규칙:

- Kafka key = `complaintId` 문자열
- Consumer는 `eventId` 기준 멱등 처리
- 재시도 후 DLQ
- 현재 범위에서 Outbox 패턴 미적용
- 이벤트는 DB 상태가 실제 저장된 이후 발행

Consumer:

| Topic | notification-service | statistics-service |
|---|---|---|
| complaint.created.v1 | 필요 시 정책에 따라 | 신규 접수 집계 |
| complaint.status.changed.v1 | 상태 변경 알림 | 상태별 집계 |
| complaint.response.registered.v1 | 답변 등록 알림 | 기본적으로 불필요 |

---

## 8. 현재 저장소에서 Codex가 먼저 확인해야 할 기술 부채/불일치

코드 작성 전에 아래 항목을 검사한다.

### 8.1 CI 서비스명 불일치

현재 `.github/workflows/ci.yml`의 테스트 loop에 `department-service`가 포함되어 있다.

하지만 표준 서비스명과 실제 디렉터리는:

```text
assignment-service
```

이므로 CI 수정 작업에서는 해당 이름을 우선 확인한다.

### 8.2 누락된 user/statistics 서비스

API 및 ERD에는 필요하지만 현재 clone 된 루트 뼈대에는 없다.

Codex는 현재 담당자의 브랜치/작업 계획을 확인하지 않은 채 자동 생성하지 않는다.

### 8.3 README/Compose 명칭

현재 infra에서 Assignment Service 전용 DB가 `department-db`라는 이름으로 존재한다.

이것은 서비스 표준 명칭 `assignment-service`와 다른 DB container 이름일 뿐이므로, 팀이 변경하기 전에는 무조건 rename하지 않는다.

### 8.4 Spring 버전

README는 JDK 17 / Spring Boot 3.x / Spring Cloud compatible version만 정하고 정확한 version pair는 아직 고정하지 않았다.

첫 Spring Boot service 생성 시 임의로 서비스마다 다른 버전을 선택하지 않는다. 팀이 정한 버전이 기존 소스에 있는지 먼저 검색한다.

---

## 9. 현재 역할별 작업 관점

### Backend A — 현재 저장소에서 확인되는 확정 업무

Backend A 문서에서 확정적으로 확인되는 범위는 **공통 도메인/통신 계약 동결**이다.

- 서비스 경계/소유권
- 상태 머신
- Entity canonical naming
- JWT payload
- Feign DTO
- Kafka event schema
- 공통 오류 계약

이 계약은 이미 `backend-a-b-contract-summary.md`로 전달되어 있으므로 Codex는 이를 다시 설계하지 않는다.

실제 특정 service 구현을 Backend A에게 귀속시키는 내용은 현재 문서만으로 확정할 수 없으므로 임의로 담당자를 지정하지 않는다.

### Backend B — 계약 문서에서 직접 연결되는 작업

Backend A 계획 문서가 B의 병렬 작업으로 명시한 영역:

- Gateway
- Kafka Consumer/연동
- Eureka
- Config
- JWT/Gateway 계약 적용
- OpenFeign 연동에 필요한 계약 사용

Notification Service는 Kafka consumer 구현과 직접 연결된다.

### Infra / DevOps — 현재 저장소에 이미 준비된 범위

현재 repository에서 확인되는 인프라 작업:

- Kafka KRaft single node local infra
- MariaDB 서비스별 DB
- Dockerfile
- Docker Compose
- `.env.example`
- start/stop/health-check scripts
- GitHub Actions CI
- commit convention validation

현재 CI/CD는 **CI 기본 구성**까지이며, 운영 CD 대상 서버/registry는 확정되어 있지 않다. Codex는 배포 플랫폼을 임의로 가정하지 않는다.

---

## 10. 권장 구현 순서

Codex가 repository 전체 구현을 요청받았을 때는 아래 dependency 순서를 제안한다.

### Phase 0 — Repository audit

코드 수정 없이 먼저 확인:

```text
- git status / branch
- 각 service의 Gradle/Spring source 존재 여부
- 현재 PR/branch에서 추가된 서비스
- docs 계약과 코드의 불일치
- CI/Compose의 서비스명 불일치
```

### Phase 1 — MSA foundation

1. `config-service`
2. `discovery-service`
3. `gateway-service`
4. 모든 service의 Config/Eureka 등록
5. actuator health

완료 조건:

- Eureka에서 서비스 조회 가능
- Gateway가 discovery 기반으로 route 가능
- Config Server 연결 가능
- `/actuator/health` 정상

### Phase 2 — Gateway authentication

1. JWT 검증 filter
2. Public/Protected route 구분
3. 외부 `X-User-*` 헤더 제거
4. 내부 인증 헤더 생성
5. `X-Request-Id` 전파
6. 401/403/error envelope 확인

### Phase 3 — Assignment + Complaint synchronous flow

1. Assignment Entity/Repository/Service
2. Internal Assignment API
3. Complaint Entity/Attachment/StatusHistory/Response
4. Complaint 생성
5. OpenFeign client
6. 배정 성공 → ASSIGNED
7. business miss/timeout → RECEIVED 유지

### Phase 4 — Kafka

1. Event envelope 공통 모델
2. Complaint Producer
3. Notification Consumer
4. `eventId` idempotency
5. retry/DLQ
6. 상태/답변 이벤트 통합 테스트

### Phase 5 — Officer workflow

1. 담당 민원 조회
2. ASSIGNED -> IN_PROGRESS
3. Response 등록
4. Response 없는 COMPLETED 차단
5. IN_PROGRESS -> COMPLETED
6. 상태 이력/Event 검증

### Phase 6 — Statistics

`statistics-service` 디렉터리의 팀 합의/생성 여부 확인 후 진행한다.

1. event consumer
2. 일별 집계
3. 부서별 상태 카운트
4. avg processing time
5. Admin 조회 API

### Phase 7 — API completion

- Public response
- Notification read API
- Auth API의 남은 기능
- 첨부파일 업로드/다운로드 및 메타데이터 계약 검증
- API spec과 실제 response 검증

---

## 11. Codex가 코드를 작성할 때의 완료 기준

작업 단위마다 반드시 다음을 확인한다.

- 기존 파일을 불필요하게 삭제/재생성하지 않았는가
- 각 서비스가 자기 DB만 접근하는가
- API endpoint/DTO가 계약 문서와 일치하는가
- 상태 전이 규칙을 우회하지 않는가
- Gateway 외 서비스에서 JWT 중복 검증을 만들지 않았는가
- Kafka key가 complaintId 문자열인가
- consumer의 eventId 멱등성 처리가 있는가
- Feign business miss와 network error를 구분하는가
- 테스트가 추가되었는가
- `./gradlew test`가 해당 서비스에서 통과하는가
- Compose/CI 변경 시 `docker compose ... config --quiet` 검증이 되는가

---

## 12. Codex 최초 분석 프롬프트

다음 프롬프트로 작업을 시작한다.

```text
이 repository는 이미 팀이 clone하여 사용하는 G-Civil MSA backend 저장소다.
새로운 프로젝트 뼈대를 만들지 말고 기존 구조를 보존해.

먼저 다음 파일을 읽어:
- README.md
- docs/api-spec.md
- docs/contracts/backend-a-b-contract-summary.md
- docs/contracts/backend-a-domain-contract-freeze.md
- docs/plans/backend-a-contract-plan.md
- infra/docker-compose.yml
- infra/docker-compose.apps.yml
- .github/workflows/ci.yml
- 각 service README와 현재 source tree

그 후 코드는 수정하지 말고 다음을 보고해:
1. 현재 구현되어 있는 것과 빈 뼈대만 있는 것
2. API/ERD/contract 기준으로 아직 필요한 것
3. 계약과 현재 저장소의 불일치
4. 내가 맡은 작업에 필요한 선행 dependency
5. 변경해야 할 파일 후보와 구현 순서

중요 규칙:
- backend-a-b-contract-summary.md의 A/B 계약을 재설계하지 말 것
- 서비스 간 DB 직접 접근 금지
- 별도 ASSIGNMENT 테이블 생성 금지
- Complaint 상태의 source of truth는 complaint-service
- JWT는 Gateway에서 검증하고 내부 서비스는 X-User-* 헤더를 사용
- COMPLAINT_ATTACHMENT는 최신 ERD/API에 따라 complaint-service 소유로 구현하되, 실제 파일 저장 인프라는 기존 저장소 계약을 먼저 확인할 것
- user-service/statistics-service 디렉터리가 없으면 임의 생성하지 말고 먼저 보고할 것
```

---

## 13. 현재 팀이 따로 확정해야 하는 항목

Codex가 추측해서 결정하지 않는다.

1. 첨부파일 실제 바이너리 저장 방식(local volume/object storage 등)과 운영 환경별 저장 경로 정책
2. `user-service` 디렉터리를 누가 언제 생성할지
3. `statistics-service` 디렉터리를 누가 언제 생성할지
4. Spring Boot / Spring Cloud 정확한 버전 조합
5. 운영 배포 환경 및 registry
6. Backend A/B의 최종 서비스 구현 ownership — 계약 문서만으로는 일부만 확인 가능

