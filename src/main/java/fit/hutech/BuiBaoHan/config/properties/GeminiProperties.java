package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Google Gemini AI API configuration properties
 * Sử dụng Gemini 2.0 Flash - Free tier với 15 RPM và 1,500 requests/day
 */
@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    
    /**
     * Gemini API key từ Google AI Studio
     * Lấy tại: https://aistudio.google.com/app/apikey
     */
    private String apiKey;
    
    /**
     * Model Gemini sử dụng
     * Options: gemini-2.0-flash, gemini-1.5-flash, gemini-1.5-pro
     */
    private String model = "gemini-2.0-flash";
    
    /**
     * Base URL của Gemini API
     */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    
    /**
     * Số tokens tối đa cho response
     */
    private int maxOutputTokens = 2048;
    
    /**
     * Temperature cho độ ngẫu nhiên của response (0.0 - 2.0)
     * 0.0 = deterministic, 2.0 = very creative
     */
    private double temperature = 0.7;
    
    /**
     * Top P sampling parameter
     */
    private double topP = 0.95;
    
    /**
     * Top K sampling parameter
     */
    private int topK = 40;
    
    /**
     * Request timeout in seconds
     */
    private int timeout = 30;
    
    /**
     * Enable/disable Gemini API
     */
    private boolean enabled = true;
    
    /**
     * Build full API URL for generateContent endpoint
     */
    public String getGenerateContentUrl() {
        return String.format("%s/models/%s:generateContent?key=%s", 
                baseUrl, model, apiKey);
    }
    
    /**
     * Check if API is properly configured
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY_HERE");
    }
}
