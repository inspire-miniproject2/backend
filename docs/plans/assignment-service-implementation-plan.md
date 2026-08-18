    # Implementation Plan: Assignment Service

## Overview
이 문서는 민원온 프로젝트의 `assignment-service` 구현 계획이다. 이 서비스는 별도 `ASSIGNMENT` 테이블 없이 `DEPARTMENT`, `COMPLAINT_CATEGORY`, `DEPARTMENT_CATEGORY`를 배정 규칙 aggregate로 사용하며, `complaint-service`의 내부 Feign 요청을 받아 자동 배정 결과를 반환한다. 목표는 1) 배정 기준 데이터 저장, 2) 내부 자동 배정 API 제공, 3) 배정 실패를 예외가 아닌 business miss로 처리하는 흐름을 안전하게 구현하는 것이다.

## Architecture Decisions
- `assignment-service`는 `assignment_db`만 소유하고, 다른 서비스 DB에는 직접 접근하지 않는다.
- 배정 규칙은 `DEPARTMENT + COMPLAINT_CATEGORY + DEPARTMENT_CATEGORY` 조합으로 관리한다.
- `assignment-service`는 민원 상태를 직접 변경하지 않고, 배정 결과만 반환한다.
- 내부 자동 배정 API는 `POST /api/v1/internal/assignments` 단일 진입점으로 구현한다.
- 배정 실패는 `5xx`가 아니라 `200 OK + assignmentFound=false`로 반환한다.
- 공무원 상세 조회는 이번 범위에서 최소화하고, 우선 `officerUserId` 중심으로 계약을 맞춘다.
- 로컬 개발 기준 DB는 MySQL 또는 PostgreSQL 중 팀 선택 DB를 쓰되, 서비스 코드는 JPA 기준으로 DB 의존성을 최소화한다.

## Assumptions
- 기준일은 2026-08-18이다.
- 현재 `assignment-service` 디렉터리에는 구현 코드가 없고, 실질적으로 신규 서비스 골격부터 시작한다.
- 기준 계약은 [backend-a-domain-contract-freeze.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-domain-contract-freeze.md), [backend-a-b-contract-summary.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-b-contract-summary.md), [api-spec.md](/Users/imhyeon/Projects/minwonon/backend/docs/api-spec.md) 를 따른다.
- `assignment-service/README.md`의 `department-db` 표현은 구식이며, 실제 표준 명칭은 `assignment_db`로 본다.

## Deliverables
- `assignment-service` 애플리케이션 기본 골격
- `department`, `complaint_category`, `department_category` 엔티티 및 저장소
- 내부 자동 배정 API 구현
- 배정 규칙 관리용 최소 관리자 API 또는 초기 데이터 입력 방식
- 요청/응답/에러 DTO
- 서비스/레포지토리 테스트
- 로컬 실행 및 데이터 준비 문서

## Task List

### Phase 1: Foundation

## Task 1: 서비스 골격과 런타임 설정 정리

**Description:** `assignment-service`를 독립 실행 가능한 Spring Boot 서비스로 세팅한다. 빌드 파일, 기본 패키지 구조, 애플리케이션 이름, 포트, datasource, JPA, actuator 등 최소 런타임 설정을 정리하고, README의 오래된 `department-db` 표현도 함께 바로잡는다.

**Acceptance criteria:**
- [ ] `assignment-service`가 단독으로 기동 가능한 기본 프로젝트 구조를 가진다.
- [ ] `spring.application.name=assignment-service` 등 표준 명칭이 설정된다.
- [ ] DB 연결 설정 키와 프로필 구조가 로컬 실행 기준으로 정리된다.

**Verification:**
- [ ] 애플리케이션 기동 확인: `assignment-service`가 실행되고 health endpoint가 응답한다.
- [ ] 설정 검토: 서비스명과 DB명 표현이 문서/설정에서 일관된다.
- [ ] 수기 확인: 다른 서비스 없이도 부팅 실패 없이 올라온다.

**Dependencies:** None

