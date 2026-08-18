package com.gcivil.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterComplaintResponseRequest {

    @NotBlank(message = "responseContent 는 필수입니다.")
    @Size(min = 20, max = 3000, message = "responseContent 는 20자 이상 3000자 이하여야 합니다.")
    private String responseContent;

    private Boolean isPublic;

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}
