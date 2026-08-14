# 환경변수 규칙

이 문서는 백엔드 소스, Docker Compose, GitHub Actions와 AWS 배포 환경이 동일한 변수 이름을 사용하기 위한 계약입니다. 새 변수가 필요하면 먼저 이 문서와 `.env.example`에 추가한 뒤 애플리케이션 설정과 Compose를 함께 변경합니다.

## 기본 원칙

1. 환경변수 이름은 대문자 `SNAKE_CASE`를 사용합니다.
2. 여러 서비스가 같은 종류의 값을 가질 때는 서비스 접두사를 붙입니다. 예: `COMPLAINT_DB_PASSWORD`.
3. Docker 컨테이너 사이에서는 `localhost`를 사용하지 않고 Compose 서비스 이름을 사용합니다. 예: `kafka:29092`, `complaint-db:3306`.
4. 실제 비밀번호, 토큰과 운영 주소는 Git에 커밋하지 않습니다.
5. `.env.example`에는 로컬 개발용 예시와 필요한 변수 이름만 기록합니다.
6. 필수 Secret은 Compose의 `${VARIABLE:?message}` 형식으로 누락 즉시 실패하게 합니다.
7. 시간은 `Asia/Seoul`, API 시간값은 ISO 8601을 기본으로 합니다.
8. AWS에서는 Access Key를 파일에 저장하지 않고 EC2 IAM Role을 사용합니다.

## 파일별 역할

| 위치 | 용도 | Git 커밋 |
|---|---|---|
| `.env.example` | 로컬 실행 예시와 변수 목록 | 포함 |
| `.env` | 개인 로컬 실행값 | 제외 |
| `config-repo/*.yml` | Spring 설정과 환경변수 연결 | 포함 |
| `infra/docker-compose*.yml` | 컨테이너에 환경변수 전달 | 포함 |
| GitHub Environment Secrets | CI/CD 실제 Secret | Git 외부 |
| EC2 환경 또는 Parameter Store | AWS 배포값 | Git 외부 |

## 공통 런타임

| 변수 | 기본값 | 필수 | 설명 |
|---|---|---:|---|
| `COMPOSE_PROJECT_NAME` | `g-civil-msa` | N | Compose 프로젝트 이름 |
| `TZ` | `Asia/Seoul` | N | 컨테이너 시간대 |
| `SPRING_PROFILES_ACTIVE` | `local` | N | `local`, `dev`, `prod` 프로필 |

## 인증

| 변수 | 로컬 예시 | 필수 | Secret | 설명 |
|---|---|---:|---:|---|
| `JWT_SECRET` | 32자 이상의 로컬 전용 값 | Y | Y | JWT 서명 키 |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | `3600000` | N | N | Access Token 만료시간(ms) |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | `1209600000` | N | N | Refresh Token 만료시간(ms) |

Gateway와 User Service는 같은 `JWT_SECRET`을 사용해야 합니다. 공유·운영 환경에서는 `.env.example`의 값을 그대로 사용하지 않습니다.

## Kafka

| 변수 | 기본값 | 필수 | 설명 |
|---|---|---:|---|
| `KAFKA_HOST_PORT` | `9092` | N | 호스트에서 Kafka에 접속할 포트 |
| `KAFKA_COMPLAINT_CREATED_TOPIC` | `complaint.created.v1` | N | 민원 생성 이벤트 Topic |
| `KAFKA_COMPLAINT_CREATED_DLT` | `complaint.created.v1.dlt` | N | 민원 생성 DLT |
| `KAFKA_COMPLAINT_STATUS_TOPIC` | `complaint.status.changed.v1` | N | 상태 변경 이벤트 Topic |
| `KAFKA_COMPLAINT_STATUS_DLT` | `complaint.status.changed.v1.dlt` | N | 상태 변경 DLT |
| `NOTIFICATION_KAFKA_GROUP_ID` | `notification-service` | N | Notification Consumer Group |
| `STATISTICS_KAFKA_GROUP_ID` | `statistics-service` | N | Statistics Consumer Group 예약값 |