**Files likely touched:**
- `assignment-service/build.gradle` 또는 `assignment-service/pom.xml`
- `assignment-service/src/main/resources/application.yml`
- `assignment-service/README.md`

**Estimated scope:** M: 3-5 files

## Task 2: assignment_db 스키마와 엔티티 매핑 구현

**Description:** `DEPARTMENT`, `COMPLAINT_CATEGORY`, `DEPARTMENT_CATEGORY`를 실제 코드 엔티티와 DB 매핑으로 옮긴다. 내부 FK는 `assignment-service` DB 안에서만 걸고, `officerUserId`는 `user-service`의 논리 참조로만 유지한다.

**Acceptance criteria:**
- [ ] `Department`, `ComplaintCategory`, `DepartmentCategory` 엔티티가 ERD 기준 필드를 모두 가진다.
- [ ] 유니크 제약과 인덱스가 배정 규칙 조회에 맞게 반영된다.
- [ ] 별도 `ASSIGNMENT` 엔티티/테이블을 만들지 않는다.

**Verification:**
- [ ] JPA schema 생성 또는 DDL 검토: 3개 테이블이 의도한 이름과 컬럼으로 생성된다.
- [ ] 수기 확인: `department_id`, `category_id` FK는 내부에서만 연결된다.
- [ ] 수기 확인: `officer_user_id`는 논리 참조로만 남는다.

**Dependencies:** Task 1

**Files likely touched:**
- `assignment-service/src/main/java/.../domain/Department.java`
- `assignment-service/src/main/java/.../domain/ComplaintCategory.java`
- `assignment-service/src/main/java/.../domain/DepartmentCategory.java`
- `assignment-service/src/main/resources/application.yml`

**Estimated scope:** M: 3-5 files

## Task 3: 배정 규칙 조회 리포지토리와 도메인 서비스 구현

**Description:** 카테고리 기반으로 활성화된 부서/담당자 매핑을 찾는 조회 로직을 구현한다. 가장 단순한 우선순위는 `categoryId` 기준 활성 규칙 1건 조회이며, 이후 `categoryCode` 검증과 예외/미존재 분기를 분리한다.

**Acceptance criteria:**
- [ ] 활성 부서, 활성 카테고리, 활성 매핑만 배정 대상이 된다.
- [ ] `categoryId` 또는 `categoryCode` 불일치 상황을 구분할 수 있다.
- [ ] 배정 규칙 조회 결과를 도메인 객체 또는 응답용 결과 모델로 반환한다.

**Verification:**
- [ ] 단위 테스트: 활성 매핑 존재 시 정확한 부서/담당자 규칙을 반환한다.
- [ ] 단위 테스트: 비활성 부서/카테고리/매핑은 배정 대상에서 제외된다.
- [ ] 단위 테스트: 규칙 미존재 시 business miss 결과를 만든다.

**Dependencies:** Task 2

**Files likely touched:**
- `assignment-service/src/main/java/.../repository/DepartmentCategoryRepository.java`
- `assignment-service/src/main/java/.../service/AssignmentRuleService.java`
- `assignment-service/src/test/java/.../service/AssignmentRuleServiceTest.java`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 1-3
- [ ] `assignment-service`가 단독 부팅된다.
- [ ] 핵심 3개 테이블과 엔티티가 준비된다.
- [ ] 카테고리 기준 배정 규칙 조회가 테스트로 검증된다.
- [ ] 자동 배정 API를 올릴 수 있는 최소 도메인 기반이 갖춰진다.

### Phase 2: Internal Assignment Flow

## Task 4: 내부 자동 배정 요청/응답 DTO와 컨트롤러 구현

**Description:** 백엔드 A/B 계약에 맞춰 `POST /api/v1/internal/assignments` API를 구현한다. 요청은 `complaintId`, `complaintNo`, `categoryId`, `categoryCode`, `applicantUserId`, `submittedAt`를 받고, 성공/미매핑 응답을 계약 그대로 반환한다.

