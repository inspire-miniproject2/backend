# Implementation Plan: User Service Internal User Lookup API

## Overview
이 문서는 `assignment-service`가 담당 공무원 정보를 조회할 수 있도록 `user-service`에 최소 내부 조회 API를 구현하는 계획이다. 범위는 `GET /api/v1/internal/users/{userId}` 단일 endpoint와 그를 뒷받침하는 사용자 엔티티/저장소/예외 응답/내부 호출 검증에 한정한다. 목표는 `assignment-service`가 자동 배정 성공 시 `officerName`, `role`, `departmentId`, `isActive`를 신뢰 가능한 내부 API로 받아 사용할 수 있게 만드는 것이다.

## Architecture Decisions
- `user-service`는 `user_db`만 소유하고 다른 서비스 DB에 직접 접근하지 않는다.
- 내부 조회 API는 외부 Gateway 공개 API와 분리된 `/api/v1/internal/users/{userId}` 경로로 구현한다.
- 내부 서비스 인증은 `X-Internal-Caller` 헤더 + 내부 네트워크 격리 전제를 따른다.
- 응답 DTO는 이미 합의된 최소 필드(`userId`, `loginId`, `name`, `role`, `departmentId`, `isActive`)만 포함한다.
- `assignment-service`가 요구하는 것은 단건 조회이므로 목록/검색/관리 API는 이번 범위에서 제외한다.
- 초기 데이터는 개발 단계에서 JPA 자동 생성 + `data.sql` 또는 샘플 사용자 방식으로 최소 검증만 가능하게 한다.

## Assumptions
- 기준일은 2026-08-18이다.
- 현재 `user-service`는 Spring Boot 골격과 `ping` 컨트롤러만 존재하며, JPA/Validation/DB 매핑은 아직 없다.
- 합의된 계약 기준은 [backend-a-domain-contract-freeze.md](/Users/imhyeon/Projects/minwonon/backend/docs/contracts/backend-a-domain-contract-freeze.md) 의 3.1 USER, 8.6 내부 사용자 조회 DTO 섹션이다.
- 인프라 기준 DB는 MariaDB이며, `config-repo/user-service.yml`이 datasource driver를 제공한다.

## Deliverables
- `user-service` JPA/Validation 의존성 추가
- `User` 엔티티 및 `UserRepository`
- 내부 조회 DTO와 응답 envelope
- `GET /api/v1/internal/users/{userId}` API
- 내부 호출 검증 인터셉터 또는 필터
- 단건 조회 성공/미존재/비인가 테스트
- 로컬 샘플 데이터 또는 테스트 fixture

## Task List

### Phase 1: Foundation

## Task 1: user-service 런타임 의존성과 설정 확장

**Description:** 현재 `user-service`는 Web/Actuator/Eureka/Config만 포함되어 있으므로, 내부 조회 API 구현에 필요한 JPA, Validation, MariaDB/H2 런타임 의존성과 기본 datasource 설정을 추가한다. 테스트 환경에서 Config/Eureka 의존 없이 부팅되도록 로컬/테스트 설정도 함께 정리한다.

**Acceptance criteria:**
- [ ] `user-service`에 JPA, Validation, DB 드라이버 의존성이 추가된다.
- [ ] 로컬/테스트 환경에서 datasource와 ddl 설정이 동작한다.
- [ ] Config Server/Eureka 없이 테스트 부팅 가능한 설정이 준비된다.

**Verification:**
- [ ] 애플리케이션 컨텍스트 테스트가 부팅된다.
- [ ] 설정 검토: `spring.application.name=user-service`와 datasource 키가 인프라 규칙과 일치한다.
- [ ] 수기 확인: 테스트 환경이 외부 인프라 없이 실행 가능하다.

**Dependencies:** None

**Files likely touched:**
- `user-service/build.gradle`
- `user-service/src/main/resources/application.yml`
- `user-service/src/test/resources/application-test.yml`

**Estimated scope:** S: 1-2 files

## Task 2: USER 엔티티와 저장소 구현

**Description:** ERD 기준 `USER` 테이블을 코드에 매핑한다. 내부 조회 API에 필요한 최소 컬럼을 모두 포함하되, 비밀번호 해시 같은 민감 정보는 응답 DTO로 노출하지 않도록 경계를 분리한다.

**Acceptance criteria:**
- [ ] `User` 엔티티가 `user_id`, `login_id`, `name`, `role`, `department_id`, `is_active`를 포함한다.
- [ ] `login_id`, `email` unique와 `role` enum 매핑이 반영된다.
- [ ] `UserRepository`에서 `userId` 단건 조회가 가능하다.

