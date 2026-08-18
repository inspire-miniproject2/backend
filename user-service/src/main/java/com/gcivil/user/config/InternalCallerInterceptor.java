package com.gcivil.user.config;

import com.gcivil.user.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalCallerInterceptor implements HandlerInterceptor {

    private static final String INTERNAL_CALLER_HEADER = "X-Internal-Caller";
    private static final String INTERNAL_PATH_PATTERN = "/api/v1/internal/**";

    private final InternalUserProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public InternalCallerInterceptor(InternalUserProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!pathMatcher.match(INTERNAL_PATH_PATTERN, request.getRequestURI())) {
            return true;
        }

        String internalCaller = request.getHeader(INTERNAL_CALLER_HEADER);
        List<String> allowedCallers = properties.getAllowedCallers();
        if (internalCaller == null || allowedCallers.stream().noneMatch(internalCaller::equals)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "허용된 내부 서비스 호출이 아닙니다.");
        }
        return true;
    }
}
