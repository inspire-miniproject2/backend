package com.gcivil.assignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assignment.internal")
public class AssignmentInternalProperties {

    private String allowedCaller = "complaint-service";
    private String userServiceCaller = "assignment-service";

    public String getAllowedCaller() {
        return allowedCaller;
    }

    public void setAllowedCaller(String allowedCaller) {
        this.allowedCaller = allowedCaller;
    }

    public String getUserServiceCaller() {
        return userServiceCaller;
    }

    public void setUserServiceCaller(String userServiceCaller) {
        this.userServiceCaller = userServiceCaller;
    }
}
