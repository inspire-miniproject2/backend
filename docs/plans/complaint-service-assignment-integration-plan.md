# Implementation Plan: Complaint Service Assignment Integration

## Overview
이 문서는 `complaint-service`가 민원 접수 직후 `assignment-service`를 동기 호출하도록 연동하는 구현 계획이다. 현재 `complaint-service`는 Spring Boot 골격과 일부 Flyway DDL만 있는 초기 상태이므로, 이번 작업은 단순 Feign 연결만이 아니라 민원 저장, 자동 배정 반영, 상태 이력 기록, 후속 Kafka 이벤트 발행 준비까지 한 번에 이어지는 최소 실행 흐름을 만드는 것을 목표로 한다.

## Architecture Decisions
- `complaint-service`는 민원 상태의 source of truth이며, 배정 성공 여부에 따라 `COMPLAINT.currentStatus`를 직접 관리한다.
- `assignment-service`는 `POST /api/v1/internal/assignments`로만 호출하고, 배정 성공/실패 판단 결과만 반환받는다.
- 민원은 항상 먼저 `RECEIVED`로 저장하고, 배정 성공 시 같은 처리 흐름 안에서 `ASSIGNED`로 전이한다.
- 배정 실패(`assignmentFound=false`) 또는 연동 장애/타임아웃 시 민원은 삭제하지 않고 `RECEIVED` 상태로 유지한다.
- 상태 변경 이력은 `COMPLAINT_STATUS_HISTORY`에 남기며, 자동 배정으로 인한 전이는 `changedByUserId=null`을 허용한다.
- `complaint.created.v1`는 민원 최초 저장 후 발행하고, `complaint.status.changed.v1`는 `RECEIVED -> ASSIGNED` 같은 상태 전이 때 발행한다.
- JWT/로그인 기능은 이번 작업 범위에서 제외하고, 우선 내부 연동과 도메인 흐름을 완성한다.

## Assumptions
- 기준일은 2026-08-18이다.
- 기준 계약은 [api-spec.md](/Users/imhyeon/Projects/minwonon/backend/docs/api-spec.md), [backend-a-domain-contract-freeze.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-domain-contract-freeze.md), [backend-a-b-contract-summary.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-b-contract-summary.md) 를 따른다.
- `assignment-service`와 `user-service`의 내부 API는 이미 구현되어 있으며, `complaint-service`가 다음 호출 계약을 사용한다.
- 현재 `complaint-service`의 Flyway DDL은 최종 ERD보다 단순하므로, 이번 계획에는 스키마 보강 작업이 포함된다.

## Deliverables
- `complaint-service` JPA/Flyway/Feign/Kafka 기본 런타임 설정
- `COMPLAINT`, `COMPLAINT_ATTACHMENT`, `COMPLAINT_STATUS_HISTORY`, `COMPLAINT_RESPONSE` 기준 도메인/스키마 정비
- 민원 접수 API의 최소 저장 흐름
- `assignment-service` 연동용 Feign client 및 DTO
- 배정 성공/실패 반영 서비스 로직
- 상태 이력 기록 로직
- `complaint.created.v1`, `complaint.status.changed.v1` 발행 지점 설계 또는 최소 구현
- 테스트와 로컬 실행 문서

## Task List

### Phase 1: Foundation

## Task 1: complaint-service 런타임 의존성과 환경 설정 정리

**Description:** `complaint-service`에 민원 접수 및 내부 연동 구현에 필요한 JPA, Validation, OpenFeign, Kafka, MySQL/H2 런타임 의존성을 추가한다. 또한 `local`과 `dev` 프로필을 분리해 로컬에서는 H2 또는 팀 합의 로컬 DB로 빠르게 개발하고, `dev`에서는 실제 `infra/docker-compose.yml` 기준 MySQL 연결을 쓰도록 정리한다.

**Acceptance criteria:**
- [ ] `complaint-service`에 JPA, Validation, OpenFeign, Kafka 관련 의존성이 추가된다.
- [ ] `application.yml`, `application-local.yml`, `application-dev.yml` 구조가 분리된다.
- [ ] Config Server/Eureka 없이도 테스트와 로컬 부팅이 가능하다.

**Verification:**
- [ ] 애플리케이션 부팅 확인: `complaint-service`가 로컬 프로필로 실행된다.
- [ ] 설정 검토: 서비스명, 포트, datasource 키가 팀 표준과 일치한다.
- [ ] 테스트 부팅 확인: 컨텍스트 테스트가 외부 인프라 없이 통과한다.

**Dependencies:** None

