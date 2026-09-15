package com.logistics.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldDetail> details
) {
    public record FieldDetail(String field, String reason) {}

    public static ErrorResponse of(int status, String code, String message, String path) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String message, String path, List<FieldDetail> details) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, path, details);
    }
}