**Acceptance criteria:**
- [ ] 내부 자동 배정 endpoint가 계약 경로와 메서드로 노출된다.
- [ ] 성공 시 `assignmentFound=true`와 부서/담당자 정보가 반환된다.
- [ ] 미매핑 시 `assignmentFound=false`, `reasonCode`, `reasonMessage`가 반환된다.

**Verification:**
- [ ] API 테스트: 유효한 카테고리 요청 시 success response shape가 계약과 일치한다.
- [ ] API 테스트: 미매핑 요청 시 200 OK + miss response가 내려간다.
- [ ] 수기 확인: 응답 필드명이 계약 문서와 동일하다.

**Dependencies:** Task 3

**Files likely touched:**
- `assignment-service/src/main/java/.../api/InternalAssignmentController.java`
- `assignment-service/src/main/java/.../dto/AssignmentRequest.java`
- `assignment-service/src/main/java/.../dto/AssignmentResponse.java`
- `assignment-service/src/test/java/.../api/InternalAssignmentControllerTest.java`

**Estimated scope:** M: 3-5 files

## Task 5: 입력 검증과 내부 인증 규칙 반영

**Description:** 내부 API에 대한 필수값 검증, `X-Internal-Caller` 기반 최소 내부 호출 보호, 공통 에러 응답 매핑을 정리한다. 이번 범위에서는 내부 네트워크 격리를 전제로 하되, 외부 사용자가 실수로 접근했을 때의 방어선을 만든다.

**Acceptance criteria:**
- [ ] 필수 요청값 누락 시 표준 검증 오류 응답이 내려간다.
- [ ] 내부 호출 식별 헤더가 없거나 잘못되면 접근이 거부된다.
- [ ] business miss와 validation/error 응답이 구분된다.

**Verification:**
- [ ] API 테스트: 잘못된 요청에 대해 400 계열 오류가 내려간다.
- [ ] API 테스트: 내부 호출 헤더 누락 시 401 또는 403으로 차단된다.
- [ ] 수기 확인: `assignmentFound=false`는 에러가 아니라 정상 응답으로 유지된다.

**Dependencies:** Task 4

**Files likely touched:**
- `assignment-service/src/main/java/.../config/WebConfig.java`
- `assignment-service/src/main/java/.../exception/GlobalExceptionHandler.java`
- `assignment-service/src/test/java/.../api/InternalAssignmentSecurityTest.java`

**Estimated scope:** S: 1-2 files

## Checkpoint: After Tasks 4-5
- [ ] `complaint-service`가 바로 호출할 수 있는 내부 API가 준비된다.
- [ ] 성공/미매핑/잘못된 요청/비인가 호출 흐름이 구분된다.
- [ ] 계약 문서 기준 응답 shape가 실제 구현과 맞는다.

### Phase 3: Data Management and Readiness

## Task 6: 배정 기준 데이터 입력 방식 구현

**Description:** 자동 배정 API가 실제로 동작하려면 부서, 카테고리, 매핑 규칙 데이터가 필요하다. 관리자 API 3종 또는 최소 seed data 로더 중 하나를 선택해 개발 단계에서 데이터를 주입할 수 있게 만든다.

**Acceptance criteria:**
- [ ] 부서, 카테고리, 매핑 규칙을 로컬에서 생성할 수 있다.
- [ ] 최소 1개 이상의 샘플 규칙으로 자동 배정 API를 검증할 수 있다.
- [ ] 중복 매핑 또는 비활성 데이터 처리 기준이 정리된다.

**Verification:**
- [ ] 수기 확인: 샘플 데이터 입력 후 자동 배정 API가 성공 응답을 반환한다.
- [ ] 단위 또는 통합 테스트: 중복 규칙 방지 제약이 동작한다.
- [ ] 문서 검토: 운영 전환 시 seed data를 어떻게 대체할지 메모가 남아 있다.

**Dependencies:** Task 2, Task 4

**Files likely touched:**
- `assignment-service/src/main/java/.../api/AdminDepartmentController.java`
- `assignment-service/src/main/java/.../api/AdminCategoryController.java`
- `assignment-service/src/main/resources/data.sql`

**Estimated scope:** M: 3-5 files