**Verification:**
- [ ] JPA schema 생성 또는 DDL 검토: USER 구조가 ERD와 맞는다.
- [ ] 단위 테스트 또는 통합 테스트: 샘플 사용자 저장/조회가 동작한다.
- [ ] 수기 확인: passwordHash는 내부 조회 DTO에 노출되지 않는다.

**Dependencies:** Task 1

**Files likely touched:**
- `user-service/src/main/java/.../domain/User.java`
- `user-service/src/main/java/.../domain/Role.java`
- `user-service/src/main/java/.../repository/UserRepository.java`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 1-2
- [ ] `user-service`가 DB와 함께 부팅된다.
- [ ] USER 엔티티와 저장소가 준비된다.
- [ ] 내부 조회 API를 올릴 최소 데이터 계층이 갖춰진다.

### Phase 2: Internal Lookup API

## Task 3: 내부 조회 DTO와 서비스 계층 구현

**Description:** `assignment-service`가 요구하는 최소 응답 DTO를 구현하고, `userId` 기준으로 사용자를 조회해 DTO로 변환하는 서비스 계층을 만든다. 역할/활성 여부/부서 정보는 그대로 전달하되, 없는 사용자는 표준 `RESOURCE_NOT_FOUND`로 처리한다.

**Acceptance criteria:**
- [ ] 내부 응답 DTO가 합의된 필드 6개만 포함한다.
- [ ] 사용자 미존재 시 `RESOURCE_NOT_FOUND` 에러로 변환된다.
- [ ] 비활성 사용자도 조회는 되지만 `isActive`로 상태를 구분해 반환한다.

**Verification:**
- [ ] 서비스 테스트: 존재하는 사용자 조회 시 DTO가 정확히 매핑된다.
- [ ] 서비스 테스트: 존재하지 않는 사용자 조회 시 예외가 발생한다.
- [ ] 수기 확인: passwordHash, email, phone은 DTO에 포함되지 않는다.

**Dependencies:** Task 2

**Files likely touched:**
- `user-service/src/main/java/.../dto/InternalUserResponse.java`
- `user-service/src/main/java/.../service/InternalUserLookupService.java`
- `user-service/src/test/java/.../service/InternalUserLookupServiceTest.java`

**Estimated scope:** S: 1-2 files

## Task 4: GET /api/v1/internal/users/{userId} 컨트롤러 구현

**Description:** 내부 서비스가 호출할 수 있는 단건 조회 endpoint를 구현한다. 요청 경로와 응답 envelope는 계약 문서에 맞추고, `assignment-service`가 그대로 Feign으로 사용할 수 있게 JSON shape를 고정한다.

**Acceptance criteria:**
- [ ] `GET /api/v1/internal/users/{userId}` endpoint가 구현된다.
- [ ] 성공 응답이 `{ success, data, message }` 형태를 사용한다.
- [ ] `userId` path variable 검증과 미존재 처리 규칙이 반영된다.

**Verification:**
- [ ] API 테스트: 정상 조회 시 200 OK와 예상 DTO가 내려간다.
- [ ] API 테스트: 존재하지 않는 사용자 조회 시 404 + `RESOURCE_NOT_FOUND`.
- [ ] 수기 확인: `assignment-service` Feign DTO와 필드명이 정확히 일치한다.

**Dependencies:** Task 3

**Files likely touched:**
- `user-service/src/main/java/.../web/InternalUserController.java`
- `user-service/src/main/java/.../dto/ApiResponse.java`
- `user-service/src/test/java/.../web/InternalUserControllerTest.java`

**Estimated scope:** M: 3-5 files

## Task 5: 내부 호출 헤더 검증과 공통 예외 응답 구현

**Description:** 외부 사용자가 내부 API를 실수로 호출하지 않도록 `X-Internal-Caller` 기반 최소 보호막을 추가한다. 동시에 `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_SERVER_ERROR`를 공통 오류 envelope로 내려주는 핸들러를 정리한다.

**Acceptance criteria:**
- [ ] 내부 API에서 `X-Internal-Caller` 누락/불일치 시 접근이 차단된다.
- [ ] 공통 오류 응답이 `requestId`를 포함해 내려간다.
- [ ] 내부 조회 성공 응답과 오류 응답 shape가 일관된다.