**Files likely touched:**
- `complaint-service/build.gradle`
- `complaint-service/src/main/resources/application.yml`
- `complaint-service/src/main/resources/application-local.yml`
- `complaint-service/src/main/resources/application-dev.yml`
- `complaint-service/src/test/resources/application-test.yml`

**Estimated scope:** M: 3-5 files

## Task 2: complaint DB 스키마를 최종 계약 기준으로 보강

**Description:** 현재 DDL은 `complaints`, `complaint_responses`만 일부 정의되어 있어 실제 계약과 차이가 있다. `COMPLAINT`, `COMPLAINT_ATTACHMENT`, `COMPLAINT_STATUS_HISTORY`, `COMPLAINT_RESPONSE` 구조를 최종 계약에 맞게 보강하고, `complaintNo`, `categoryCode`, 상태값, 배정 필드, 완료 시각, 생성/수정 시각을 확정한다.

**Acceptance criteria:**
- [ ] `complaints` 테이블이 `complaintNo`, `title`, `content`, `currentStatus`, `submittedAt`, `assignedAt`, `completedAt`, `createdAt`, `updatedAt`를 포함한다.
- [ ] `complaint_attachments`, `complaint_status_history`, `complaint_responses` 테이블이 추가 또는 보강된다.
- [ ] `COMPLETED` 전 `RESPONSE` 필요 규칙을 표현할 수 있는 데이터 구조가 준비된다.

**Verification:**
- [ ] Flyway migration 적용 확인: 로컬 DB에 4개 핵심 테이블이 생성된다.
- [ ] 스키마 검토: 컬럼명과 nullable 규칙이 계약 문서와 일치한다.
- [ ] 수기 확인: `changed_by_user_id`는 null 허용으로 설계된다.

**Dependencies:** Task 1

**Files likely touched:**
- `complaint-service/src/main/resources/db/migration/V1__create_complaint_schema.sql`
- `complaint-service/src/main/resources/db/migration/V2__...sql`
- `docs/api-spec.md`

**Estimated scope:** M: 3-5 files

## Task 3: Complaint 도메인 엔티티와 저장소 구현

**Description:** 보강된 스키마를 기준으로 민원 aggregate와 필요한 하위 엔티티를 코드에 매핑한다. 핵심은 `Complaint`, `ComplaintResponse`, `ComplaintStatusHistory`, `ComplaintAttachment` 엔티티와 상태 enum을 정리하고, 민원 저장과 상태 전이를 서비스 계층에서 다룰 수 있게 기반을 만드는 것이다.

**Acceptance criteria:**
- [ ] `ComplaintStatus` enum이 `RECEIVED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`로 고정된다.
- [ ] 핵심 엔티티와 저장소가 최종 DDL과 일치하게 매핑된다.
- [ ] 민원 저장, 상태 이력 저장, 답변 존재 여부 조회를 위한 저장소 메서드가 준비된다.

**Verification:**
- [ ] JPA 매핑 검토: 엔티티 필드와 컬럼명이 맞는다.
- [ ] 저장소 테스트: 민원과 상태 이력이 저장된다.
- [ ] 수기 확인: 상태 enum 문자열이 계약 문서 표기와 일치한다.

**Dependencies:** Task 2

**Files likely touched:**
- `complaint-service/src/main/java/.../domain/Complaint.java`
- `complaint-service/src/main/java/.../domain/ComplaintResponse.java`
- `complaint-service/src/main/java/.../domain/ComplaintStatusHistory.java`
- `complaint-service/src/main/java/.../domain/ComplaintAttachment.java`
- `complaint-service/src/main/java/.../repository/...`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 1-3
- [ ] `complaint-service`가 DB와 함께 부팅된다.
- [ ] complaint DB 구조가 최종 계약에 맞게 정리된다.
- [ ] 민원 aggregate와 상태 이력 엔티티가 준비된다.
- [ ] assignment 연동을 올릴 최소 데이터 기반이 갖춰진다.

### Phase 2: Complaint Create Flow

## Task 4: 민원 접수 요청/응답 DTO와 기본 생성 API 구현

**Description:** 시민 민원 접수의 최소 경로를 만든다. 우선 `POST /api/v1/complaints`에 대해 제목, 내용, 카테고리, 첨부 메타데이터를 받아 민원을 `RECEIVED` 상태로 저장하고 `complaintId`, `complaintNo`, `currentStatus`를 반환하는 기본 흐름을 구현한다.

