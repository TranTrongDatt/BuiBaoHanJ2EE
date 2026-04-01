package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Password Reset Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "password-reset")
public class PasswordResetProperties {
    
    /**
     * Token configuration
     */
    private Token token = new Token();
    
    /**
     * Rate limit configuration
     */
    private RateLimit rateLimit = new RateLimit();
    
    @Data
    public static class Token {
        /**
         * Token expiry time in minutes
         */
        private int expiryMinutes = 15;
    }
    
    @Data
    public static class RateLimit {
        /**
         * Maximum password reset requests per time window
         */
        private int maxRequests = 3;
        
        /**
         * Time window in minutes
         */
        private int windowMinutes = 60;
    }
}
