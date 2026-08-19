# assignment-service

부서 정보, 민원 카테고리, 카테고리-부서-담당 공무원 매핑 규칙을 관리하는 서비스입니다.

## 역할
- `assignment_db` 소유
- `DEPARTMENT`, `COMPLAINT_CATEGORY`, `DEPARTMENT_CATEGORY` 관리
- `complaint-service`의 내부 Feign 요청을 받아 자동 배정 결과 반환
- 민원 상태는 직접 바꾸지 않고 배정 결과만 응답

## 내부 API
- `POST /api/v1/internal/assignments`
- 내부 호출 헤더: `X-Internal-Caller: complaint-service`

## 로컬 실행 메모
- 기본 포트: `8082`
- `local`, `dev` 프로필은 MySQL과 Flyway migration을 사용합니다.
- 자동화 테스트의 `test` 프로필만 H2와 `src/test/resources/data.sql`을 사용합니다.
- 이미 적용된 Flyway migration은 수정하지 않고 후속 변경을 새 버전 파일로 추가합니다.
