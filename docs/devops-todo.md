# DevOps TODO

## 완료

- [x] 백엔드와 프론트엔드 GitHub 저장소 분리
- [x] User, Complaint, Assignment, Notification 서비스별 DB 구성
- [x] Kafka KRaft 단일 노드와 상태 변경 Topic·DLT 자동 생성
- [x] Config Repository와 `local`/`dev` 프로필 분리
- [x] JWT, DB 비밀번호를 환경변수로 분리
- [x] 서비스별 Dockerfile과 Docker Compose 기본 구성
- [x] GitHub Actions 테스트·Compose 검증 구성
- [x] CI 검사를 단일 Runner로 통합하고 중복 실행 자동 취소
- [x] 컨테이너 이미지 발행을 필요한 서비스만 선택하는 수동 실행으로 경량화
- [x] 커밋 규칙과 PR 템플릿 구성

## 백엔드 소스가 들어오면

- [ ] 모든 서비스 `./gradlew test` 통과
- [ ] 모든 서비스 Docker 이미지 빌드
- [ ] Config Server 설정 조회 확인
- [ ] Eureka에 5개 애플리케이션 서비스 등록 확인
- [ ] Gateway를 통한 API 접근 확인
- [ ] Complaint → Assignment OpenFeign 성공·실패 확인
- [ ] 상태 변경 이벤트 발행·알림 저장 확인
- [ ] 특정 서비스 중단 후 나머지 서비스 장애 격리 확인
- [ ] `./scripts/health-check.sh --strict` 통과

## 배포 환경이 정해지면

- [ ] GHCR 이미지 Push 확인
- [ ] GitHub Environment `dev` 생성
- [ ] JWT·DB·배포 서버 값을 GitHub Secrets에 등록
- [ ] 배포 서버 Docker Compose 작성
- [ ] SSH 또는 배포 플랫폼 기반 자동 배포 연결
- [ ] 배포 실패 시 이전 이미지 태그로 롤백
- [ ] 배포 및 롤백 시연 기록