**Acceptance criteria:**
- [ ] 민원 생성 API가 기본 요청값 검증과 함께 구현된다.
- [ ] 저장 직후 민원은 항상 `RECEIVED` 상태다.
- [ ] 응답에 `complaintId`, `complaintNo`, `currentStatus`, `submittedAt`가 포함된다.

**Verification:**
- [ ] API 테스트: 정상 생성 시 201 Created 응답이 내려간다.
- [ ] API 테스트: 필수값 누락 시 validation 오류가 내려간다.
- [ ] 수기 확인: DB에 민원 본문과 현재 상태가 저장된다.

**Dependencies:** Task 3

**Files likely touched:**
- `complaint-service/src/main/java/.../web/ComplaintCommandController.java`
- `complaint-service/src/main/java/.../dto/CreateComplaintRequest.java`
- `complaint-service/src/main/java/.../dto/CreateComplaintResponse.java`
- `complaint-service/src/main/java/.../service/ComplaintCommandService.java`

**Estimated scope:** M: 3-5 files

## Task 5: 민원 번호 생성 규칙과 초기 상태 이력 기록 구현

**Description:** 민원 저장과 함께 `complaintNo` 생성 규칙 `CIV-{yyyy}-{6자리 순번}` 을 적용하고, 최초 상태 `RECEIVED`에 대한 상태 이력을 남긴다. 이후 자동 배정 연동과 이벤트 발행이 이 정보를 재사용할 수 있게 한다.

**Acceptance criteria:**
- [ ] `complaintNo`가 연도 포함 규칙으로 생성된다.
- [ ] 최초 저장 시 `previousStatus=null`, `newStatus=RECEIVED` 이력이 기록된다.
- [ ] 자동 전이와 수동 전이를 구분할 수 있도록 상태 이력 구조가 준비된다.

**Verification:**
- [ ] 서비스 테스트: 연속 생성 시 번호가 중복 없이 발급된다.
- [ ] 저장소 테스트: 최초 상태 이력이 생성된다.
- [ ] 수기 확인: `changedByUserId`는 최초 접수 기준 적절히 저장된다.

**Dependencies:** Task 4

**Files likely touched:**
- `complaint-service/src/main/java/.../service/ComplaintNumberGenerator.java`
- `complaint-service/src/main/java/.../service/ComplaintCommandService.java`
- `complaint-service/src/test/java/.../service/ComplaintCommandServiceTest.java`

**Estimated scope:** S: 1-2 files

## Checkpoint: After Tasks 4-5
- [ ] 시민 민원 접수의 최소 생성 경로가 동작한다.
- [ ] 민원은 `RECEIVED` 상태로 저장된다.
- [ ] `complaintNo`와 최초 상태 이력이 함께 남는다.

### Phase 3: Assignment Integration

## Task 6: assignment-service Feign client와 내부 연동 DTO 구현

**Description:** `complaint-service`에서 `assignment-service`를 호출할 Feign client를 추가한다. 합의된 요청/응답 DTO와 내부 헤더 `X-Internal-Caller: complaint-service`, `X-Request-Id` 전달 규칙을 그대로 반영한다.

**Acceptance criteria:**
- [ ] `POST /api/v1/internal/assignments` Feign client가 구현된다.
- [ ] 요청 DTO가 `complaintId`, `complaintNo`, `categoryId`, `categoryCode`, `applicantUserId`, `submittedAt`를 포함한다.
- [ ] 응답 DTO가 성공/미매핑 케이스를 모두 표현한다.

**Verification:**
- [ ] 단위 테스트: Feign 요청 헤더와 body가 계약과 일치한다.
- [ ] 수기 검토: DTO 필드명이 `assignment-service` 구현과 정확히 맞는다.
- [ ] 설정 검토: local/dev 환경에서 `assignment-service` 주소를 주입할 수 있다.

**Dependencies:** Task 5

**Files likely touched:**
- `complaint-service/src/main/java/.../client/AssignmentServiceClient.java`
- `complaint-service/src/main/java/.../client/dto/AssignmentRequest.java`
- `complaint-service/src/main/java/.../client/dto/AssignmentResponse.java`
- `complaint-service/src/main/resources/application.yml`

**Estimated scope:** M: 3-5 files

## Task 7: 민원 생성 후 자동 배정 반영 서비스 로직 구현

**Description:** 민원 저장 직후 `assignment-service`를 호출하고, 응답에 따라 민원 상태와 배정 정보를 갱신한다. 배정 성공이면 `assignedDepartmentId`, `assignedOfficerUserId`, `assignedAt`를 저장하고 `ASSIGNED`로 전이하며, 실패면 `RECEIVED`를 유지한다. 연동 장애나 타임아웃도 동일하게 `RECEIVED` 유지 정책을 따른다.

