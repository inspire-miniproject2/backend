# Config Repository

Spring Cloud Config Server가 로컬에서 읽는 설정 저장소입니다.

- `application.yml`: 모든 서비스 공통 설정
- `application-local.yml`: 로컬 개발 설정
- `application-dev.yml`: 공유 개발 환경 설정
- `<service-name>.yml`: 서비스별 설정

비밀번호와 JWT Secret은 이 디렉터리에 값으로 저장하지 않습니다. `${ENV_NAME}` 형태로만 참조하고 실제 값은 `.env` 또는 GitHub Secrets에서 주입합니다.