애플리케이션 컨테이너의 Kafka 주소는 `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092`로 고정합니다. `localhost:9092`는 호스트에서 직접 테스트할 때만 사용합니다.

## 데이터베이스

각 서비스는 `<SERVICE>_DB_NAME`, `<SERVICE>_DB_USER`, `<SERVICE>_DB_PASSWORD`, `<SERVICE>_DB_ROOT_PASSWORD`, `<SERVICE>_DB_HOST_PORT` 형식을 사용합니다.

| 서비스 | DB 이름 기본값 | 호스트 포트 | 컨테이너 주소 |
|---|---|---:|---|
| User | `user_db` | `3310` | `user-db:3306` |
| Complaint | `complaint_db` | `3307` | `complaint-db:3306` |
| Assignment | `assignment_db` | `3308` | `assignment-db:3306` |
| Notification | `notification_db` | `3309` | `notification-db:3306` |
| Statistics(예약) | `statistics_db` | `3311` | `statistics-db:3306` |

DB 비밀번호와 Root 비밀번호는 Secret입니다. 서비스는 자신의 DB만 사용하며 다른 서비스 DB에 직접 접속하지 않습니다.

## 첨부파일 저장소

| 변수 | 기본값 | 필수 조건 | 설명 |
|---|---|---|---|
| `FILE_STORAGE_TYPE` | `local` | 항상 | `local` 또는 `s3` |
| `LOCAL_STORAGE_PATH` | `/data/attachments` | `local`일 때 | Docker Volume 내부 경로 |
| `AWS_REGION` | `ap-northeast-2` | `s3`일 때 | AWS 서울 리전 |
| `S3_ATTACHMENT_BUCKET` | 빈 값 | `s3`일 때 | 비공개 첨부파일 버킷 이름 |

로컬 개발은 `FILE_STORAGE_TYPE=local`을 사용합니다. S3 사용이 팀에서 확정된 뒤 AWS 환경만 `s3`로 변경합니다. EC2 IAM Role을 사용하므로 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`는 프로젝트 환경변수 규격에 포함하지 않습니다.

## 애플리케이션 포트

| 변수 | 기본값 | 서비스 |
|---|---:|---|
| `GATEWAY_PORT` | `8080` | API Gateway |
| `COMPLAINT_PORT` | `8081` | Complaint |
| `ASSIGNMENT_PORT` | `8082` | Assignment |
| `NOTIFICATION_PORT` | `8083` | Notification |
| `USER_PORT` | `8084` | User |
| `STATISTICS_PORT` | `8085` | Statistics 예약값 |
| `CONFIG_PORT` | `8888` | Config Server |
| `DISCOVERY_PORT` | `8761` | Eureka Server |

`STATISTICS_*` 변수는 서비스 추가가 확정되기 전까지 예약값입니다. 실제 소스와 Compose 서비스 추가 시 백엔드 B와 포트·DB 이름을 다시 확인합니다.

## 변경 절차

환경변수를 추가하거나 이름을 변경할 때는 다음 파일을 한 PR에서 함께 수정합니다.

1. `docs/environment-variables.md`
2. `.env.example`
3. `infra/docker-compose.yml` 또는 `infra/docker-compose.apps.yml`
4. 필요한 `config-repo/<service>.yml`
5. 관련 README와 테스트 스크립트

기존 환경변수 이름을 바로 삭제하지 않습니다. 백엔드 코드와 배포 환경이 새 이름으로 전환된 것을 확인한 뒤 제거합니다.

## 검증

로컬 `.env`가 없다면 먼저 생성합니다.

```bash
cp .env.example .env
```

Compose 변수 치환과 문법을 확인합니다.

```bash
docker compose --env-file .env -f infra/docker-compose.yml -f infra/docker-compose.apps.yml config --quiet
```

실제 Secret이나 `.env`가 Git 추적 대상에 포함되지 않았는지도 확인합니다.

```bash
git status --short
git check-ignore .env
```
