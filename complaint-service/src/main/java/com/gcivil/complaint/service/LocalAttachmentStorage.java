package com.gcivil.complaint.service;

import com.gcivil.complaint.config.AttachmentStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Primary
@Component
public class LocalAttachmentStorage implements AttachmentStorage {

    private final AttachmentStorageProperties properties;

    public LocalAttachmentStorage(AttachmentStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void store(String filePath, MultipartFile multipartFile) throws IOException {
        assertLocalStorageEnabled();
        Path target = resolve(filePath);
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = multipartFile.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public Resource load(String filePath) throws IOException {
        assertLocalStorageEnabled();
        Path target = resolve(filePath);
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            return null;
        }
        return new PathResource(target);
    }

    private Path resolve(String filePath) {
        return Path.of(properties.getLocalRoot()).resolve(filePath).normalize();
    }

    private void assertLocalStorageEnabled() {
        if (!"local".equalsIgnoreCase(properties.getType())) {
            throw new IllegalStateException("local attachment storage is disabled");
        }
    }
}
