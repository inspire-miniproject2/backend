# Implementation Plan: Complaint Response Registration API

## Overview
이 문서는 `complaint-service`의 공식 답변 등록 API 구현 계획이다. 이번 작업의 목적은 담당 공무원 또는 관리자가 민원에 대한 공식 답변을 등록하고, 이후 `IN_PROGRESS -> COMPLETED` 상태 전이의 선행 조건을 충족할 수 있도록 최소 실행 흐름을 만드는 것이다. 단순히 `complaint_responses` 테이블에 데이터를 저장하는 것에 그치지 않고, 권한/상태 검증, 이벤트 발행, 후속 상태 변경 API와의 연결 지점을 함께 정리한다.

## Architecture Decisions
- 답변 등록 API는 `complaint-service`가 소유한다.
- 답변 등록은 상태 변경 API와 분리된 별도 커맨드로 구현한다.
- 답변 등록만으로 민원 상태를 자동으로 `COMPLETED`로 바꾸지 않는다.
- 답변이 먼저 등록된 뒤, 별도의 상태 변경 API에서 `IN_PROGRESS -> COMPLETED` 전이를 허용한다.
- 답변 등록 성공 시 Kafka 이벤트 `complaint.response.registered.v1`를 별도 발행한다.
- `isPublic` 변경 이벤트는 이번 범위에서 다루지 않는다.
- 현재 인증 구조상 외부 JWT 완성 전까지는 임시 헤더 기반 사용자 식별을 사용하되, 추후 JWT 기반으로 치환 가능하게 경계를 분리한다.

## Assumptions
- 기준일은 2026-08-18이다.
- 기준 계약은 [api-spec.md](/Users/imhyeon/Projects/minwonon/backend/docs/api-spec.md), [backend-a-domain-contract-freeze.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-domain-contract-freeze.md), [backend-a-b-contract-summary.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-b-contract-summary.md) 를 따른다.
- 대상 API는 `POST /api/v1/officer/complaints/{complaintId}/response` 이다.
- 요청 필드는 `responseContent`, `isPublic` 이며, `responseContent` 길이 제약은 20~3000자다.
- 현재 버전에서는 답변 수정, 답변 삭제, 공개여부 변경 이벤트, 내부 메모, 보완요청 관련 필드는 포함하지 않는다.

## Deliverables
- 답변 등록 API 컨트롤러/DTO/서비스 구현 계획
- `complaint_responses` 저장 규칙 정리
- 답변 등록 전 민원 상태 검증 규칙 정리
- `complaint.response.registered.v1` 발행 시점과 payload 정리
- 테스트 및 수동 검증 시나리오 정리

## Task List

### Phase 1: Domain Guardrails

## Task 1: 답변 등록 도메인 규칙 명시

**Description:** 답변 등록 가능 상태와 금지 상태를 먼저 고정한다. `RECEIVED`, `ASSIGNED` 상태에서는 공식 처리 답변 등록을 막고, 최소 `IN_PROGRESS` 상태에서만 등록하도록 정리하면 이후 완료 처리 규칙과 충돌이 줄어든다. 현재 정책상 한 민원당 공식 답변은 1건으로 고정할지 여부도 구현 전에 분명히 해야 한다.

**Acceptance criteria:**
- [ ] 답변 등록 허용 상태가 문서에 명시된다.
- [ ] 답변 중복 등록 허용 여부가 구현 정책으로 정리된다.
- [ ] `COMPLETED` 전이와의 관계가 문서에 명시된다.

**Verification:**
- [ ] 계약 문서와 계획 문서 간 상태 규칙 충돌이 없는지 확인한다.
- [ ] `COMPLETED 전 RESPONSE 필수` 규칙과 답변 등록 정책이 논리적으로 맞는지 검토한다.

**Dependencies:** None

**Files likely touched:**
- `docs/plans/complaint-response-registration-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Task 2: 기존 Complaint / Response 도메인 모델 점검

**Description:** 현재 `ComplaintResponse` 엔티티와 `complaint_responses` 스키마가 계약 문서와 일치하는지 확인하고, 부족한 컬럼이나 제약이 있으면 구현 전에 보강한다. 핵심 점검 포인트는 `complaint_id`, `responder_user_id`, `response_content`, `is_public`, `responded_at`, `updated_at` 이다.

**Acceptance criteria:**
- [ ] `ComplaintResponse` 엔티티가 최종 필드 구조와 일치한다.
- [ ] `complaint_responses` 스키마가 응답 DTO/이벤트 payload 생성에 충분하다.
- [ ] `complaintId` 기준 조회 및 중복 답변 여부 확인용 저장소 메서드가 준비된다.

**Verification:**
- [ ] 엔티티와 DDL 컬럼명이 일치하는지 확인한다.
- [ ] 저장소에서 `existsByComplaintId(...)` 또는 동등 기능을 제공할 수 있는지 확인한다.

**Dependencies:** Task 1

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/domain/ComplaintResponse.java`
- `complaint-service/src/main/java/com/gcivil/complaint/repository/ComplaintResponseRepository.java`
- `complaint-service/src/main/resources/db/migration/*.sql`

