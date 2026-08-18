package com.gcivil.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public class CreateComplaintRequest {

    @NotNull
    @Positive
    private Long categoryId;

    @NotBlank
    private String categoryCode;

    @NotBlank
    @Size(min = 5, max = 100)
    private String title;

    @NotBlank
    @Size(min = 20, max = 3000)
    private String content;

    private List<String> notifyChannels = new ArrayList<>();

    private List<MultipartFile> attachmentFiles = new ArrayList<>();

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getNotifyChannels() {
        return notifyChannels;
    }

    public void setNotifyChannels(List<String> notifyChannels) {
        this.notifyChannels = notifyChannels == null ? new ArrayList<>() : notifyChannels;
    }

    public List<MultipartFile> getAttachmentFiles() {
        return attachmentFiles;
    }

    public void setAttachmentFiles(List<MultipartFile> attachmentFiles) {
        this.attachmentFiles = attachmentFiles == null ? new ArrayList<>() : attachmentFiles;
    }
}
