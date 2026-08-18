# Backend A PR Checklist

작성일: 2026-08-18

## 목적
- Backend A 범위에서 완료한 `user-service`, `assignment-service`, `complaint-service` 연동 작업을 PR 단위로 정리한다.
- `develop` 브랜치로 올리기 전에 확인해야 할 항목과, 머지 후 바로 이어질 작업을 체크리스트로 남긴다.

## 이번 PR에 포함되는 작업
- [x] `user-service` 내부 사용자 조회 API 구현
- [x] `assignment-service` 자동 배정 API 구현
- [x] `assignment-service -> user-service` 내부 조회 연동 구현
- [x] `complaint-service` 민원 접수 API 기본 구현
- [x] `complaint-service -> assignment-service` 동기 연동 구현
- [x] `complaint.created.v1` 이벤트 발행 반영
- [x] `complaint.status.changed.v1` 이벤트 발행 반영
- [x] 로컬/개발 환경 분리를 위한 `application-local.yml`, `application-dev.yml` 정리
- [x] MySQL 기준 로컬 실행 설정 반영
- [x] 관련 계획 문서 및 계약 문서 보강

## PR 전 확인 체크리스트
- [ ] 현재 브랜치가 작업 목적에 맞는 `feature/*` 이름인지 확인
- [ ] `git status` 기준 불필요한 변경 파일이 없는지 확인
- [ ] 민감 정보가 코드나 문서에 포함되지 않았는지 확인
- [ ] `user-service`, `assignment-service`, `complaint-service` 실행 방법이 최신 상태인지 확인
- [ ] 내부 연동 헤더 규칙이 문서와 구현에 동일하게 반영됐는지 확인
- [ ] Kafka 이벤트 이름이 계약 문서와 구현에 동일하게 반영됐는지 확인
- [ ] 로컬 MySQL 포트와 프로필 설정이 `infra/docker-compose.yml` 기준과 일치하는지 확인
- [ ] 테스트 결과 또는 수동 검증 결과를 PR 본문에 적을 수 있게 정리

## PR 본문에 꼭 적을 내용
- [ ] 작업 내용
- [ ] 변경 이유
- [ ] 확인한 내용
- [ ] 실행 방법
- [ ] 아직 남아 있는 후속 작업

## 머지 후 바로 이어질 Backend A 작업
- [ ] `complaint-service` 상태 변경 API 구현
- [ ] `COMPLETED` 전 `RESPONSE` 필수 규칙을 실제 API 플로우에 반영
- [ ] `complaint-service` 답변 등록 API 구현
- [ ] 조회 API와 상태 이력 조회 API 구현
- [ ] Kafka consumer 연동 기준으로 notification/statistics 팀과 최종 통합 확인
- [ ] JWT 로그인 적용 시 내부 헤더 기반 임시 인증 제거 범위 재정리

## Backend B와 재확인할 계약
- [ ] Gateway JWT payload 및 헤더 전달 규칙
- [ ] `POST /api/v1/internal/assignments` 요청/응답 DTO 최종 고정본
- [ ] `complaint.created.v1` 이벤트 소비 여부 및 필드 해석
- [ ] `complaint.status.changed.v1` 이벤트 소비 규칙
- [ ] `complaint.response.registered.v1` 이벤트 소비 규칙

## 권장 PR 제목
- `[Add] Backend A 서비스 연동 및 complaint-service 접수 흐름 구현`

## 권장 PR 대상
- `feature/... -> develop`
