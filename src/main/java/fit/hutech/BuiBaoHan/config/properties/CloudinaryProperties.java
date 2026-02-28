package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Cloudinary cloud storage configuration properties
 * Đăng ký tại: https://cloudinary.com/
 * Free tier: 25GB storage + 25K transformations/month
 */
@Data
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    
    /**
     * Cloudinary Cloud Name
     */
    private String cloudName;
    
    /**
     * Cloudinary API Key
     */
    private String apiKey;
    
    /**
     * Cloudinary API Secret
     */
    private String apiSecret;
    
    /**
     * Enable/disable Cloudinary (use local storage if disabled)
     */
    private boolean enabled = false;
    
    /**
     * Base folder for all uploads in Cloudinary
     */
    private String folder = "miniverse";
    
    /**
     * Default image quality (auto, best, good, eco, low)
     */
    private String quality = "auto:good";
    
    /**
     * Auto format detection (webp, avif for modern browsers)
     */
    private boolean autoFormat = true;
    
    /**
     * Check if Cloudinary is properly configured
     */
    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
