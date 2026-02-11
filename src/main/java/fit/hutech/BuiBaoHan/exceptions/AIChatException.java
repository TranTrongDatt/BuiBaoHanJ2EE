package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when AI chat service encounters an error
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AIChatException extends RuntimeException {
    
    private final AIChatError errorType;
    private final String provider;

    public enum AIChatError {
        API_KEY_INVALID,
        RATE_LIMIT_EXCEEDED,
        CONTEXT_TOO_LONG,
        SERVICE_UNAVAILABLE,
        RESPONSE_TIMEOUT,
        CONTENT_FILTERED,
        QUOTA_EXCEEDED
    }

    public AIChatException(AIChatError errorType, String provider) {
        super(String.format("AI Chat error with %s: %s", provider, getErrorMessage(errorType)));
        this.errorType = errorType;
        this.provider = provider;
    }

    public AIChatException(AIChatError errorType, String provider, String details) {
        super(String.format("AI Chat error with %s: %s - %s", 
                provider, getErrorMessage(errorType), details));
        this.errorType = errorType;
        this.provider = provider;
    }

    public AIChatException(String message) {
        super(message);
        this.errorType = null;
        this.provider = null;
    }

    private static String getErrorMessage(AIChatError errorType) {
        return switch (errorType) {
            case API_KEY_INVALID -> "Invalid API key";
            case RATE_LIMIT_EXCEEDED -> "Rate limit exceeded, please try again later";
            case CONTEXT_TOO_LONG -> "Conversation context is too long";
            case SERVICE_UNAVAILABLE -> "AI service is temporarily unavailable";
            case RESPONSE_TIMEOUT -> "Response timed out";
            case CONTENT_FILTERED -> "Content was filtered for safety";
            case QUOTA_EXCEEDED -> "AI usage quota exceeded";
        };
    }

    public AIChatError getErrorType() {
        return errorType;
    }

    public String getProvider() {
        return provider;
    }
}
