# Implementation Plan: Backend A Common Contract Freeze

## Overview
이 문서는 민원온 프로젝트에서 백엔드 A가 48시간 내 우선 확정해야 하는 공통 계약 작업의 실행 계획이다. 범위는 ERD 기준 `USER`, `COMPLAINT`, `ASSIGNMENT`, `RESPONSE`, `ATTACHMENT`, `STATUS` 필드 고정, 상태값 enum 고정, JWT payload 합의, Feign 요청/응답 DTO 정의, Kafka 이벤트 스키마 초안, 공통 에러 코드 정리까지로 제한한다. 구현보다 계약 동결을 먼저 끝내서 백엔드 B가 Gateway, Kafka Consumer, Config/Eureka 연동을 병렬로 진행할 수 있게 만드는 것이 목표다.

## Architecture Decisions
- 서비스 경계는 `user/auth`, `complaint`, `assignment`, `notification`, `statistics`로 정리한다.
- 서비스 간 참조는 DB FK가 아니라 ID 기반 논리 참조로 유지한다.
- 상태 머신의 원본(source of truth)은 `complaint-service`가 가진다. `assignment-service`는 배정 결과를 반환하지만 최종 상태 전이는 `complaint-service`가 기록한다.
- JWT는 Gateway 검증과 내부 서비스 인가에 필요한 최소 클레임만 포함한다.
- 동기 계약은 Feign DTO, 비동기 계약은 Kafka 이벤트 스키마로 분리하고, 둘 다 버전 필드를 둔다.
- 이번 계획의 완료 기준은 "코드 구현"이 아니라 "B가 의존해도 되는 계약 문서와 예시가 합의된 상태"다.

## Assumptions
- 기준일은 2026년 8월 15일이며, 역할 분담표의 "48시간 역할 분담"을 이번 스프린트의 제약으로 본다.
- `docs/api-spec.md`가 현재 API 계약의 기준 문서이며, 추가 산출물은 `docs/` 아래에 둔다.
- 첨부파일 원본은 S3에 저장되고, 계약 범위에서는 메타데이터와 다운로드 권한만 다룬다.
- `RESPONSE`는 공개 답변과 처리 완료 시점에 연결되는 민원 응답 엔티티로 본다.

## Deliverables
- 공통 계약 결정 문서 1부
- 도메인 필드/enum 표 1부
- JWT payload 명세 1부
- Feign DTO 명세 1부
- Kafka 이벤트 스키마 초안 1부
- 공통 에러 코드 및 표준 오류 응답 규약 1부
- 백엔드 B 전달용 handoff checklist 1부

## Task List

### Phase 1: Domain Contract Freeze

## Task 1: 서비스 경계와 엔티티 소유권 고정

**Description:** `USER`, `COMPLAINT`, `ASSIGNMENT`, `RESPONSE`, `ATTACHMENT`, `STATUS` 엔티티를 어떤 서비스가 소유하는지 먼저 고정한다. 이 단계에서 서비스 명칭, 통계용 데이터와 운영용 데이터의 분리, Notification/Statistics가 조회만 하는 필드와 생산하는 필드를 구분한다.

**Acceptance criteria:**
- [ ] 각 엔티티별 소유 서비스가 하나로 결정되어 문서에 표로 정리된다.
- [ ] 서비스 간 참조 필드가 "물리 FK"가 아닌 "논리 참조 ID"로 표시된다.
- [ ] 계약상 사용할 표준 서비스 명칭이 하나로 정리된다.

**Verification:**
- [ ] 문서 검토: ERD의 각 테이블이 소유 서비스와 함께 매핑되어 있다.
- [ ] 문서 검토: 서비스 간 직접 DB 접근이 금지된다는 규칙이 명시되어 있다.
- [ ] 수기 확인: 백엔드 B가 Gateway, Kafka, Config 작업을 시작하는 데 필요한 서비스 이름 혼선이 없다.

**Dependencies:** None

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Task 2: 상태값 enum과 상태 전이 규칙 고정

