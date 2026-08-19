package com.gcivil.complaint.service;

import com.gcivil.complaint.config.AttachmentStorageProperties;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(prefix = "gcivil.attachment.storage", name = "type", havingValue = "s3")
public class S3AttachmentStorage implements AttachmentStorage {

    private final AttachmentStorageProperties properties;
    private final S3Client s3Client;

    public S3AttachmentStorage(AttachmentStorageProperties properties, S3Client s3Client) {
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public void store(String filePath, MultipartFile multipartFile) throws IOException {
        String bucket = requireBucket();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .contentType(multipartFile.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));
        } catch (SdkException ex) {
            throw new IOException("failed to store attachment in s3", ex);
        }
    }

    @Override
    public Resource load(String filePath) throws IOException {
        String bucket = requireBucket();
        try {
            return new InputStreamResource(s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .build()));
        } catch (NoSuchKeyException ex) {
            return null;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return null;
            }
            throw new IOException("failed to load attachment from s3", ex);
        } catch (SdkException ex) {
            throw new IOException("failed to load attachment from s3", ex);
        }
    }

    private String requireBucket() {
        String bucket = properties.getS3Bucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("s3 attachment storage bucket is not configured");
        }
        return bucket.trim();
    }
}