**Verification:**
- [ ] API 테스트: 헤더 누락 시 403 오류가 내려간다.
- [ ] API 테스트: 잘못된 `userId` 형식 또는 검증 실패 시 400 계열 응답이 내려간다.
- [ ] 수기 확인: `assignment-service`가 Feign 예외를 분기 처리할 수 있는 수준으로 코드/메시지가 명확하다.

**Dependencies:** Task 4

**Files likely touched:**
- `user-service/src/main/java/.../config/InternalCallerInterceptor.java`
- `user-service/src/main/java/.../config/WebConfig.java`
- `user-service/src/main/java/.../exception/GlobalExceptionHandler.java`

**Estimated scope:** M: 3-5 files

## Checkpoint: After Tasks 3-5
- [ ] `assignment-service`가 기대하는 내부 사용자 조회 API가 준비된다.
- [ ] 성공/미존재/비인가 호출 흐름이 분리된다.
- [ ] 응답 shape가 계약 문서와 일치한다.

### Phase 3: Readiness

## Task 6: 샘플 데이터와 연동 검증 정리

**Description:** `assignment-service`의 샘플 `officer_user_id` 값과 맞는 최소 사용자 데이터를 준비한다. 로컬 통합 검증 기준으로 `OFFICER`, 부서 ID, 활성 여부가 맞는 테스트 데이터와 실행 순서를 문서화한다.

**Acceptance criteria:**
- [ ] `assignment-service` 샘플 규칙과 맞는 사용자 데이터가 준비된다.
- [ ] 로컬 통합 시나리오에 필요한 샘플 사용자 1~2명이 정의된다.
- [ ] README 또는 문서에 실행 순서와 호출 예시가 정리된다.

**Verification:**
- [ ] 수기 확인: `assignment-service`가 `officerUserId=201` 조회 시 이름/부서/활성 여부를 받을 수 있다.
- [ ] 통합 테스트 또는 수기 테스트: `assignment-service -> user-service` 호출이 성공한다.
- [ ] 문서 검토: 새 팀원이 로컬에서 연동 흐름을 따라갈 수 있다.

**Dependencies:** Task 5

**Files likely touched:**
- `user-service/src/main/resources/data.sql`
- `user-service/README.md`
- `docs/plans/user-service-internal-user-api-plan.md`

**Estimated scope:** S: 1-2 files

## Checkpoint: Complete
- [ ] `GET /api/v1/internal/users/{userId}`가 계약대로 동작한다.
- [ ] `assignment-service` Feign 호출이 붙을 준비가 된다.
- [ ] 샘플 데이터와 로컬 검증 경로가 정리된다.
- [ ] 내부 호출 보호와 오류 응답 규칙이 구현 기준으로 명확하다.

## Suggested Sequence
- Day 1 오전: Task 1, Task 2
- Day 1 오후: Task 3, Task 4
- Day 2 오전: Task 5
- Day 2 오후: Task 6, Complete Checkpoint

## Parallelization Guidance
- Task 1-4는 순차 진행이 안전하다. DB/JPA 기반이 먼저 있어야 컨트롤러와 서비스가 흔들리지 않는다.
- Task 5는 Task 4 완료 후 병렬 보완이 가능하다. 한 명은 인터셉터, 다른 한 명은 예외 응답 정리를 맡을 수 있다.
- Task 6은 핵심 구현 후 문서화/샘플 데이터 정리 작업으로 병렬 처리 가능하다.

## Risks and Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| user-service가 아직 DB/JPA가 없어 구현 범위가 예상보다 커짐 | High | 내부 조회 API 범위만 최소로 제한하고 인증/JWT 기능은 이번 작업에서 제외한다 |
| assignment-service 샘플 데이터와 user-service 샘플 사용자가 불일치함 | High | `officer_user_id`와 동일한 샘플 사용자 ID를 명시적으로 맞춘다 |
| 내부 API 보호가 너무 약하거나 너무 무거움 | Medium | 이번 범위는 인터셉터 기반 `X-Internal-Caller` 검증으로 통일한다 |
| 응답 DTO가 과도하게 커져 계약이 흔들림 | Medium | 합의된 6개 필드만 유지하고 나머지 필드는 추후 확장으로 미룬다 |

## Open Questions
- `user-service` 샘플 데이터 적재를 `data.sql`로 할지 테스트 fixture 위주로 할지 팀 합의가 필요하다.
- 향후 JWT/로그인 기능이 붙을 때 현재 `User` 엔티티와 내부 조회 API DTO를 어떻게 분리 유지할지 검토가 필요하다.
- `assignment-service` 외에 `complaint-service`나 `notification-service`도 같은 내부 조회 API를 재사용할지 추후 정리가 필요하다.