**Description:** 민원 상태 머신의 enum과 전이 규칙을 고정한다. `RECEIVED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`를 기준으로 누가 어떤 조건에서 전이를 발생시키는지, 배정 실패 시 `RECEIVED` 유지 규칙, 상태 이력 저장 방식, Kafka 발행 시점을 함께 정리한다.

**Acceptance criteria:**
- [ ] `ComplaintStatus` enum 최종 목록과 각 상태의 의미가 문서화된다.
- [ ] 허용 전이와 금지 전이가 표 또는 다이어그램으로 정리된다.
- [ ] 상태 변경의 기록 주체, 이력 저장 필드, 이벤트 발행 시점이 명시된다.

**Verification:**
- [ ] 문서 검토: 업무흐름도의 접수→배정→처리중→완료 흐름과 충돌이 없다.
- [ ] 문서 검토: 배정 실패 시 `201 Created + RECEIVED 유지` 규칙이 반영되어 있다.
- [ ] 수기 확인: B가 상태 변경 이벤트 Consumer 계약을 설계할 수 있을 정도로 명확하다.

**Dependencies:** Task 1

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Task 3: 핵심 엔티티 필드 고정

**Description:** ERD 기준으로 `USER`, `COMPLAINT`, `ASSIGNMENT`, `RESPONSE`, `ATTACHMENT`, `STATUS`의 필드를 최종 고정한다. 필수/선택 여부, 외부 서비스 논리 참조, 생성/수정 시각, 공개 여부, 담당자/부서 식별자, 첨부파일 메타데이터 범위를 B와 맞물리는 최소 계약 단위로 정의한다.

**Acceptance criteria:**
- [ ] 6개 엔티티 각각에 대해 필드명, 타입, 필수 여부, 소유 서비스가 정리된다.
- [ ] `USER.role`, `COMPLAINT.current_status`, `RESPONSE.is_public`, `ATTACHMENT` 메타데이터 필드가 빠짐없이 포함된다.
- [ ] 구현 전 논의가 필요한 애매한 필드가 별도 "Open Questions"로 분리된다.

**Verification:**
- [ ] 문서 검토: ERD 이미지의 주요 필드가 누락 없이 반영되어 있다.
- [ ] 수기 확인: Feign DTO와 Kafka 이벤트에 재사용할 공통 필드가 식별된다.
- [ ] 수기 확인: 백엔드 A와 B가 서로 다른 필드명을 쓰지 않도록 canonical naming이 정리된다.

**Dependencies:** Task 1, Task 2

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 1-3
- [ ] 서비스 경계, 상태 머신, 핵심 필드가 하나의 기준 문서에서 일관되게 읽힌다.
- [ ] 백엔드 B가 Gateway/JWT/Kafka 측 계약 작업을 시작할 수 있다.
- [ ] 팀 리뷰 전까지 남은 쟁점이 Open Questions로 분리되어 있다.
- [ ] 계약 문서를 기준으로 용어 혼선이 없다.

### Phase 2: Sync Interface Contract

## Task 4: JWT payload와 인증/인가 최소 계약 고정

**Description:** Gateway와 각 서비스가 공통으로 해석할 JWT payload를 확정한다. 최소 클레임 후보는 `sub`, `userId`, `loginId`, `role`, `departmentId`, `tokenType`, `iat`, `exp`이며, 어떤 필드가 필수인지와 OFFICER/ADMIN에서만 필요한 클레임을 구분한다.

**Acceptance criteria:**
- [ ] Access Token payload 필드와 타입이 문서화된다.
- [ ] Refresh Token에 포함할 최소 필드와 Access Token과의 차이가 명시된다.
- [ ] 각 서비스가 토큰에서 신뢰해도 되는 필드와 DB 재조회가 필요한 필드가 구분된다.

