# Complaint S3 Deployment Checklist

작성일: 2026-08-19

이 문서는 `complaint-service` 첨부파일을 S3로 운영하기 전에 확인할 항목을 정리합니다.

## 1. 애플리케이션 설정

- [ ] 배포 환경의 `FILE_STORAGE_TYPE`가 `s3`인지 확인
- [ ] `AWS_REGION`가 실제 버킷 리전과 같은지 확인
- [ ] `S3_ATTACHMENT_BUCKET`가 실제 운영 버킷명인지 확인
- [ ] `S3_ATTACHMENT_PREFIX`가 `complaints/` 또는 팀 합의 prefix인지 확인
- [ ] `complaint-service`가 위 환경변수를 읽도록 재배포되었는지 확인

권장 값 예시:

```env
FILE_STORAGE_TYPE=s3
AWS_REGION=ap-northeast-2
S3_ATTACHMENT_BUCKET=minwonon-attachments-prod
S3_ATTACHMENT_PREFIX=complaints/
```

## 2. AWS IAM / S3 권한

- [ ] `complaint-service`가 동작하는 EC2 또는 ECS Task Role이 연결되어 있는지 확인
- [ ] 정적 Access Key를 `.env`, 소스코드, Docker 이미지에 넣지 않았는지 확인
- [ ] IAM 정책이 아래 권한만 포함하는지 확인
  - `s3:ListBucket`
  - `s3:PutObject`
  - `s3:GetObject`
  - `s3:DeleteObject`
- [ ] IAM 정책 범위가 전체 버킷이 아니라 `complaints/` prefix 기준인지 확인

## 3. S3 버킷 설정

- [ ] 버킷이 `private`인지 확인
- [ ] S3 Block Public Access 4개가 모두 켜져 있는지 확인
- [ ] Object Ownership이 `Bucket owner enforced`인지 확인
- [ ] Public ACL을 사용하지 않는지 확인
- [ ] 업로드 대상 prefix가 `complaints/`로 생성되는지 확인

## 4. 런타임 확인

배포 인스턴스 안에서 아래를 확인합니다.

```bash
aws sts get-caller-identity
aws s3 ls "s3://${S3_ATTACHMENT_BUCKET}/${S3_ATTACHMENT_PREFIX}"
```

- [ ] `aws sts get-caller-identity`가 기대한 Role ARN을 반환하는지 확인
- [ ] `aws s3 ls`가 권한 오류 없이 실행되는지 확인

## 5. 업로드 기능 확인

테스트 순서:

1. `POST /api/v1/complaints`로 첨부파일 1건 포함 민원 접수
2. 응답이 `201 Created`인지 확인
3. `complaint_attachments`에 메타데이터가 저장됐는지 확인
4. `file_path`가 로컬 경로가 아니라 S3 object key인지 확인
5. 실제 버킷에 object가 생성됐는지 확인

확인 포인트:

- [ ] DB `complaint_attachments.file_path` 예시:
  - `complaints/CIV-2026-000001/{uuid}.jpg`
- [ ] S3 object key가 DB 값과 일치하는지 확인
- [ ] `original_filename`, `stored_filename`, `content_type`, `file_size`가 정상 저장됐는지 확인

## 6. 다운로드 기능 확인

테스트 순서:

1. `GET /api/v1/complaints/{complaintId}/attachments/{attachmentId}` 호출
2. 본인 민원은 시민 계정으로 다운로드 가능한지 확인
3. 공무원/관리자도 다운로드 가능한지 확인
4. 권한 없는 시민은 `403`인지 확인
5. 존재하지 않는 파일은 `404`인지 확인

- [ ] `Content-Disposition`에 원본 파일명이 들어가는지 확인
- [ ] `Content-Type`이 업로드 파일 타입과 맞는지 확인
- [ ] 다운로드된 바이트가 원본과 같은지 확인

## 7. 장애 상황 확인

- [ ] 버킷명이 비어 있으면 업로드가 `SERVICE_UNAVAILABLE` 또는 내부 오류로 드러나는지 확인
- [ ] S3 권한이 없을 때 업로드 실패가 로그에 남는지 확인
- [ ] 존재하지 않는 object key는 다운로드 시 `404 RESOURCE_NOT_FOUND`인지 확인

## 8. 배포 후 모니터링

- [ ] `complaint-service` 로그에 S3 관련 예외가 없는지 확인
- [ ] 첫 업로드/다운로드 이후 CloudWatch 또는 애플리케이션 로그를 점검
- [ ] 과도한 5xx 응답이 없는지 확인

## 9. 완료 기준

- [ ] 첨부파일 업로드 성공
- [ ] S3 object 생성 확인
- [ ] 다운로드 성공
- [ ] 권한 제어 정상 동작
- [ ] 로그상 오류 없음