**Acceptance criteria:**
- [ ] 배정 성공 시 민원에 부서/담당자/배정시각이 저장되고 상태가 `ASSIGNED`로 바뀐다.
- [ ] 배정 실패 또는 규칙 미존재 시 민원은 `RECEIVED` 상태를 유지한다.
- [ ] 연동 장애 시 외부 민원 생성 API는 성공 응답을 유지하되 내부적으로 `RECEIVED` 상태로 남는다.

**Verification:**
- [ ] 서비스 테스트: 배정 성공 시 상태와 배정 필드가 반영된다.
- [ ] 서비스 테스트: `assignmentFound=false`면 `RECEIVED` 유지가 검증된다.
- [ ] 서비스 테스트: Feign 예외 발생 시 민원이 삭제되지 않고 유지된다.

**Dependencies:** Task 6

**Files likely touched:**
- `complaint-service/src/main/java/.../service/ComplaintCommandService.java`
- `complaint-service/src/main/java/.../service/AssignmentIntegrationService.java`
- `complaint-service/src/test/java/.../service/ComplaintAssignmentIntegrationTest.java`

**Estimated scope:** M: 3-5 files

## Task 8: 자동 배정 상태 이력 기록과 COMPLETED 선행 규칙 기반 정합성 정리

**Description:** 배정 성공 시 `RECEIVED -> ASSIGNED` 상태 이력을 추가 기록하고, 이후 작업을 위해 `COMPLETED 전 RESPONSE 필요` 규칙을 도메인 규칙으로 명시한다. 이번 연동 작업의 직접 범위는 생성 직후 배정까지이지만, 이후 상태 변경 API 구현이 흔들리지 않도록 선행 규칙을 서비스 레벨에 정리한다.

**Acceptance criteria:**
- [ ] 배정 성공 시 `previousStatus=RECEIVED`, `newStatus=ASSIGNED` 이력이 기록된다.
- [ ] 자동 전이 이력의 `changedByUserId`는 null 허용으로 저장된다.
- [ ] `COMPLETED` 전 `RESPONSE` 필요 규칙이 도메인 서비스 또는 검증 메서드로 문서화된다.

**Verification:**
- [ ] 저장소 테스트: 자동 배정 성공 시 두 번째 상태 이력이 남는다.
- [ ] 수기 확인: 자동 전이에 사용자 ID가 강제되지 않는다.
- [ ] 코드 검토: 완료 전 답변 필요 규칙이 후속 API에서 재사용 가능하게 분리되어 있다.

**Dependencies:** Task 7

**Files likely touched:**
- `complaint-service/src/main/java/.../service/ComplaintStatusHistoryService.java`
- `complaint-service/src/main/java/.../domain/ComplaintStatusPolicy.java`
- `complaint-service/src/test/java/.../service/ComplaintStatusHistoryServiceTest.java`

**Estimated scope:** S: 1-2 files

## Checkpoint: After Tasks 6-8
- [ ] `complaint-service -> assignment-service` 동기 연동이 붙는다.
- [ ] 배정 성공/실패/장애 분기가 모두 정리된다.
- [ ] 상태 이력이 자동 전이까지 포함해 누적된다.
- [ ] 이후 officer 처리 API 구현을 위한 상태 규칙 기반이 준비된다.

### Phase 4: Event Publishing and Readiness

## Task 9: complaint.created.v1 / complaint.status.changed.v1 발행 지점 구현

**Description:** 민원 저장과 상태 전이 시점에 Kafka 이벤트를 발행한다. 이번 단계에서는 최소한 `complaint.created.v1`과 자동 배정 성공 시의 `complaint.status.changed.v1`를 정확한 payload로 만들고, 향후 `complaint.response.registered.v1` 확장을 고려한 공통 envelope 구조를 정리한다.

**Acceptance criteria:**
- [ ] 민원 최초 저장 후 `complaint.created.v1`가 발행된다.
- [ ] 자동 배정 성공으로 상태가 `ASSIGNED` 되면 `complaint.status.changed.v1`가 발행된다.
- [ ] envelope와 payload 필드가 계약 문서와 일치한다.

**Verification:**
- [ ] 단위 테스트: 이벤트 payload에 `complaintId`, `complaintNo`, 상태, 카테고리, 사용자 정보가 포함된다.
- [ ] 수기 검토: Kafka topic 이름과 `eventType`이 구분되어 사용된다.
- [ ] 로컬 로그 확인: 이벤트 발행 지점이 저장/전이 성공 이후에만 호출된다.

