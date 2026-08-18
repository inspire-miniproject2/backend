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
- 기본 프로필은 H2 메모리 DB로 기동됩니다.
- 샘플 데이터는 `src/main/resources/data.sql`로 적재됩니다.
