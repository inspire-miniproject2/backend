# Kafka Topic 규격

## `complaint-status-changed.v1`

Complaint Service가 허용된 상태 변경을 DB에 저장한 뒤 발행하고 Notification Service가 소비합니다.

권장 이벤트 필드:

```json
{
  "eventId": "uuid",
  "eventVersion": 1,
  "occurredAt": "2026-08-13T16:00:00+09:00",
  "complaintId": 1,
  "userId": 10,
  "previousStatus": "ASSIGNED",
  "changedStatus": "IN_PROGRESS"
}
```

- 파티션: 3
- 메시지 Key: `complaintId`
- Consumer Group: `notification-service`
- 중복 방지: Notification Service가 `eventId`를 유일값으로 저장

## `complaint-status-changed.v1.dlt`

역직렬화 오류나 재시도 후에도 처리하지 못한 이벤트를 보관합니다.

- 파티션: 1
- 원본 Topic, Partition, Offset, 실패 사유를 Header 또는 DLT Payload에 포함합니다.

