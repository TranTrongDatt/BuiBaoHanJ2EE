package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Google reCAPTCHA Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "google.recaptcha")
public class RecaptchaProperties {
    
    /**
     * Whether reCAPTCHA is enabled
     */
    private boolean enabled = true;
    
    /**
     * reCAPTCHA site key (public key)
     */
    private String siteKey = "";
    
    /**
     * reCAPTCHA secret key (private key)
     */
    private String secret = "";
    
    /**
     * reCAPTCHA verification URL
     */
    private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";
    
    /**
     * Minimum score threshold for v3 (0.0-1.0)
     */
    private double threshold = 0.5;
}
