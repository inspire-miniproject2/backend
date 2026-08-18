package com.gcivil.user.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "user.internal")
public class InternalUserProperties {

    private List<String> allowedCallers = new ArrayList<>();

    public List<String> getAllowedCallers() {
        return allowedCallers;
    }

    public void setAllowedCallers(List<String> allowedCallers) {
        this.allowedCallers = allowedCallers;
    }
}