**Estimated scope:** S: 1-3 files

## Checkpoint: After Tasks 1-2
- [ ] 답변 등록 정책이 흔들리지 않게 고정된다.
- [ ] 구현에 필요한 Response 저장 구조가 준비된다.

### Phase 2: API Contract and Command Flow

## Task 3: 답변 등록 요청/응답 DTO와 엔드포인트 스펙 확정

**Description:** `POST /api/v1/officer/complaints/{complaintId}/response` 엔드포인트의 요청/응답 스펙을 코드 레벨 DTO로 고정한다. 요청은 `responseContent`, `isPublic` 중심으로 단순하게 유지하고, 응답은 `responseId`, `complaintId`, `currentStatus`, `respondedAt` 정도를 반환해 후속 상태 변경과 화면 반영에 필요한 최소 정보만 제공한다.

**Acceptance criteria:**
- [ ] 요청 DTO에 `responseContent`, `isPublic`가 포함된다.
- [ ] 요청 DTO validation이 계약 문서와 일치한다.
- [ ] 응답 DTO에 답변 등록 결과를 표현할 최소 필드가 포함된다.

**Verification:**
- [ ] 정상/실패 케이스 예시가 API 문서와 일치하는지 확인한다.
- [ ] validation 오류 메시지가 공통 에러 응답 규격에 맞는지 검토한다.

