package fit.hutech.BuiBaoHan.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a payment operation fails
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentFailedException extends RuntimeException {
    
    private final String paymentMethod;
    private final String transactionId;
    private final String errorCode;

    public PaymentFailedException(String paymentMethod, String errorCode, String message) {
        super(String.format("Payment failed via %s: %s (Code: %s)", paymentMethod, message, errorCode));
        this.paymentMethod = paymentMethod;
        this.transactionId = null;
        this.errorCode = errorCode;
    }

    public PaymentFailedException(String paymentMethod, String transactionId, String errorCode, String message) {
        super(String.format("Payment failed via %s [%s]: %s (Code: %s)", 
                paymentMethod, transactionId, message, errorCode));
        this.paymentMethod = paymentMethod;
        this.transactionId = transactionId;
        this.errorCode = errorCode;
    }

    public PaymentFailedException(String message) {
        super(message);
        this.paymentMethod = null;
        this.transactionId = null;
        this.errorCode = null;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
