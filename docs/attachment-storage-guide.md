# Attachment Storage Guide

작성일: 2026-08-18

이 문서는 `complaint-service`의 첨부파일 저장/다운로드 방식과 `local`, `s3` 운영 방식을 함께 정리합니다.

## 1. 현재 구조
- 첨부파일 메타데이터는 `complaint_db.complaint_attachments` 테이블에 저장합니다.
- 원본 파일은 DB에 넣지 않고 별도 저장소에 보관합니다.
- 다운로드 API는 `complaint-service`가 권한을 검사한 뒤 파일 스트림을 직접 내려줍니다.

관련 API:
- 업로드: `POST /api/v1/complaints`
- 다운로드: `GET /api/v1/complaints/{complaintId}/attachments/{attachmentId}`

## 2. 저장되는 메타데이터
`complaint_attachments`에는 아래 값이 저장됩니다.
- `original_filename`
- `stored_filename`
- `file_path`
- `content_type`
- `file_size`
- `uploaded_at`

여기서 `file_path`는 저장소 종류에 따라 의미가 달라집니다.
- `local`: 로컬 디렉터리 하위 상대 경로
- `s3`: S3 object key

## 3. 환경변수

`complaint-service`는 아래 설정을 사용합니다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `FILE_STORAGE_TYPE` | `local` | 첨부파일 저장소 구현 선택 |
| `ATTACHMENT_LOCAL_ROOT` | `${java.io.tmpdir}/minwonon-attachments` | `local` 저장 루트 |
| `AWS_REGION` | `ap-northeast-2` | S3 사용 시 AWS 리전 |
| `S3_ATTACHMENT_BUCKET` | 없음 | S3 버킷 이름 |
| `S3_ATTACHMENT_PREFIX` | `complaints/` | S3 object key prefix |

현재 코드 기준:
- `local` 저장소는 구현되어 있습니다.
- `s3` 저장소도 구현되어 있습니다.
- `FILE_STORAGE_TYPE=s3`이면 Complaint Service가 AWS SDK로 S3에 업로드/다운로드를 수행합니다.

## 4. Local 방식

### 동작 방식
- 업로드 시 `file_path` 예: `complaints/CIV-2026-000001/{uuid}.jpg`
- 실제 저장 위치:
  - `${ATTACHMENT_LOCAL_ROOT}/complaints/CIV-2026-000001/{uuid}.jpg`
- 다운로드 시 DB의 `file_path`를 기준으로 로컬 파일을 읽어 응답합니다.

### 언제 쓰면 좋은가
- 로컬 개발
- Postman/브라우저 다운로드 테스트
- CI 또는 단일 개발 환경

### 예시 설정
```bash
export FILE_STORAGE_TYPE=local
export ATTACHMENT_LOCAL_ROOT=/tmp/minwonon-attachments
```

## 5. S3 방식

### 목표 구조
- DB의 `file_path`에는 S3 object key만 저장
- 예: `complaints/CIV-2026-000001/{uuid}.jpg`
- 다운로드 시 `complaint-service`가 권한 확인 후 S3에서 파일을 읽어 스트림 반환

### 왜 필요한가
- 서버 재시작/재배포와 무관하게 파일 유지
- 여러 인스턴스에서 동일 첨부파일 접근 가능
- 운영 환경에서 로컬 디스크 의존성 제거

### 현재 상태
- 운영 문서와 환경 계약, AWS SDK 연동 코드가 모두 준비됨
- Complaint Service가 권한 확인 후 S3 object를 직접 읽어 스트림으로 반환

### 운영 시 지켜야 할 점
- 버킷은 private 유지
- public URL 저장 금지
- IAM Role 기반 접근
- `complaints/` prefix만 권한 부여

추가 참고:
- [s3-attachments.md](/Users/imhyeon/Projects/minwonon/backend/docs/s3-attachments.md)

## 6. 권한 규칙
- 시민: 본인 민원 첨부파일만 다운로드 가능
- `OFFICER`, `ADMIN`: 다운로드 가능
- 민원 또는 첨부파일이 없으면 `404 RESOURCE_NOT_FOUND`
- 권한이 없으면 `403 FORBIDDEN`

## 7. 현재 구현 기준 동작 정리
- 민원 접수 시:
  - 메타데이터 저장
  - `local`이면 실제 파일도 저장
- 민원 상세 조회 시:
  - 첨부파일 메타데이터만 반환
- 첨부파일 다운로드 시:
  - 권한 검사 후 실제 파일 스트림 반환

## 8. 운영 전 확인사항
1. `FILE_STORAGE_TYPE=s3` 설정
2. `S3_ATTACHMENT_BUCKET`, `S3_ATTACHMENT_PREFIX`, `AWS_REGION` 설정
3. EC2/ECS 역할에 버킷 prefix 권한 부여
4. 실제 업로드 후 DB `file_path`에 S3 object key가 저장되는지 확인
