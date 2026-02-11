package fit.hutech.BuiBaoHan.config.properties;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Library configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "library")
public class LibraryProperties {
    
    /**
     * Maximum books a user can borrow at once
     */
    private int maxBorrowLimit = 5;
    
    /**
     * Default borrow duration in days
     */
    private int borrowDurationDays = 14;
    
    /**
     * Maximum renewals allowed
     */
    private int maxRenewals = 2;
    
    /**
     * Renewal extension days
     */
    private int renewalDays = 7;
    
    /**
     * Days before due date to send reminder
     */
    private int reminderDaysBeforeDue = 3;
    
    /**
     * Late fee per day
     */
    private BigDecimal lateFeePerDay = new BigDecimal("5000");
    
    /**
     * Maximum late fee
     */
    private BigDecimal maxLateFee = new BigDecimal("100000");
    
    /**
     * Lost book fee multiplier (x times the book price)
     */
    private double lostBookFeeMultiplier = 1.5;
    
    /**
     * Days until book is considered lost
     */
    private int daysUntilLost = 30;
    
    /**
     * Library card configuration
     */
    private LibraryCard card = new LibraryCard();
    
    /**
     * Reservation configuration
     */
    private Reservation reservation = new Reservation();
    
    @Data
    public static class LibraryCard {
        /**
         * Card validity period in years
         */
        private int validityYears = 1;
        
        /**
         * Card issuance fee
         */
        private BigDecimal issuanceFee = new BigDecimal("50000");
        
        /**
         * Card renewal fee
         */
        private BigDecimal renewalFee = new BigDecimal("30000");
        
        /**
         * Replacement fee for lost card
         */
        private BigDecimal replacementFee = new BigDecimal("50000");
        
        /**
         * Days before expiry to send renewal reminder
         */
        private int renewalReminderDays = 30;
        
        /**
         * Auto-generate card number
         */
        private boolean autoGenerateNumber = true;
        
        /**
         * Card number prefix
         */
        private String numberPrefix = "MV";
    }
    
    @Data
    public static class Reservation {
        /**
         * Enable book reservation
         */
        private boolean enabled = true;
        
        /**
         * Maximum reservations per user
         */
        private int maxPerUser = 3;
        
        /**
         * Days to hold reserved book
         */
        private int holdDays = 3;
        
        /**
         * Queue size limit per book
         */
        private int maxQueueSize = 10;
        
        /**
         * Send notification when book is available
         */
        private boolean notifyOnAvailable = true;
    }
}
