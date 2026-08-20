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

## 2. 민원 카테고리 목록 조회

### Method / URL
- `GET http://localhost:8081/api/v1/complaint-categories`

### Example URL
- `GET http://localhost:8081/api/v1/complaint-categories`
- `GET http://localhost:8081/api/v1/complaint-categories?activeOnly=false`

### Query Params
- `activeOnly`: 기본값 `true`

### Success Response Example
```json
{
  "success": true,
  "data": [
    {
      "categoryId": 1,
      "categoryName": "도로·교통",
      "categoryCode": "TRAFFIC"
    },
    {
      "categoryId": 2,
      "categoryName": "환경",
      "categoryCode": "ENVIRONMENT"
    },
    {
      "categoryId": 3,
      "categoryName": "건설·시설",
      "categoryCode": "FACILITY"
    }
  ],
  "message": "카테고리 목록을 조회했습니다."
}
```

## 3. 내 민원 목록 조회

### Method / URL
- `GET http://localhost:8081/api/v1/complaints/my`

### Example URL
- `GET http://localhost:8081/api/v1/complaints/my?page=0&size=20`
- `GET http://localhost:8081/api/v1/complaints/my?page=0&size=20&status=IN_PROGRESS&categoryCode=TRAFFIC&keyword=신호`

### Headers
- `X-User-Id: 101`
- `X-Request-Id: req-my-complaints-101`

### Query Params
- `page`: 기본값 `0`
- `size`: 기본값 `20`, 최대 `100`
- `keyword`: 선택, 민원번호 또는 제목 검색, 최대 `50자`
- `categoryCode`: 선택, 예: `TRAFFIC`
- `status`: 선택, `RECEIVED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`

### Success Response Example
```json
{
  "success": true,
  "data": {
    "summary": {
      "total": 3,
      "received": 1,
      "assigned": 0,
      "inProgress": 1,
      "completed": 1
    },
    "content": [
      {
        "complaintId": 3,
        "complaintNo": "CIV-2026-000303",
        "title": "보도블록 정비 요청",
        "categoryCode": "FACILITY",
        "currentStatus": "COMPLETED",
        "assignedDepartmentName": "시설관리과",
        "assignedDepartmentId": 20,
        "assignedOfficerUserId": 202
      },
      {
        "complaintId": 2,
        "complaintNo": "CIV-2026-000302",
        "title": "횡단보도 보행신호 개선 요청",
        "categoryCode": "TRAFFIC",
        "currentStatus": "IN_PROGRESS",
        "assignedDepartmentName": "교통정책과",
        "assignedDepartmentId": 10,
        "assignedOfficerUserId": 201
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "hasNext": false
  },
  "message": "내 민원 목록을 조회했습니다."
}
```

## 4. 민원 상세 조회

### Method / URL
- `GET http://localhost:8081/api/v1/complaints/{complaintId}`

### Example URL
- `GET http://localhost:8081/api/v1/complaints/1`

### Headers
- 시민 본인 조회
  - `X-User-Id: 101`
- 공무원/관리자 조회
  - `X-User-Id: 201`
  - `X-User-Role: OFFICER`

