package com.gcivil.complaint.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcivil.complaint.config.AttachmentStorageProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3AttachmentStorageTest {

    private final S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
    private final AttachmentStorageProperties properties = new AttachmentStorageProperties();
    private final S3AttachmentStorage storage;

    S3AttachmentStorageTest() {
        properties.setType("s3");
        properties.setS3Bucket("gcivil-attachments");
        storage = new S3AttachmentStorage(properties, s3Client);
    }

    @Test
    void storesMultipartFileToConfiguredBucketAndKey() throws IOException {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "attachmentFiles",
                "evidence.txt",
                "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8)
        );

        storage.store("complaints/CIV-2026-000001/evidence.txt", multipartFile);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }

    @Test
    void loadsAttachmentStreamFromConfiguredBucket() throws IOException {
        byte[] bytes = "download me".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> inputStream = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) bytes.length).build(),
                AbortableInputStream.create(new java.io.ByteArrayInputStream(bytes))
        );
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(inputStream);

        var resource = storage.load("complaints/CIV-2026-000001/evidence.txt");

        assertThat(resource).isNotNull();
        assertThat(resource.getInputStream().readAllBytes()).isEqualTo(bytes);
    }

    @Test
    void returnsNullWhenObjectDoesNotExist() throws IOException {
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertThat(storage.load("complaints/CIV-2026-000001/missing.txt")).isNull();
    }

    @Test
    void rejectsS3UsageWithoutBucket() {
        AttachmentStorageProperties missingBucketProperties = new AttachmentStorageProperties();
        missingBucketProperties.setType("s3");
        S3AttachmentStorage missingBucketStorage = new S3AttachmentStorage(missingBucketProperties, s3Client);

        assertThatThrownBy(() -> missingBucketStorage.store(
                "complaints/CIV-2026-000001/evidence.txt",
                new MockMultipartFile("attachmentFiles", "evidence.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOf(IllegalStateException.class);
    }
}
