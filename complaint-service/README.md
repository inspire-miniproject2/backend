# complaint-service

민원 접수·조회·상태 변경 담당 디렉터리입니다. 이 서비스는 `complaint-db`만 소유합니다.

## Implemented Scope
- `POST /api/v1/complaints` multipart 민원 접수
- `PATCH /api/v1/officer/complaints/{complaintId}/status` 상태 변경
- `POST /api/v1/officer/complaints/{complaintId}/response` 공식 답변 등록
- 첨부파일 메타데이터 저장
- `assignment-service` 동기 자동 배정 호출
- `complaint.created.v1`, `complaint.status.changed.v1`, `complaint.response.registered.v1` 트랜잭션 직후 동기 발행

## Local Run
- 로컬 DB는 MySQL 컨테이너(`infra/docker-compose.yml`) 기준입니다.
- 기본 포트는 `8081`, 기본 DB 포트는 `3307` 입니다.

```bash
cd /Users/imhyeon/Projects/minwonon/backend/complaint-service
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
SPRING_PROFILES_ACTIVE=local \
SPRING_CLOUD_CONFIG_ENABLED=false \
EUREKA_CLIENT_ENABLED=false \
./gradlew bootRun
```

## Request Notes
- JWT/로그인 기능은 아직 붙이지 않았기 때문에, 현재는 임시로 `X-User-Id` 헤더를 받아 신청자 ID를 전달합니다.
- `assignment-service` 요청 계약상 `categoryCode`가 필요하므로, 현재 접수 요청에서도 `categoryCode`를 함께 받습니다.
- 첨부파일 원본은 아직 S3 업로드를 붙이지 않았고, 이번 단계에서는 메타데이터만 저장합니다.

## Example Request
```bash
curl -X POST http://localhost:8081/api/v1/complaints \
  -H "X-User-Id: 101" \
  -H "X-Request-Id: req-complaint-1" \
  -F "categoryId=1" \
  -F "categoryCode=TRAFFIC" \
  -F "title=어린이보호구역 신호시간 조정 요청" \
  -F "content=출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다." \
  -F "notifyChannels=EMAIL" \
  -F "attachmentFiles=@/path/to/photo.jpg"
```

## Postman / Test Examples
- Postman 예시는 [complaint-service-postman-examples.md](/Users/imhyeon/Projects/minwonon/backend/docs/complaint-service-postman-examples.md) 에 정리했습니다.