**Dependencies:** Task 2

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/dto/*`
- `complaint-service/src/main/java/com/gcivil/complaint/web/*`
- `docs/api-spec.md`

**Estimated scope:** S: 2-4 files

## Task 4: 답변 등록 커맨드 서비스 구현

**Description:** 서비스 계층에서 민원 조회, 상태 검증, 중복 답변 검증, 답변 저장을 하나의 트랜잭션으로 처리한다. 민원이 존재하지 않거나, 허용되지 않은 상태이거나, 이미 공식 답변이 등록된 경우 공통 에러 코드로 실패해야 한다.

**Acceptance criteria:**
- [ ] 민원 미존재 시 적절한 예외가 반환된다.
- [ ] 허용되지 않은 상태에서 답변 등록 시 실패한다.
- [ ] 답변 저장 성공 시 `respondedAt`이 기록된다.
- [ ] 중복 답변 금지 정책이라면 두 번째 등록은 실패한다.

**Verification:**
- [ ] 서비스 테스트: 정상 등록 시 DB에 1건 저장된다.
- [ ] 서비스 테스트: 상태 검증 실패 시 저장되지 않는다.
- [ ] 서비스 테스트: 중복 등록 차단 정책이 검증된다.

**Dependencies:** Task 3

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/service/ComplaintResponseCommandService.java`
- `complaint-service/src/main/java/com/gcivil/complaint/repository/ComplaintResponseRepository.java`
- `complaint-service/src/test/java/com/gcivil/complaint/service/*`

**Estimated scope:** M: 3-5 files

## Task 5: 컨트롤러와 공통 예외 응답 연결

**Description:** 답변 등록 API를 컨트롤러에 노출하고, 현재 `complaint-service`의 `ApiResponse`, `GlobalExceptionHandler` 포맷에 맞춰 응답을 통일한다. 임시 인증 구조에서는 최소한 `X-User-Id`를 받아 `responderUserId`로 사용하도록 연결한다.

**Acceptance criteria:**
- [ ] `POST /api/v1/officer/complaints/{complaintId}/response` 엔드포인트가 추가된다.
- [ ] `X-User-Id` 기반으로 응답 등록 사용자 ID가 저장된다.
- [ ] 성공 시 201 Created 응답이 반환된다.

**Verification:**
- [ ] 웹 테스트: 정상 요청 시 201 응답이 내려간다.
- [ ] 웹 테스트: validation 실패 시 공통 에러 포맷이 내려간다.
- [ ] 웹 테스트: 존재하지 않는 민원에 대해 실패 응답이 내려간다.

**Dependencies:** Task 4

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/web/ComplaintResponseCommandController.java`
- `complaint-service/src/test/java/com/gcivil/complaint/web/*`

**Estimated scope:** S: 2-4 files

## Checkpoint: After Tasks 3-5
- [ ] 답변 등록 API의 최소 커맨드 흐름이 동작한다.
- [ ] 후속 상태 변경 API가 참조할 공식 답변 데이터가 저장된다.

### Phase 3: Event Publishing and Integration

## Task 6: complaint.response.registered.v1 이벤트 발행 구현

**Description:** 답변 등록 커밋 이후 `complaint.response.registered.v1` 이벤트를 발행한다. 알림 채널 최종 판단은 `notification-service`가 수행하므로, `complaint-service`는 계약된 핵심 payload를 안정적으로 전달하는 데 집중한다.

**Acceptance criteria:**
- [ ] 답변 등록 성공 후에만 이벤트가 발행된다.
- [ ] 이벤트 payload가 계약 문서 필드와 일치한다.
- [ ] 실패 롤백 시 이벤트가 발행되지 않는다.

**Verification:**
- [ ] 단위 테스트 또는 통합 테스트로 publisher 호출 여부를 검증한다.
- [ ] payload에 `complaintId`, `complaintNo`, `responseId`, `applicantUserId`, `responderUserId`, `isPublic`, `respondedAt`, `assignedDepartmentId`, `notifyChannels`가 포함되는지 확인한다.

**Dependencies:** Task 5

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/event/*`
- `complaint-service/src/main/java/com/gcivil/complaint/service/*`
- `docs/api-spec.md`

**Estimated scope:** M: 3-5 files

## Task 7: COMPLETED 선행 조건과의 연결 지점 정리

**Description:** 이번 작업에서 상태 변경 API까지 한 번에 구현하지 않더라도, 답변 등록 결과를 `ComplaintStatusPolicy` 또는 동등한 정책 객체가 참조할 수 있게 정리한다. 후속 `IN_PROGRESS -> COMPLETED` API 구현 시 `RESPONSE` 존재 여부만 검사하면 되도록 연결 포인트를 만든다.

**Acceptance criteria:**
- [ ] 상태 정책 코드가 답변 존재 여부를 검사할 수 있는 구조가 마련된다.
- [ ] 답변 등록 API와 완료 처리 API의 책임이 분리된다.
- [ ] 후속 구현 시 필요한 TODO 또는 명시적 정책 메서드가 정리된다.

**Verification:**
- [ ] 현재 코드 기준으로 `COMPLETED` 검증 지점이 어디인지 문서화한다.
- [ ] 후속 상태 변경 API 계획과 충돌하지 않는지 확인한다.

**Dependencies:** Task 6

**Files likely touched:**
- `complaint-service/src/main/java/com/gcivil/complaint/service/ComplaintStatusPolicy.java`
- `docs/plans/complaint-response-registration-plan.md`

**Estimated scope:** S: 1-2 files

## Checkpoint: After Tasks 6-7
- [ ] 답변 등록과 Kafka 이벤트 발행이 연결된다.
- [ ] 완료 처리 API 구현을 위한 선행 데이터와 정책 경계가 준비된다.

### Phase 4: Verification

## Task 8: 테스트 및 수동 검증 시나리오 정리

**Description:** Postman 또는 HTTP 클라이언트 기준으로 답변 등록 검증 시나리오를 정리한다. 특히 정상 등록, 상태 불일치, 중복 등록, 존재하지 않는 민원, 이후 `COMPLETED` 가능 여부를 확인할 수 있어야 한다.

**Acceptance criteria:**
- [ ] 수동 테스트 시나리오가 문서화된다.
- [ ] 최소 웹 테스트 또는 서비스 테스트가 추가된다.
- [ ] 로컬 실행 시 필요한 헤더/예시 요청값이 정리된다.

**Verification:**
- [ ] `complaint-service` 실행 후 수동 호출 절차가 재현 가능하다.
- [ ] 테스트 코드가 실패 케이스를 포함한다.

**Dependencies:** Task 7

**Files likely touched:**
- `complaint-service/src/test/java/com/gcivil/complaint/web/*`
- `complaint-service/README.md`
- `docs/plans/complaint-response-registration-plan.md`

**Estimated scope:** S: 2-4 files

## Final Checklist
- [ ] 답변 등록 API 경로와 DTO가 계약 문서와 일치한다.
- [ ] 공식 답변 저장이 트랜잭션으로 처리된다.
- [ ] `complaint.response.registered.v1` 이벤트가 등록 성공 후 발행된다.
- [ ] `COMPLETED 전 RESPONSE 필수` 규칙과 자연스럽게 연결된다.
- [ ] 후속 상태 변경 API 구현을 바로 이어갈 수 있다.

## Open Decisions
- [x] 한 민원당 공식 답변은 1건으로 고정
- [x] 답변 등록 허용 상태는 `ASSIGNED`, `IN_PROGRESS`
- [x] `responderUserId`는 임시 헤더 `X-User-Id`로 수신
