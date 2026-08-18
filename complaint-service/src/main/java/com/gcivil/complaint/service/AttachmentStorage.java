package com.gcivil.complaint.service;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentStorage {
    void store(String filePath, MultipartFile multipartFile) throws IOException;

    Resource load(String filePath) throws IOException;
}
