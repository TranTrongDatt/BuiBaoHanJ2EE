package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when user exceeds borrow limit
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class BorrowLimitExceededException extends RuntimeException {
    
    private final int currentBorrows;
    private final int maxBorrows;

    public BorrowLimitExceededException(int currentBorrows, int maxBorrows) {
        super(String.format("Borrow limit exceeded. You have %d active borrows out of maximum %d allowed.", 
                currentBorrows, maxBorrows));
        this.currentBorrows = currentBorrows;
        this.maxBorrows = maxBorrows;
    }

    public BorrowLimitExceededException(String message) {
        super(message);
        this.currentBorrows = 0;
        this.maxBorrows = 0;
    }

    public int getCurrentBorrows() {
        return currentBorrows;
    }

    public int getMaxBorrows() {
        return maxBorrows;
    }
}