## Task 7: 통합 테스트와 로컬 실행 문서 마무리

**Description:** `assignment-service` 단독 기준 통합 검증을 정리한다. DB 기동, 샘플 데이터 주입, 내부 API 호출 예시, 예상 응답을 README 또는 별도 문서에 남겨서 백엔드 B와 통합 시 혼선이 없게 만든다.

**Acceptance criteria:**
- [ ] 로컬 실행 순서가 문서화된다.
- [ ] DB 준비, 샘플 데이터, 내부 API 호출 예시가 포함된다.
- [ ] 핵심 성공/미매핑 케이스가 테스트 또는 문서 예시로 검증된다.

**Verification:**
- [ ] 문서 검토: 새 팀원이 보고 바로 로컬 실행을 시도할 수 있다.
- [ ] 통합 테스트 또는 수기 테스트: 샘플 요청 2종이 예상 응답을 반환한다.
- [ ] 수기 확인: `complaint-service` 연동 담당자가 바로 붙일 수 있다.

**Dependencies:** Task 5, Task 6

**Files likely touched:**
- `assignment-service/README.md`
- `assignment-service/src/test/java/.../integration/AssignmentIntegrationTest.java`
- `docs/api-spec.md`

**Estimated scope:** S: 1-2 files

## Checkpoint: Complete
- [ ] `assignment-service`가 단독 실행 가능하다.
- [ ] 자동 배정 API가 계약대로 동작한다.
- [ ] 배정 실패를 정상 응답으로 처리한다.
- [ ] 샘플 데이터와 로컬 실행 문서가 준비된다.
- [ ] `complaint-service` 연동 착수에 필요한 정보가 정리된다.

## Suggested Sequence
- Day 1 오전: Task 1, Task 2
- Day 1 오후: Task 3, Checkpoint 1
- Day 2 오전: Task 4, Task 5
- Day 2 오후: Task 6, Task 7, Complete Checkpoint

## Parallelization Guidance
- Task 1-4는 순차 진행이 안전하다. 서비스 골격과 엔티티가 먼저 있어야 내부 API가 흔들리지 않는다.
- Task 5는 Task 4 진행 후 병렬 보완이 가능하다. 한 명은 입력 검증, 다른 한 명은 내부 인증 헤더 처리를 맡을 수 있다.
- Task 6의 데이터 입력 방식은 관리자 API와 seed data 중 하나를 택하면 구현 범위를 줄일 수 있다.
- Task 7은 테스트 정리와 문서화 작업이라, 핵심 구현이 끝난 뒤 병렬로 진행 가능하다.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| `assignment-service`가 너무 많은 관리 기능까지 떠안음 | Medium | 이번 범위는 자동 배정 API와 최소 데이터 입력 방식까지만 제한한다 |
| 부서/카테고리/매핑 활성화 기준이 모호함 | High | 조회 로직에서 활성 조건을 명시하고 테스트 케이스로 고정한다 |
| `officerName`을 위해 user-service 의존이 커짐 | Medium | 이번 1차 구현은 `officerUserId` 중심으로 만들고, 이름 조회는 후속 과제로 분리한다 |
| business miss와 실제 장애가 혼동됨 | High | `assignmentFound=false`와 5xx 오류를 분리해 테스트로 고정한다 |
| README와 실제 DB 명칭이 다름 | Low | 계획 초반에 `department-db` 표기를 `assignment_db`로 수정한다 |

## Open Questions
- `officerName`은 1차 구현에서 고정값/seed data로 처리할지, user-service 조회를 붙일지 결정이 필요하다.
- 배정 규칙이 여러 건일 때 우선순위 컬럼이 필요한지 여부는 아직 미정이다. 현재는 “활성 규칙 1건” 가정이다.
- 관리자 API를 만들지 않고 `data.sql`만으로 초기 데이터를 넣을지 팀 합의가 필요하다.
- 내부 호출 보안을 Spring Security로 붙일지, 간단한 인터셉터 기반 헤더 검증으로 시작할지 결정이 필요하다.
