package fit.hutech.BuiBaoHan.controllers.advice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standardized error response format for API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String errorId,
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors,
        Map<String, Object> details
) {
    /**
     * Validation field error
     */
    public record FieldError(
            String field,
            Object rejectedValue,
            String message
    ) {}

    /**
     * Builder for ErrorResponse
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String errorId = UUID.randomUUID().toString();
        private LocalDateTime timestamp = LocalDateTime.now();
        private int status;
        private String error;
        private String message;
        private String path;
        private List<FieldError> fieldErrors;
        private Map<String, Object> details;

        public Builder errorId(String errorId) {
            this.errorId = errorId;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder fieldErrors(List<FieldError> fieldErrors) {
            this.fieldErrors = fieldErrors;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(
                    errorId, timestamp, status, error, message, path, fieldErrors, details
            );
        }
    }

    /**
     * Create a simple error response
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                null,
                null
        );
    }

    /**
     * Create error response with field errors
     */
    public static ErrorResponse withFieldErrors(int status, String error, String message, 
                                                  String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                UUID.randomUUID().toString(),
                LocalDateTime.now(),
                status,
                error,
                message,
                path,
                fieldErrors,
                null
        );
    }
}