**Verification:**
- [ ] 문서 검토: Gateway JWT 검증/헤더 전달 요구와 충돌이 없다.
- [ ] 수기 확인: `role`, `departmentId`만으로 OFFICER 권한 분기 가능 여부가 설명된다.
- [ ] 수기 확인: 토큰 payload만으로 민원 작성자/담당자 권한 체크의 기본 경로가 보인다.

**Dependencies:** Task 1

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Task 5: Feign 요청/응답 DTO와 공통 에러 코드 고정

**Description:** 백엔드 A와 B가 바로 맞물리는 동기 계약을 정리한다. 우선순위는 `complaint-service -> assignment-service` 자동 배정 요청/응답, 사용자 권한 확인용 최소 사용자 조회 DTO, 공통 에러 응답 포맷과 에러 코드를 고정하는 것이다. 재시도 가능 오류와 비재시도 오류도 함께 분류한다.

**Acceptance criteria:**
- [ ] 자동 배정 요청/응답 DTO 필드가 예시 payload와 함께 정의된다.
- [ ] 서비스 간 공통 에러 코드와 HTTP 매핑이 표로 정리된다.
- [ ] 타임아웃/배정 실패/리소스 없음/권한 없음에 대한 표준 응답 규칙이 문서화된다.

**Verification:**
- [ ] 문서 검토: `201 Created + RECEIVED 유지` 예외 흐름이 Feign 실패 처리 규칙과 연결된다.
- [ ] 수기 확인: 백엔드 B가 OpenFeign Client 인터페이스를 바로 만들 수 있을 정도로 DTO가 충분히 구체적이다.
- [ ] 수기 확인: Gateway와 서비스가 같은 오류 envelope를 사용하도록 응답 예시가 정리된다.

**Dependencies:** Task 2, Task 3, Task 4

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 4-5
- [ ] JWT payload와 Feign DTO만으로 B가 Gateway 및 서비스 간 동기 호출 뼈대를 구현할 수 있다.
- [ ] 에러 코드가 인증, 인가, 검증, 리소스 조회, 상태 전이 실패를 모두 커버한다.
- [ ] 계약 문서에 예시 요청/응답이 포함되어 있다.

### Phase 3: Async Interface Contract and Handoff

## Task 6: Kafka 이벤트 스키마와 발행 트리거 고정

**Description:** 상태 변경과 처리 완료를 중심으로 Kafka 이벤트 초안을 고정한다. 최소 범위는 `ComplaintStatusChanged`, `ComplaintCompleted`, 필요 시 `ComplaintAssigned` 이벤트이며, 공통 envelope와 payload 필드, 버전, 발행 주체, idempotency key, consumer 기대 동작을 정리한다.

**Acceptance criteria:**
- [ ] 이벤트 종류별 토픽명, 발행 서비스, 발행 시점, payload 필드가 정리된다.
- [ ] 공통 envelope에 `eventId`, `eventType`, `eventVersion`, `occurredAt`, `producer`, `payload`가 포함된다.
- [ ] Notification/Statistics가 소비할 최소 필드가 빠짐없이 정의된다.

**Verification:**
- [ ] 문서 검토: 업무흐름도의 "완료 후 알림 발송"과 시스템 설계도의 Kafka 흐름이 일치한다.
- [ ] 수기 확인: B가 Producer/Consumer 계약을 구현할 때 추가 질문 없이 시작할 수 있다.
- [ ] 수기 확인: 중복 소비 대비를 위한 식별자 또는 idempotent 처리 기준이 문서에 있다.

**Dependencies:** Task 2, Task 3, Task 5

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Task 7: 백엔드 B 전달용 계약 패키지와 리뷰 체크리스트 작성

**Description:** 앞선 결정사항을 B가 바로 사용할 수 있게 handoff 패키지로 정리한다. 범위는 "확정된 것", "B가 바로 구현 가능한 것", "합의 필요 쟁점", "48시간 내 확인 필요한 질문"을 분리한 리뷰 체크리스트다.

