package com.gcivil.assignment.config;

import com.gcivil.assignment.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalCallerInterceptor implements HandlerInterceptor {

    private static final String INTERNAL_CALLER_HEADER = "X-Internal-Caller";
    private static final String INTERNAL_PATH_PATTERN = "/api/v1/internal/**";

    private final AssignmentInternalProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public InternalCallerInterceptor(AssignmentInternalProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        if (!pathMatcher.match(INTERNAL_PATH_PATTERN, requestUri)) {
            return true;
        }

        String internalCaller = request.getHeader(INTERNAL_CALLER_HEADER);
        if (internalCaller == null || !properties.getAllowedCaller().equals(internalCaller)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "허용된 내부 서비스 호출이 아닙니다.");
        }
        return true;
    }
}
