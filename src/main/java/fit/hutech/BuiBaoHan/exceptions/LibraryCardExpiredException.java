package fit.hutech.BuiBaoHan.exceptions;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a library card has expired
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class LibraryCardExpiredException extends RuntimeException {
    
    private final String cardNumber;
    private final LocalDate expiryDate;

    public LibraryCardExpiredException(String cardNumber, LocalDate expiryDate) {
        super(String.format("Library card '%s' expired on %s. Please renew your card.", 
                cardNumber, expiryDate));
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
    }

    public LibraryCardExpiredException(String message) {
        super(message);
        this.cardNumber = null;
        this.expiryDate = null;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }
}