**Acceptance criteria:**
- [ ] B 전달용 체크리스트가 계약 문서 안 또는 별도 섹션으로 정리된다.
- [ ] 합의 완료 항목과 후속 논의 항목이 구분된다.
- [ ] 리뷰 순서가 `도메인 -> JWT -> Feign -> Kafka -> 에러` 순으로 정리된다.

**Verification:**
- [ ] 수기 확인: 30분 이내 계약 리뷰 미팅 진행이 가능한 형태다.
- [ ] 수기 확인: B가 담당하는 Gateway/Kafka/Eureka/Config 작업과 직접 연결되는 입력값이 식별된다.
- [ ] 문서 검토: open question이 담당자와 due date와 함께 남아 있다.

**Dependencies:** Task 4, Task 5, Task 6

**Files likely touched:**
- `docs/plans/backend-a-contract-plan.md`

**Estimated scope:** S: 1-2 files

## Checkpoint: Complete
- [ ] 도메인 필드, enum, JWT, Feign DTO, Kafka 이벤트, 에러 코드가 모두 문서화되어 있다.
- [ ] 백엔드 B가 의존 가능한 계약과 추가 합의가 필요한 쟁점이 분리되어 있다.
- [ ] 구현 전 계약 리뷰가 가능한 최소 예시 payload가 준비되어 있다.
- [ ] 다음 구현 작업으로 자연스럽게 이어질 수 있다.

## Suggested 48-Hour Sequence
- Day 1 오전: Task 1, Task 2
- Day 1 오후: Task 3
- Day 1 종료 체크: Checkpoint After Tasks 1-3
- Day 2 오전: Task 4, Task 5
- Day 2 오후: Task 6, Task 7
- Day 2 종료 체크: Complete Checkpoint

## Parallelization Guidance
- Task 1-3은 순차 진행이 안전하다. 도메인 소유권과 상태 머신이 먼저 정해져야 나머지 계약이 흔들리지 않는다.
- Task 4와 Task 5는 Task 1-3 완료 후 같은 날 병렬 검토가 가능하다. 단, 최종 문서 반영은 한 사람이 canonical naming 기준으로 정리한다.
- Task 6은 Task 5의 Feign/에러 규칙을 참조하지만, 이벤트 payload 초안 작성 자체는 B와 병렬 토론이 가능하다.
- Task 7은 실질적으로 handoff 정리 작업이므로 다른 계약 작업이 끝난 뒤 마지막에 수행한다.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| 서비스 명칭 혼선 | High | 계약 문서와 저장소 디렉터리 명칭을 모두 `assignment-service`로 통일한다 |
| 상태 전이 책임 주체가 불명확함 | High | `complaint-service`를 source of truth로 명시하고 이벤트 발행 시점을 함께 고정한다 |
| JWT에 과도한 클레임을 넣어 인증 책임이 분산됨 | Medium | 최소 클레임 원칙과 재조회 필드를 분리한다 |
| Kafka 이벤트 payload가 Notification/Statistics 요구를 누락함 | High | Consumer 관점 필수 필드 체크리스트를 별도 검토한다 |
| 48시간 안에 모든 쟁점 합의가 어려움 | Medium | 구현 차단 이슈와 추후 보완 이슈를 분리해 먼저 동결 가능한 계약부터 확정한다 |

## Decision Status

| Item | Status | Decision / Owner |
|------|--------|------------|
| 상태 변경 이벤트 | 확정 | `complaint.status.changed.v1` 하나로 통합 |
| JWT `departmentId` | 확정 | Access Token 및 Gateway 내부 헤더에 포함 |
| `RESPONSE.isPublic` 변경 이벤트 | 확정 | 현재 범위에서는 발행하지 않음 |
| 첨부파일 다운로드 방식 | 미결 | 프록시/서명 URL 중 Backend A가 구현 전 결정 |
| `user-service` 디렉터리 및 DB 추가 | 미결 | 팀 소유자와 생성 시점 합의 필요 |
| `statistics-service` 디렉터리 및 DB 추가 | 미결 | 팀 소유자와 생성 시점 합의 필요 |
