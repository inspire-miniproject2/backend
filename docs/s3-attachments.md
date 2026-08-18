# S3 Attachment Storage

Complaint Service stores attachment metadata in `complaint_db` and stores the original object in a private S3 bucket.

## Environment contract

| Variable | Local default | Dev deployment | Description |
|---|---|---|---|
| `FILE_STORAGE_TYPE` | `local` | `s3` | Selects the attachment storage implementation |
| `AWS_REGION` | `ap-northeast-2` | `ap-northeast-2` | AWS Seoul region |
| `S3_ATTACHMENT_BUCKET` | empty | required | Private attachment bucket name |
| `S3_ATTACHMENT_PREFIX` | `complaints/` | `complaints/` | Object key prefix owned by Complaint Service |

AWS access key and secret key must not be stored in `.env`, GitHub Secrets, source code, or Docker Compose for an EC2 deployment. Complaint Service uses the AWS SDK default credential chain and receives temporary credentials from the EC2 instance role.

## Bucket rules

- Keep all four S3 Block Public Access settings enabled.
- Keep object ownership set to bucket-owner enforced; do not use public ACLs.
- Store only an S3 object key in the database, not a public URL.
- Authorize a download in Complaint Service before reading the object from S3.
- Use the `complaints/` prefix so the instance role cannot access unrelated objects.

The current API contract returns a file stream from Complaint Service. Browser-direct upload and presigned URL CORS rules are therefore outside the current scope.

## EC2 IAM role

1. Copy `infra/aws/iam/complaint-s3-policy.json.example`.
2. Replace `REPLACE_WITH_BUCKET_NAME` with the dev bucket name.
3. Create a customer-managed IAM policy with that JSON.
4. Attach the policy to an EC2 IAM role whose trusted entity is EC2.
5. Attach the role to the deployment instance.

The policy grants only `ListBucket`, `PutObject`, `GetObject`, and `DeleteObject` under `complaints/`.

## Verification on EC2

Do not configure `aws_access_key_id` on the instance. After attaching the role, verify the temporary identity and the bucket prefix:

```bash
aws sts get-caller-identity
aws s3 ls "s3://${S3_ATTACHMENT_BUCKET}/${S3_ATTACHMENT_PREFIX}"
```

An S3 object URL returning `AccessDenied` in a normal browser is expected for a private bucket.
