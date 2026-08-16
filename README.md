# G-Civil MSA

정부 민원 접수, 부서 배정, 처리 및 알림을 다루는 Spring Boot 기반 MSA 프로젝트입니다.

현재 저장소에는 팀 개발을 시작하기 위한 백엔드 서비스 디렉터리와 로컬 인프라, 컨테이너 빌드, CI 기본 구성이 준비되어 있습니다. 각 Spring Boot 서비스 소스는 담당자가 해당 디렉터리에 추가합니다. 프론트엔드는 별도 [frontend 저장소](https://github.com/inspire-miniproject2/frontend)에서 관리합니다.

## 기준 환경

- JDK 17
- Gradle Wrapper
- Spring Boot 3.x / Spring Cloud 호환 버전
- MariaDB 11.4
- Apache Kafka 3.9.1 (KRaft, 단일 노드 개발 환경)
- Docker Compose v2
- 시간대: `Asia/Seoul`

Spring Boot와 Spring Cloud의 정확한 버전 조합은 백엔드 담당자가 프로젝트를 생성할 때 한 번에 통일해야 합니다.

## 디렉터리

```text
g-civil-msa/
├── discovery-service/       # Eureka Server
├── config-service/          # Spring Cloud Config Server
├── gateway-service/         # Spring Cloud Gateway
├── complaint-service/       # 민원 접수·처리
├── assignment-service/      # 부서·배정
├── notification-service/    # Kafka 기반 알림
├── infra/                   # Docker Compose
├── scripts/                 # 실행·중지·점검
└── .github/workflows/       # CI
```

## 현재 구현 상태와 확장 합의

- 현재 추적 중인 6개 서비스 디렉터리는 `README.md`, `Dockerfile`, `.dockerignore`만 있는 구현 전 골격이다.
- 계약상 필요한 `user-service`와 `statistics-service`는 아직 저장소에 없다. 담당자와 생성 시점을 합의한 뒤 기존 루트 구조에 추가한다.
- 두 서비스가 추가되기 전까지 Compose와 CI에 존재하지 않는 build context를 임의로 선언하지 않는다.
- 계약의 표준 서비스명은 `assignment-service`다. 로컬 인프라의 `department-db`와 `DEPARTMENT_DB_*` 환경변수는 이 서비스가 소유하는 DB의 기존 운영 이름이며, 호환성을 위해 유지한다.
- API 명세의 논리 DB명 `assignment_db`와 로컬 Compose의 실제 기본 DB명 `department_db`는 동일 소유 경계를 가리킨다.

## 빠른 시작

1. 로컬 환경 파일을 만듭니다.

```bash
cp .env.example .env
```

2. Kafka와 서비스별 DB를 실행합니다.

```bash
./scripts/start-infra.sh
```

3. 상태를 확인합니다.

```bash
./scripts/health-check.sh
```

애플리케이션까지 실행한 뒤 모든 Health endpoint를 필수로 검사하려면 strict 모드를 사용합니다.

```bash
./scripts/health-check.sh --strict
```

4. 인프라를 종료합니다.

```bash
./scripts/stop.sh
```

애플리케이션 소스와 Gradle Wrapper가 모든 서비스에 추가된 후에는 다음 명령으로 전체를 빌드하고 실행할 수 있습니다.

```bash
./scripts/start-all.sh
```

## 로컬 포트

| 구성 요소 | 호스트 포트 | 컨테이너 내부 주소 |
|---|---:|---|
| API Gateway | 8080 | `gateway-service:8080` |
| Complaint | 8081 | `complaint-service:8081` |
| Assignment | 8082 | `assignment-service:8082` |
| Notification | 8083 | `notification-service:8083` |
| Config Server | 8888 | `config-service:8888` |
| Eureka | 8761 | `discovery-service:8761` |
| Kafka | 9092 | `kafka:29092` |
| Complaint DB | 3307 | `complaint-db:3306` |
| Department DB | 3308 | `department-db:3306` |
| Notification DB | 3309 | `notification-db:3306` |

컨테이너 사이에서는 `localhost` 대신 위의 서비스 이름을 사용합니다.

## 서비스가 지켜야 할 환경변수

Compose는 각 서비스에 다음 설정을 전달합니다.

- `SPRING_CONFIG_IMPORT`: Config Server 위치
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: Eureka 위치
- `SPRING_DATASOURCE_*`: 서비스 전용 DB 연결 정보
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka 내부 주소
- `SERVER_PORT`: 서비스 포트

서비스별 `application.yml`은 이 값들을 덮어쓰지 않도록 환경변수 기반으로 구성합니다. 다른 서비스의 DB에 직접 접근해서는 안 됩니다.

## Git 및 보안 규칙

- 커밋과 Pull Request 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다.
- 저장소를 Clone한 뒤 `./scripts/setup-git-hooks.sh`를 한 번 실행합니다.
- `.env`는 커밋하지 않습니다.
- 비밀번호, 토큰, 운영 서버 주소는 GitHub Actions Secrets 또는 배포 환경에 저장합니다.
- 기능 브랜치는 `feature/<name>` 형식을 권장합니다.
- Pull Request에서 CI가 통과한 뒤 병합합니다.
- 운영 배포 워크플로는 배포 대상 서버와 이미지 저장소가 정해진 뒤 별도로 추가합니다.

## 백엔드 담당자 체크리스트

각 서비스 디렉터리에 다음 파일이 들어와야 컨테이너 빌드가 가능합니다.

```text
gradlew
gradle/wrapper/
build.gradle (또는 build.gradle.kts)
settings.gradle (또는 settings.gradle.kts)
src/
```

각 서비스는 Actuator를 추가하고 `/actuator/health`를 노출해야 합니다.
