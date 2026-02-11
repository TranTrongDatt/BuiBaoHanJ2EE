package fit.hutech.BuiBaoHan.controllers.advice;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import fit.hutech.BuiBaoHan.exceptions.AIChatException;
import fit.hutech.BuiBaoHan.exceptions.BookNotAvailableException;
import fit.hutech.BuiBaoHan.exceptions.BorrowLimitExceededException;
import fit.hutech.BuiBaoHan.exceptions.DuplicateResourceException;
import fit.hutech.BuiBaoHan.exceptions.FileUploadException;
import fit.hutech.BuiBaoHan.exceptions.InvalidOperationException;
import fit.hutech.BuiBaoHan.exceptions.LibraryCardExpiredException;
import fit.hutech.BuiBaoHan.exceptions.PaymentFailedException;
import fit.hutech.BuiBaoHan.exceptions.RateLimitExceededException;
import fit.hutech.BuiBaoHan.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST API endpoints
 */
@RestControllerAdvice(basePackages = "fit.hutech.BuiBaoHan.controllers.api")
@Slf4j
public class ApiExceptionHandler {

    // ==================== Resource Exceptions ====================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(Map.of(
                        "resource", ex.getResourceName() != null ? ex.getResourceName() : "Unknown",
                        "field", ex.getFieldName() != null ? ex.getFieldName() : "id",
                        "value", ex.getFieldValue() != null ? ex.getFieldValue() : "N/A"
                ))
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==================== Validation Exceptions ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()
                ))
                .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed. Check 'fieldErrors' for details.",
                request.getRequestURI(),
                fieldErrors
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        getPropertyPath(violation),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());
        
        ErrorResponse error = ErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Constraint validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    private String getPropertyPath(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot > 0 ? path.substring(lastDot + 1) : path;
    }

    // ==================== Security Exceptions ====================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        log.warn("Access denied: {} - {}", request.getRequestURI(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to access this resource",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentication failed: " + ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // ==================== Business Exceptions ====================

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(
            InvalidOperationException ex, HttpServletRequest request) {
        
        log.warn("Invalid operation: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(
            PaymentFailedException ex, HttpServletRequest request) {
        
        log.error("Payment failed: {}", ex.getMessage());
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getPaymentMethod() != null) details.put("paymentMethod", ex.getPaymentMethod());
        if (ex.getTransactionId() != null) details.put("transactionId", ex.getTransactionId());
        if (ex.getErrorCode() != null) details.put("errorCode", ex.getErrorCode());
        
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .error("Payment Required")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    @ExceptionHandler(LibraryCardExpiredException.class)
    public ResponseEntity<ErrorResponse> handleLibraryCardExpired(
            LibraryCardExpiredException ex, HttpServletRequest request) {
        
        log.warn("Library card expired: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Library Card Expired",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BorrowLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleBorrowLimitExceeded(
            BorrowLimitExceededException ex, HttpServletRequest request) {
        
        log.warn("Borrow limit exceeded: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error("Borrow Limit Exceeded")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(Map.of(
                        "currentBorrows", ex.getCurrentBorrows(),
                        "maxBorrows", ex.getMaxBorrows()
                ))
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(BookNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleBookNotAvailable(
            BookNotAvailableException ex, HttpServletRequest request) {
        
        log.warn("Book not available: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Book Not Available",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==================== File Upload Exceptions ====================

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUpload(
            FileUploadException ex, HttpServletRequest request) {
        
        log.warn("File upload error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "File Upload Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        
        log.warn("File too large: {}", ex.getMessage());
        
        @SuppressWarnings("deprecation")
        int statusCode = HttpStatus.PAYLOAD_TOO_LARGE.value();
        
        ErrorResponse error = ErrorResponse.of(
                statusCode,
                "File Too Large",
                "The uploaded file exceeds the maximum allowed size",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(statusCode).body(error);
    }

    // ==================== AI Chat Exceptions ====================

    @ExceptionHandler(AIChatException.class)
    public ResponseEntity<ErrorResponse> handleAIChat(
            AIChatException ex, HttpServletRequest request) {
        
        log.error("AI Chat error: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "AI Service Error",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    // ==================== Rate Limit Exception ====================

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        
        Duration retryAfter = ex.getRetryAfter() != null ? ex.getRetryAfter() : Duration.ofSeconds(60);
        
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .details(Map.of("retryAfterSeconds", retryAfter.toSeconds()))
                .build();
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfter.toSeconds()))
                .body(error);
    }

    // ==================== Database Exceptions ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        log.error("Data integrity violation: {}", ex.getMessage());
        
        String message = "Database constraint violation";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "A record with this value already exists";
        }
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                message,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ==================== Generic Exception Handler ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error occurred: ", ex);
        
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
