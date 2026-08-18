package com.gcivil.complaint.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChangeComplaintStatusRequest {

    @NotNull(message = "newStatus 는 필수입니다.")
    private String newStatus;

    @Size(max = 500, message = "changeMemo 는 500자 이하여야 합니다.")
    private String changeMemo;

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangeMemo() {
        return changeMemo;
    }

    public void setChangeMemo(String changeMemo) {
        this.changeMemo = changeMemo;
    }
}