### Success Response Example
```json
{
  "success": true,
  "data": {
    "complaintId": 1,
    "complaintNo": "CIV-2026-000501",
    "applicantUserId": 101,
    "categoryCode": "TRAFFIC",
    "title": "어린이보호구역 신호시간 조정 요청",
    "content": "출근 시간대 차량 정체로 인해 보행 대기 시간이 길어 조정을 요청드립니다.",
    "currentStatus": "IN_PROGRESS",
    "assignedDepartmentName": "교통정책과",
    "assignedDepartmentId": 10,
    "assignedOfficerUserId": 201,
    "submittedAt": "2026-08-18T10:00:00",
    "assignedAt": "2026-08-18T10:10:00",
    "completedAt": null,
    "response": {
      "responseId": 11,
      "responderUserId": 201,
      "isPublic": true,
      "responseContent": "현장 교통량을 확인한 뒤 신호시간 조정을 검토 중입니다.",
      "respondedAt": "2026-08-18T10:30:00"
    },
    "attachments": [
      {
        "attachmentId": 21,
        "originalFilename": "현장사진.jpg",
        "contentType": "image/jpeg",
        "fileSize": 12345,
        "uploadedAt": "2026-08-18T10:00:00"
      }
    ],
    "statusHistories": [
      {
        "previousStatus": null,
        "newStatus": "RECEIVED",
        "changedByUserId": 101,
        "changeMemo": "민원 접수",
        "changedAt": "2026-08-18T10:00:00"
      },
      {
        "previousStatus": "RECEIVED",
        "newStatus": "ASSIGNED",
        "changedByUserId": null,
        "changeMemo": "자동 배정 완료",
        "changedAt": "2026-08-18T10:10:00"
      }
    ]
  },
  "message": "민원 상세를 조회했습니다."
}
```

## 5. 첨부파일 다운로드

### Method / URL
- `GET http://localhost:8081/api/v1/complaints/{complaintId}/attachments/{attachmentId}`

### Example URL
- `GET http://localhost:8081/api/v1/complaints/1/attachments/21`

### Headers
- 시민 본인 다운로드
  - `X-User-Id: 101`
- 공무원/관리자 다운로드
  - `X-User-Id: 201`
  - `X-User-Role: OFFICER`

### Success Response
- 응답 본문은 JSON이 아니라 실제 파일 바이트 스트림입니다.
- 주요 응답 헤더 예시:
  - `Content-Disposition: attachment; filename="evidence.txt"`
  - `Content-Type: text/plain`

### cURL Example
```bash
curl -OJ "http://localhost:8081/api/v1/complaints/1/attachments/21" \
  -H "X-User-Id: 101"
```

### 동작 조건
- 시민은 본인 민원의 첨부파일만 다운로드 가능
- `OFFICER`, `ADMIN`은 다운로드 가능
- 첨부파일 메타데이터만 존재하고 원본 파일이 없으면 `404 RESOURCE_NOT_FOUND`

## 6. 담당 민원 목록 조회

### Method / URL
- `GET http://localhost:8081/api/v1/officer/complaints`

### Example URL
- `GET http://localhost:8081/api/v1/officer/complaints?page=0&size=20`
- `GET http://localhost:8081/api/v1/officer/complaints?page=0&size=20&status=IN_PROGRESS&keyword=신호`

### Headers
- `X-User-Id: 201`
- `X-Request-Id: req-officer-list-201`