**Dependencies:** Task 8

**Files likely touched:**
- `complaint-service/src/main/java/.../event/...`
- `complaint-service/src/main/java/.../service/ComplaintEventPublisher.java`
- `complaint-service/src/test/java/.../event/ComplaintEventPublisherTest.java`

**Estimated scope:** M: 3-5 files

## Task 10: 통합 테스트와 로컬 실행 문서 마무리

**Description:** 민원 생성부터 자동 배정 반영까지의 로컬 통합 시나리오를 문서화하고 검증한다. `user-service`, `assignment-service`, `complaint-service` 실행 순서와 샘플 요청/예상 응답을 남겨서 다음 구현 단계에서 바로 재사용할 수 있게 한다.

**Acceptance criteria:**
- [ ] 로컬 실행 순서가 문서화된다.
- [ ] 민원 생성 성공, 배정 성공, 배정 실패 예시가 정리된다.
- [ ] 핵심 테스트 또는 수기 시나리오가 문서 기준으로 재현 가능하다.

**Verification:**
- [ ] 문서 검토: 새 팀원이 보고 바로 연동 테스트를 시도할 수 있다.
- [ ] 수기 테스트: 샘플 요청 2종 이상이 기대 상태로 저장된다.
- [ ] README 또는 plan 문서에 환경 변수와 선행 실행 서비스가 정리된다.

**Dependencies:** Task 9

**Files likely touched:**
- `complaint-service/README.md`
- `docs/plans/complaint-service-assignment-integration-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Checkpoint: Complete
- [ ] 민원 접수 후 자동 배정까지의 최소 E2E 흐름이 동작한다.
- [ ] 배정 실패/장애에도 민원 데이터가 보존된다.
- [ ] 상태 이력과 Kafka 이벤트가 계약 기준으로 정리된다.
- [ ] 다음 단계인 officer 처리 API 구현으로 안전하게 넘어갈 수 있다.

## Suggested Sequence
- Day 1 오전: Task 1, Task 2
- Day 1 오후: Task 3, Task 4
- Day 2 오전: Task 5, Task 6
- Day 2 오후: Task 7, Task 8
- Day 3 오전: Task 9
- Day 3 오후: Task 10, Complete Checkpoint

## Parallelization Guidance
- Task 1-3은 순차 진행이 안전하다. 스키마와 엔티티가 확정되어야 이후 연동 코드가 흔들리지 않는다.
- Task 4-5는 같은 사람 또는 두 사람이 분리 가능하다. 한 명은 API/서비스, 다른 한 명은 번호 생성과 상태 이력을 맡을 수 있다.
- Task 6-8은 계약이 이미 고정되어 있으므로, 한 명은 Feign DTO/클라이언트, 다른 한 명은 상태 전이/이력 로직을 병렬로 준비할 수 있다.
- Task 9는 공통 이벤트 스키마가 이미 합의된 상태여야 하므로 앞선 과제 후 진행한다.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| 현재 complaint DB 스키마가 최종 계약보다 단순함 | High | Flyway migration을 먼저 보강하고 엔티티 작업은 그 다음에 진행한다 |
| assignment 연동 장애 시 민원 생성까지 실패로 전파될 수 있음 | High | 저장 우선, 연동 실패 시 `RECEIVED` 유지 정책을 서비스 테스트로 고정한다 |
| 상태 이력과 현재 상태 업데이트가 분리되어 정합성 문제가 날 수 있음 | High | 하나의 서비스 트랜잭션에서 민원 저장, 상태 전이, 이력 기록을 묶는다 |
| Kafka 이벤트 payload가 계약과 어긋날 수 있음 | Medium | `docs/api-spec.md`와 계약 문서를 기준으로 DTO를 먼저 고정하고 테스트에서 필드명을 검증한다 |
| JWT/권한 작업을 같이 시작하면 범위가 급격히 커짐 | Medium | 이번 단계는 내부 연동과 민원 저장 흐름에 집중하고 인증은 다음 작업으로 분리한다 |

## Open Questions
- 로컬 프로필에서 `complaint-service`도 H2를 유지할지, 아니면 바로 MySQL 로컬 컨테이너 기준으로 맞출지 팀 결정이 필요하다.
- 민원 접수 시 첨부파일 메타데이터 저장까지 이번 1차 구현에 포함할지, 본문 저장과 자동 배정만 먼저 갈지 범위 확정이 필요하다.
- `complaint.created.v1` 발행을 트랜잭션 직후 동기 발행으로 둘지, outbox 없이 단순 producer 호출로 시작할지 구현 방식 결정이 필요하다.
