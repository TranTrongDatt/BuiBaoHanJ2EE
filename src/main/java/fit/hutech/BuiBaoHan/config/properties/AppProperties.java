package fit.hutech.BuiBaoHan.config.properties;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Application general configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    
    /**
     * Application name
     */
    private String name = "MiniVerse";
    
    /**
     * Application version
     */
    private String version = "1.0.0";
    
    /**
     * Application environment
     */
    private String environment = "development";
    
    /**
     * Base URL (alias: baseUrl)
     */
    private String url = "http://localhost:9090";
    
    /**
     * Base URL (deprecated, use 'url' instead)
     */
    private String baseUrl = "http://localhost:9090";
    
    /**
     * Initialize sample data on startup
     */
    private boolean initData = true;
    
    /**
     * Admin email
     */
    private String adminEmail = "admin@miniverse.com";
    
    /**
     * Support email
     */
    private String supportEmail = "support@miniverse.com";
    
    /**
     * Contact phone
     */
    private String contactPhone = "+84-000-000-000";
    
    /**
     * Default timezone
     */
    private String timezone = "Asia/Ho_Chi_Minh";
    
    /**
     * Default locale
     */
    private String locale = "vi_VN";
    
    /**
     * Pagination configuration
     */
    private Pagination pagination = new Pagination();
    
    /**
     * Security configuration
     */
    private Security security = new Security();
    
    /**
     * Email configuration
     */
    private Email email = new Email();
    
    @Data
    public static class Pagination {
        /**
         * Default page size
         */
        private int defaultSize = 12;
        
        /**
         * Maximum page size
         */
        private int maxSize = 100;
        
        /**
         * Default sort field
         */
        private String defaultSort = "createdAt";
        
        /**
         * Default sort direction
         */
        private String defaultDirection = "desc";
    }
    
    @Data
    public static class Security {
        /**
         * JWT secret key
         */
        private String jwtSecret;
        
        /**
         * JWT expiration in hours
         */
        private int jwtExpirationHours = 24;
        
        /**
         * Refresh token expiration in days
         */
        private int refreshTokenExpirationDays = 7;
        
        /**
         * Remember me duration in days
         */
        private int rememberMeDays = 30;
        
        /**
         * Maximum login attempts before lockout
         */
        private int maxLoginAttempts = 5;
        
        /**
         * Lockout duration in minutes
         */
        private int lockoutDurationMinutes = 15;
        
        /**
         * Password reset token expiration in hours
         */
        private int passwordResetExpirationHours = 24;
        
        /**
         * Email verification token expiration in hours
         */
        private int emailVerificationExpirationHours = 48;
        
        /**
         * Allowed origins for CORS
         */
        private List<String> allowedOrigins = Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173"
        );
    }
    
    @Data
    public static class Email {
        /**
         * Enable email sending
         */
        private boolean enabled = true;
        
        /**
         * From address
         */
        private String fromAddress = "noreply@miniverse.com";
        
        /**
         * From name
         */
        private String fromName = "MiniVerse";
        
        /**
         * Send async
         */
        private boolean async = true;
        
        /**
         * Retry attempts
         */
        private int retryAttempts = 3;
        
        /**
         * Retry delay in seconds
         */
        private int retryDelaySeconds = 5;
    }
}
