# Complaint Service Postman Examples

작성일: 2026-08-18

## 공통 안내
- Base URL: `http://localhost:8081/api/v1`
- 현재 임시 인증 방식:
  - `X-User-Id`: 필수
- 선택 헤더:
  - `X-Request-Id`: 선택

## 1. 민원 접수

### Method / URL
- `POST http://localhost:8081/api/v1/complaints`

### Headers
- `X-User-Id: 101`
- `X-Request-Id: req-complaint-101`

### Body
- `form-data`

### form-data fields
- `categoryId`: `1`
- `categoryCode`: `TRAFFIC`
- `title`: `어린이보호구역 신호시간 조정 요청`
- `content`: `출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.`
- `notifyChannels`: `EMAIL`
- `attachmentFiles`: 파일 선택

## 2. 공식 답변 등록

### Method / URL
- `POST http://localhost:8081/api/v1/officer/complaints/{complaintId}/response`

### Example URL
- `POST http://localhost:8081/api/v1/officer/complaints/1/response`

### Headers
- `Content-Type: application/json`
- `X-User-Id: 201`
- `X-Request-Id: req-response-201`

### Body
```json
{
  "responseContent": "현장 교통량과 보행량 분석 결과에 따라 등교 시간대 보행 신호를 연장하기로 결정했습니다.",
  "isPublic": true
}
```

### 동작 조건
- `ASSIGNED`, `IN_PROGRESS` 상태에서만 등록 가능
- 한 민원당 공식 답변은 1건만 허용

## 3. 상태 변경: ASSIGNED -> IN_PROGRESS

### Method / URL
- `PATCH http://localhost:8081/api/v1/officer/complaints/{complaintId}/status`

### Example URL
- `PATCH http://localhost:8081/api/v1/officer/complaints/1/status`

### Headers
- `Content-Type: application/json`
- `X-User-Id: 201`
- `X-Request-Id: req-status-201`

### Body
```json
{
  "newStatus": "IN_PROGRESS",
  "changeMemo": "현장 조사 착수"
}
```

## 4. 상태 변경: IN_PROGRESS -> COMPLETED

### Method / URL
- `PATCH http://localhost:8081/api/v1/officer/complaints/{complaintId}/status`

### Example URL
- `PATCH http://localhost:8081/api/v1/officer/complaints/1/status`

### Headers
- `Content-Type: application/json`
- `X-User-Id: 201`
- `X-Request-Id: req-status-202`

### Body
```json
{
  "newStatus": "COMPLETED",
  "changeMemo": "조치 완료"
}
```

### 동작 조건
- 현재 상태가 `IN_PROGRESS` 여야 함
- 공식 답변이 먼저 등록되어 있어야 함

## 5. 테스트 순서 추천
1. 민원 접수
2. 응답에서 `complaintId` 확인
3. `PATCH /status` 로 `IN_PROGRESS` 변경
4. `POST /response` 로 공식 답변 등록
5. `PATCH /status` 로 `COMPLETED` 변경

## 6. 자주 나오는 실패 응답
- `VALIDATION_ERROR`
  - `newStatus` 오입력
  - `responseContent` 20자 미만
- `INVALID_STATUS_TRANSITION`
  - `ASSIGNED -> COMPLETED` 직접 전이
  - `RECEIVED -> IN_PROGRESS` 전이
- `RESPONSE_REQUIRED`
  - 공식 답변 없이 `COMPLETED` 시도
- `DUPLICATE_RESOURCE`
  - 같은 민원에 공식 답변을 두 번 등록
