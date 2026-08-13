# G-Civil MSA

정부 민원 접수, 부서 배정, 처리 및 알림을 다루는 Spring Boot 기반 MSA 프로젝트입니다. 요구사항 명세서의 User, Complaint, Assignment, Notification 서비스와 MSA 공통 서비스를 기준으로 구성합니다.

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
├── user-service/            # 사용자·JWT·Role
├── complaint-service/       # 민원 접수·처리
├── assignment-service/      # 부서·민원 배정
├── notification-service/    # Kafka 기반 알림
├── config-repo/             # 공통/서비스별 local·dev 설정
├── docs/                    # Kafka 규격·DevOps TODO
├── infra/                   # Docker Compose
├── scripts/                 # 실행·중지·점검
└── .github/workflows/       # CI
```

## 빠른 시작

1. 로컬 환경 파일을 만듭니다.

```bash
cp .env.example .env
```

2. Kafka와 서비스별 DB를 실행합니다. 상태 변경 Topic과 DLT도 자동으로 생성됩니다.

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
| User | 8084 | `user-service:8084` |
| Complaint | 8081 | `complaint-service:8081` |
| Assignment | 8082 | `assignment-service:8082` |
| Notification | 8083 | `notification-service:8083` |
| Config Server | 8888 | `config-service:8888` |
| Eureka | 8761 | `discovery-service:8761` |
| Kafka | 9092 | `kafka:29092` |
| User DB | 3310 | `user-db:3306` |
| Complaint DB | 3307 | `complaint-db:3306` |
| Assignment DB | 3308 | `assignment-db:3306` |
| Notification DB | 3309 | `notification-db:3306` |

호스트 포트는 로컬 PC의 `127.0.0.1`에만 바인딩됩니다. 컨테이너 사이에서는 `localhost` 대신 위의 서비스 이름을 사용합니다.

Kafka Topic을 확인하려면 다음 명령을 사용합니다.

```bash
./scripts/list-kafka-topics.sh
```

## 서비스 통신 원칙

- User Service는 사용자와 Role을 소유하고 JWT를 발급합니다.
- Gateway가 JWT를 검증하고 외부 요청의 단일 진입점 역할을 합니다.
- Complaint Service는 Assignment Service를 OpenFeign으로 호출합니다.
- 상태 변경 저장이 성공한 뒤 `complaint-status-changed.v1` 이벤트를 발행합니다.
- Notification Service는 이벤트를 소비하고 자체 DB에 알림을 저장합니다.
- 서비스는 다른 서비스의 DB를 직접 조회하거나 Foreign Key로 연결하지 않습니다.
- 서비스 간 관계는 `userId`, `complaintId`, `departmentId`만 공유합니다.

## 서비스가 지켜야 할 환경변수

Compose는 각 서비스에 다음 설정을 전달합니다.

- `SPRING_CONFIG_IMPORT`: Config Server 위치
- `SPRING_PROFILES_ACTIVE`: `local` 또는 `dev`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`: Eureka 위치
- `SPRING_DATASOURCE_*`: 서비스 전용 DB 연결 정보
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka 내부 주소
- `SERVER_PORT`: 서비스 포트
- `JWT_SECRET`: JWT 서명 키
- `KAFKA_COMPLAINT_STATUS_TOPIC`: 민원 상태 변경 Topic

Config Server는 [config-repo](config-repo)의 설정을 읽습니다. 서비스별 최소 `application.yml`에서는 애플리케이션 이름과 Config Server 연결만 지정하고, 비밀번호나 Secret을 저장하지 않습니다.

## Git 및 보안 규칙

- 커밋과 Pull Request 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 따릅니다.
- 저장소를 Clone한 뒤 `./scripts/setup-git-hooks.sh`를 한 번 실행합니다.
- `.env`는 커밋하지 않습니다.
- 비밀번호, 토큰, 운영 서버 주소는 GitHub Actions Secrets 또는 배포 환경에 저장합니다.
- 기능 브랜치는 `feature/<name>` 형식을 권장합니다.
- Pull Request에서 CI가 통과한 뒤 병합합니다.
- `main` 반영 시 애플리케이션 소스가 있는 서비스는 GHCR 이미지로 자동 발행됩니다.
- 실제 서버 배포는 배포 대상과 GitHub Secrets가 정해진 뒤 연결합니다.

## 현재 작업 현황

DevOps 완료 항목과 남은 검증은 [docs/devops-todo.md](docs/devops-todo.md)에서 관리합니다. Kafka 이벤트 계약은 [docs/kafka-topics.md](docs/kafka-topics.md), 백엔드 생성 기준은 [docs/backend-service-contract.md](docs/backend-service-contract.md)를 따릅니다.

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
