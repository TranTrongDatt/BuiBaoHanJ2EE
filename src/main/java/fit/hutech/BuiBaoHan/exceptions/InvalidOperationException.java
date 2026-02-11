package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is invalid in the current state
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOperationException extends RuntimeException {
    
    private final String operation;
    private final String reason;

    public InvalidOperationException(String operation, String reason) {
        super(String.format("Invalid operation '%s': %s", operation, reason));
        this.operation = operation;
        this.reason = reason;
    }

    public InvalidOperationException(String message) {
        super(message);
        this.operation = null;
        this.reason = null;
    }

    public String getOperation() {
        return operation;
    }

    public String getReason() {
        return reason;
    }
}
