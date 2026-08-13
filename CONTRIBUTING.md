# G-Civil 협업 규칙

## 브랜치 전략

- `main`: 최종 제출 또는 배포 가능한 안정 버전
- `develop`: 기능 개발이 모이는 통합 브랜치
- 작업 브랜치: `develop`에서 생성하고 Pull Request로 다시 `develop`에 병합

브랜치명은 `작업유형/기능명` 형식을 사용합니다.

```text
feature/complaint-create
fix/assignment-error
docs/readme
refactor/api-client
style/admin-page
setting/docker-compose
```

`main`과 `develop`에는 직접 Push하지 않습니다. 기능 하나당 브랜치 하나를 사용합니다.

## 커밋 메시지

### 기본 형식

```text
[작업유형]_일자/파일명 또는 기능명

- 작업한 내용
- 변경한 이유
- 확인한 내용
```

`일자`는 커밋한 날짜의 일(day)을 두 자리로 작성합니다. 예를 들어 8월 7일은 `07`입니다.

### 작업유형

| 작업유형 | 사용 상황 |
|---|---|
| `[Add]` | 새로운 기능, 파일, API 추가 |
| `[Fix]` | 오류 수정 |
| `[Update]` | 기존 기능 개선 또는 내용 변경 |
| `[Remove]` | 불필요한 코드나 파일 삭제 |
| `[Refactor]` | 기능 변화 없는 코드 구조 개선 |
| `[Docs]` | README, 주석 등 문서 수정 |
| `[Style]` | 디자인, CSS, 레이아웃 수정 |
| `[Setting]` | 환경설정, 의존성, Docker, CI 변경 |

### 예시

```text
[Add]_07/ComplaintCreate

- 민원 접수 API와 요청 검증 추가
- 민원인의 신규 민원을 저장하기 위해 구현
- 정상 요청과 필수값 누락 응답 확인
```

```text
[Fix]_07/AssignmentService.java

- 담당 부서가 없을 때 발생하던 예외 수정
- 미배정 상태로 안전하게 저장하도록 변경
- 부서가 없는 민원의 접수 결과 확인
```

```text
[Setting]_07/DockerCompose

- Kafka와 서비스별 MariaDB Health Check 추가
- 애플리케이션보다 인프라가 먼저 준비되도록 구성
- Docker Compose 설정 검증 완료
```

한 커밋에는 하나의 작업 목적만 담습니다. 파일이 많다면 파일을 모두 나열하는 대신 기능명을 사용합니다.

### 로컬 자동 검사 설치

저장소를 처음 Clone한 뒤 한 번 실행합니다.

```bash
./scripts/setup-git-hooks.sh
```

이후 규칙에 맞지 않는 커밋 제목은 로컬에서 거부됩니다. GitHub Actions에서도 Push와 Pull Request의 커밋 제목을 검사합니다.

## 커밋 전 확인사항

- 실행 또는 테스트 시 오류가 없는가?
- 불필요한 로그와 사용하지 않는 코드가 남아 있지 않은가?
- API Key, 비밀번호, 개인정보가 포함되지 않았는가?
- 커밋 하나에 작업 목적 하나만 들어 있는가?
- 제목과 본문이 변경 내용을 분명하게 설명하는가?

## Pull Request

- 작업 브랜치에서 `develop`으로 Pull Request를 생성합니다.
- 제목은 `[Add] 민원 접수 기능 구현`처럼 작업유형으로 시작합니다.
- 작업 내용, 변경 이유, 확인한 내용, 참고 사항을 작성합니다.
- 기능 동작, 코드 구조, 민감정보, 다른 기능에 미치는 영향과 문서 반영 여부를 검토합니다.
- 리뷰 승인, CI 통과, 충돌 해결 후 병합합니다.

충돌이 생기면 관련 파일을 수정한 팀원과 함께 확인하고, 최신 `develop`을 작업 브랜치에 병합한 뒤 해결합니다.

