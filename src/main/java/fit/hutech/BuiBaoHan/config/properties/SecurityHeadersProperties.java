package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Security Headers Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "security.headers")
public class SecurityHeadersProperties {
    
    /**
     * HSTS (HTTP Strict Transport Security) configuration
     */
    private Hsts hsts = new Hsts();
    
    /**
     * Content Security Policy header value
     */
    private String csp = "default-src 'self'";
    
    /**
     * X-Frame-Options header value
     */
    private String frameOptions = "SAMEORIGIN";
    
    /**
     * X-Content-Type-Options header value
     */
    private String contentTypeOptions = "nosniff";
    
    /**
     * X-XSS-Protection header value
     */
    private String xssProtection = "1; mode=block";
    
    /**
     * Referrer-Policy header value
     */
    private String referrerPolicy = "strict-origin-when-cross-origin";
    
    @Data
    public static class Hsts {
        /**
         * Whether HSTS is enabled
         */
        private boolean enabled = true;
        
        /**
         * Max age in seconds (default: 1 year)
         */
        private long maxAge = 31536000;
        
        /**
         * Include subdomains
         */
        private boolean includeSubDomains = true;
        
        /**
         * Preload directive
         */
        private boolean preload = false;
    }
}
