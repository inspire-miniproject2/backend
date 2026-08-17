# 백엔드 서비스 연동 계약

각 백엔드 담당자는 자신의 서비스 폴더에 Spring Boot 프로젝트를 생성할 때 아래 기준을 맞춥니다.

## 공통 기준

- Java 17
- Gradle Wrapper 포함
- Spring Boot Actuator 포함
- Eureka Discovery Client 포함(Config Server 제외 가능)
- Config Client 포함(Config Server 제외)
- `/actuator/health` 사용 가능
- 파일 인코딩 UTF-8, 시간대 `Asia/Seoul`

로컬에서 IDE로 실행할 때 필요한 최소 설정 예시는 다음과 같습니다.

```yaml
spring:
  application:
    name: user-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  config:
    import: optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

`spring.application.name`은 폴더명과 동일하게 지정합니다.

## 서비스별 이름과 포트

| 폴더 | application name | 포트 | 주요 의존성 |
|---|---|---:|---|
| `config-service` | `config-service` | 8888 | Config Server, Actuator |
| `discovery-service` | `discovery-service` | 8761 | Eureka Server, Config Client, Actuator |
| `gateway-service` | `gateway-service` | 8080 | Gateway, Eureka Client, Config Client, Actuator |
| `user-service` | `user-service` | 8084 | Web, JPA, Validation, Security, JWT, MariaDB, Eureka, Config, Actuator |
| `complaint-service` | `complaint-service` | 8081 | Web, JPA, Validation, OpenFeign, Kafka, MariaDB, Eureka, Config, Actuator |
| `assignment-service` | `assignment-service` | 8082 | Web, JPA, Validation, MariaDB, Eureka, Config, Actuator |
| `notification-service` | `notification-service` | 8083 | Web, JPA, Kafka, MariaDB, Eureka, Config, Actuator |

## 데이터 소유권

- `user-service` → `user-db`
- `complaint-service` → `complaint-db`
- `assignment-service` → `department-db`(로컬 컨테이너명, 논리 DB명은 `assignment_db`)
- `notification-service` → `notification-db`

다른 서비스의 DB, Entity 또는 Repository를 직접 참조하지 않습니다. 서비스 간에는 DTO와 식별값만 전달합니다.

## 통합 전 확인

- `./gradlew test`가 통과합니다.
- 환경변수 없이 비밀번호나 JWT Secret을 하드코딩하지 않습니다.
- Docker 내부 주소에 `localhost`를 사용하지 않습니다.
- 허용되지 않은 민원 상태 전이를 거부합니다.
- Complaint DB 변경이 성공한 후에만 Kafka 이벤트를 발행합니다.
- OpenFeign 실패와 Kafka 중복 소비에 대한 처리 기준이 있습니다.
