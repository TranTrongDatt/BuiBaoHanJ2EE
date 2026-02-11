package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a book is not available for borrowing or purchase
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BookNotAvailableException extends RuntimeException {
    
    private final Long bookId;
    private final String bookTitle;
    private final AvailabilityType type;

    public enum AvailabilityType {
        OUT_OF_STOCK,
        ALL_COPIES_BORROWED,
        RESERVED,
        DISCONTINUED
    }

    public BookNotAvailableException(Long bookId, String bookTitle, AvailabilityType type) {
        super(String.format("Book '%s' (ID: %d) is not available: %s", 
                bookTitle, bookId, getTypeMessage(type)));
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.type = type;
    }

    public BookNotAvailableException(String message) {
        super(message);
        this.bookId = null;
        this.bookTitle = null;
        this.type = null;
    }

    private static String getTypeMessage(AvailabilityType type) {
        return switch (type) {
            case OUT_OF_STOCK -> "Out of stock for purchase";
            case ALL_COPIES_BORROWED -> "All copies are currently borrowed";
            case RESERVED -> "Book is reserved";
            case DISCONTINUED -> "Book has been discontinued";
        };
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public AvailabilityType getType() {
        return type;
    }
}
