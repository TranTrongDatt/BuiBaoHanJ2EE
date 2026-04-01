package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * JWT Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * Secret key for signing JWT tokens
     */
    private String secret = "change-me-in-secrets";
    
    /**
     * Access token expiration time in milliseconds (default: 15 minutes)
     */
    private long expiration = 900000;
    
    /**
     * Refresh token expiration time in milliseconds (default: 7 days)
     */
    private long refreshExpiration = 604800000;
    
    /**
     * JWT Cookie configuration
     */
    private Cookie cookie = new Cookie();
    
    @Data
    public static class Cookie {
        /**
         * Access token cookie name
         */
        private String name = "MV_ACCESS_TOKEN";
        
        /**
         * Refresh token cookie name
         */
        private String refreshName = "MV_REFRESH_TOKEN";
        
        /**
         * Whether cookie should be secure (HTTPS only)
         */
        private boolean secure = false;
        
        /**
         * SameSite attribute for cookies
         */
        private String sameSite = "Strict";
        
        /**
         * Cookie domain
         */
        private String domain = "";
    }
}
