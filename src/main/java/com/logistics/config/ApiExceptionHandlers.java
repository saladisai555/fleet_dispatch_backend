package com.logistics.config;

import tools.jackson.databind.ObjectMapper;
import com.logistics.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiExceptionHandlers {

    private final ObjectMapper objectMapper;

    public ApiExceptionHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> writeError(
                response,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication is required to access this resource",
                request.getRequestURI()
        );
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> writeError(
                response,
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You do not have permission to perform this action",
                request.getRequestURI()
        );
    }

    private void writeError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String code,
            String message,
            String path
    ) throws java.io.IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");

        ErrorResponse error =
                ErrorResponse.of(status.value(), code, message, path);

        response.getWriter().write(
                objectMapper.writeValueAsString(error)
        );
    }
}