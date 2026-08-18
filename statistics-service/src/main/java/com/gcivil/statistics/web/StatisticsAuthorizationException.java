package com.gcivil.statistics.web;

import org.springframework.http.HttpStatus;

public class StatisticsAuthorizationException extends RuntimeException {
    private final HttpStatus status;

    public StatisticsAuthorizationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
