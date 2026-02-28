package fit.hutech.BuiBaoHan.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import fit.hutech.BuiBaoHan.config.properties.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cloudinary configuration - chỉ tạo bean khi enabled=true và có config hợp lệ
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CloudinaryConfig {
    
    private final CloudinaryProperties properties;
    
    /**
     * Tạo Cloudinary bean khi:
     * - cloudinary.enabled=true
     * - Có đủ cloud-name, api-key, api-secret
     */
    @Bean
    @ConditionalOnProperty(name = "cloudinary.enabled", havingValue = "true")
    public Cloudinary cloudinary() {
        if (!properties.isConfigured()) {
            log.warn("Cloudinary is enabled but not properly configured! Check your credentials.");
            return null;
        }
        
        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", properties.getCloudName(),
            "api_key", properties.getApiKey(),
            "api_secret", properties.getApiSecret(),
            "secure", true
        ));
        
        log.info("Cloudinary configured successfully for cloud: {}", properties.getCloudName());
        return cloudinary;
    }
}
