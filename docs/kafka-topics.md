# Kafka Topic 규격

## 이름 규칙

- 소문자와 점(`.`)으로 도메인·이벤트를 구분합니다.
- 이벤트 스키마가 깨지는 변경에는 버전 접미사(`.v1`)를 올립니다.
- 처리 실패 Topic은 원본 이름 뒤에 `.dlt`를 붙입니다.

## `complaint.created.v1`

Complaint Service가 신규 민원을 DB에 저장한 뒤 발행하고 Statistics Service가 소비합니다.

- 파티션: 3
- 메시지 Key: `complaintId`
- Consumer Group: `statistics-service`
- DLT: `complaint.created.v1.dlt`

## `complaint.status.changed.v1`

Complaint Service가 허용된 상태 변경을 DB에 저장한 뒤 발행하고 Notification Service와 Statistics Service가 각자의 Consumer Group으로 소비합니다.

권장 이벤트 필드:

```json
{
  "eventId": "uuid",
  "eventType": "ComplaintStatusChanged",
  "eventVersion": "v1",
  "occurredAt": "2026-08-13T16:00:00+09:00",
  "producer": "complaint-service",
  "partitionKey": "1001",
  "payload": {
    "complaintId": 1001,
    "complaintNo": "CIV-2026-000184",
    "previousStatus": "ASSIGNED",
    "currentStatus": "IN_PROGRESS",
    "statusChangedByUserId": 9001,
    "assignedDepartmentId": 21,
    "assignedOfficerUserId": 9001,
    "statusChangedAt": "2026-08-13T16:00:00+09:00",
    "notifyChannels": ["IN_APP", "EMAIL"]
  }
}
```

- 파티션: 3
- 메시지 Key: `complaintId`
- Consumer Group: `notification-service`, `statistics-service`
- 중복 방지: Notification Service가 `eventId`를 유일값으로 저장

## `complaint.response.registered.v1`

Complaint Service가 공식 답변을 DB에 저장한 뒤 발행하고 Notification Service가 소비합니다.

- 파티션: 3
- 메시지 Key: `complaintId`
- Consumer Group: `notification-service`
- DLT: `complaint.response.registered.v1.dlt`

## DLT

역직렬화 오류나 재시도 후에도 처리하지 못한 이벤트를 보관합니다.

- `complaint.created.v1.dlt`
- `complaint.status.changed.v1.dlt`
- `complaint.response.registered.v1.dlt`
- 파티션: 1
- 원본 Topic, Partition, Offset, 실패 사유를 Header 또는 DLT Payload에 포함합니다.
