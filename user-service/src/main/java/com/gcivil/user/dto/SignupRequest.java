package com.gcivil.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank(message = "loginId 는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "loginId 는 영문과 숫자 6~20자여야 합니다.")
    private String loginId;

    @NotBlank(message = "password 는 필수입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{10,}$",
            message = "password 는 10자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    private String password;

    @NotBlank(message = "name 는 필수입니다.")
    @Size(min = 2, max = 50, message = "name 은 2~50자여야 합니다.")
    private String name;

    @NotBlank(message = "email 는 필수입니다.")
    @Email(message = "email 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "phone 는 필수입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "phone 은 010-0000-0000 형식이어야 합니다.")
    private String phone;

    private Boolean emailNotifyAgreed;

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getEmailNotifyAgreed() {
        return emailNotifyAgreed;
    }

    public void setEmailNotifyAgreed(Boolean emailNotifyAgreed) {
        this.emailNotifyAgreed = emailNotifyAgreed;
    }
}
