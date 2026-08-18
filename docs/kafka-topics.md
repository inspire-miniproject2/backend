# Kafka Topic 규격

확정 이벤트 envelope와 payload는 [백엔드 도메인 계약](contracts/backend-a-domain-contract-freeze.md#12-kafka-event-contract-freeze)을 기준으로 합니다.

| Topic | Producer | Consumer | 파티션 | 용도 |
|---|---|---|---:|---|
| `complaint.created.v1` | complaint-service | statistics-service | 3 | 신규 민원 접수 집계 |
| `complaint.status.changed.v1` | complaint-service | notification-service, statistics-service | 3 | 상태 변경 알림과 집계 |
| `complaint.response.registered.v1` | complaint-service | notification-service | 3 | 공식 답변 등록 알림 |

공통 운영 규칙:

- Kafka message key와 envelope의 `partitionKey`는 `complaintId` 문자열입니다.
- Producer는 DB transaction commit 이후 이벤트를 발행합니다.
- Consumer group은 서비스별로 분리하며 `eventId`를 기준으로 멱등 처리합니다.
- 각 Topic에는 `<topic>.dlt` 형식의 1 partition DLT가 함께 생성됩니다.
- DLT에는 원본 Topic, partition, offset, 실패 사유를 header 또는 payload로 보존합니다.

로컬 Kafka는 `infra/docker-compose.yml`의 KRaft 단일 노드로 실행되며 `kafka-init`이 아래 여섯 Topic을 자동 생성합니다.

```text
complaint.created.v1
complaint.created.v1.dlt
complaint.status.changed.v1
complaint.status.changed.v1.dlt
complaint.response.registered.v1
complaint.response.registered.v1.dlt
```
