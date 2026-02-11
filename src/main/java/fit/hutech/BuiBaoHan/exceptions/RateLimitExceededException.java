package fit.hutech.BuiBaoHan.exceptions;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception khi bị rate limited (429)
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class RateLimitExceededException extends RuntimeException {
    
    private final long retryAfterSeconds;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 60; // default 60 seconds
    }
    
    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
    
    public Duration getRetryAfter() {
        return Duration.ofSeconds(retryAfterSeconds);
    }
}