### Query Params
- `page`: 기본값 `0`
- `size`: 기본값 `20`, 최대 `100`
- `status`: 선택, `RECEIVED`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`
- `keyword`: 선택, 민원번호 또는 제목 검색, 최대 `50자`

### Success Response Example
```json
{
  "success": true,
  "data": {
    "summary": {
      "newAssigned": 1,
      "inProgress": 1,
      "completed": 1
    },
    "content": [
      {
        "complaintId": 3,
        "complaintNo": "CIV-2026-000103",
        "title": "학교 앞 속도저감 시설 보강 요청",
        "assigneeName": "김담당",
        "assigneeUserId": 201,
        "status": "COMPLETED",
        "assignedDepartmentId": 10
      },
      {
        "complaintId": 2,
        "complaintNo": "CIV-2026-000102",
        "title": "횡단보도 보행신호 개선 요청",
        "assigneeName": "김담당",
        "assigneeUserId": 201,
        "status": "IN_PROGRESS",
        "assignedDepartmentId": 10
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "hasNext": false
  },
  "message": "담당 민원 목록을 조회했습니다."
}
```

### 동작 조건
- `X-User-Id` 기준으로 현재 공무원에게 배정된 민원만 조회
- `assigneeName` 은 `user-service` 내부 조회 결과를 사용

## 7. 공식 답변 등록

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

## 8. 상태 변경: ASSIGNED -> IN_PROGRESS

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

## 9. 상태 변경: IN_PROGRESS -> COMPLETED

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

## 10. 공개 민원 답변 목록 조회

### Method / URL
- `GET http://localhost:8081/api/v1/public-responses`

### Example URL
- `GET http://localhost:8081/api/v1/public-responses`
- `GET http://localhost:8081/api/v1/public-responses?keyword=신호&categoryCode=TRAFFIC&completedFrom=2026-08-01&completedTo=2026-08-14`

### Query Params
- `keyword`: 선택, 민원 제목 또는 답변 내용 검색, 최대 `50자`
- `categoryCode`: 선택, 예: `TRAFFIC`
- `completedFrom`: 선택, `yyyy-MM-dd`
- `completedTo`: 선택, `yyyy-MM-dd`

### Success Response Example
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "responseId": 7001,
        "title": "어린이보호구역 신호시간 조정 요청 처리 결과",
        "departmentName": "교통정책과",
        "completedAt": "2026-08-13",
        "statusLabel": "답변 완료"
      }
    ]
  },
  "message": "공개 답변 목록을 조회했습니다."
}
```

### 동작 조건
- 공개 답변(`isPublic=true`)만 노출
- 민원 상태가 `COMPLETED` 인 건만 노출
- 부서별 조회는 이번 버전에서 제외

## 11. 공개 민원 답변 상세 조회

### Method / URL
- `GET http://localhost:8081/api/v1/public-responses/{responseId}`

### Example URL
- `GET http://localhost:8081/api/v1/public-responses/7001`

### Success Response Example
```json
{
  "success": true,
  "data": {
    "caseTitle": "어린이보호구역 신호시간 조정 요청",
    "categoryName": "도로·교통",
    "appliedDate": "2026-08-12",
    "departmentName": "교통정책과",
    "completedAt": "2026-08-13",
    "responseContent": "현장 교통량과 보행량 조사를 실시한 결과, 등교 시간대 보행 신호를 연장하기로 결정했습니다.",
    "attachments": [
      {
        "attachmentId": 8101,
        "originalFilename": "교통량조사결과.pdf"
      }
    ]
  },
  "message": "공개 답변 상세를 조회했습니다."
}
```

### 동작 조건
- 공개 답변(`isPublic=true`)만 상세 조회 가능
- 비공개 답변 또는 없는 답변은 `404 RESOURCE_NOT_FOUND`

## 12. 테스트 순서 추천
1. 민원 접수
2. `GET /complaint-categories` 로 카테고리 확인
3. 응답에서 `complaintId` 확인
4. `GET /complaints/my` 로 본인 민원 목록 확인
5. `GET /complaints/{complaintId}` 로 상세 확인
6. `GET /complaints/{complaintId}/attachments/{attachmentId}` 로 첨부파일 다운로드 확인
7. `GET /officer/complaints` 로 담당 민원 목록 확인
8. `PATCH /status` 로 `IN_PROGRESS` 변경
9. `POST /response` 로 공식 답변 등록
10. `PATCH /status` 로 `COMPLETED` 변경

## 13. 자주 나오는 실패 응답
- `VALIDATION_ERROR`
  - `status` 오입력
  - `newStatus` 오입력
  - `keyword` 50자 초과
  - `responseContent` 20자 미만
- `INVALID_STATUS_TRANSITION`
  - `ASSIGNED -> COMPLETED` 직접 전이
  - `RECEIVED -> IN_PROGRESS` 전이
- `RESPONSE_REQUIRED`
  - 공식 답변 없이 `COMPLETED` 시도
- `DUPLICATE_RESOURCE`
  - 같은 민원에 공식 답변을 두 번 등록
- `FORBIDDEN`
  - 시민이 다른 사람 민원 상세를 조회
  - 시민이 다른 사람 민원 첨부파일을 다운로드
