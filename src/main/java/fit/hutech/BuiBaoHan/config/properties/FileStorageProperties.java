package fit.hutech.BuiBaoHan.config.properties;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * File storage configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {
    
    /**
     * Base upload directory
     */
    private String uploadDir = "uploads";
    
    /**
     * Book cover images directory
     */
    private String bookCoverDir = "uploads/books";
    
    /**
     * User avatar directory
     */
    private String avatarDir = "uploads/avatars";
    
    /**
     * Blog images directory
     */
    private String blogDir = "uploads/blog";
    
    /**
     * Temporary files directory
     */
    private String tempDir = "uploads/temp";
    
    /**
     * Maximum file size in bytes (default: 5MB)
     */
    private long maxFileSize = 5 * 1024 * 1024;
    
    /**
     * Allowed image extensions
     */
    private List<String> allowedImageTypes = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );
    
    /**
     * Allowed document extensions
     */
    private List<String> allowedDocumentTypes = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx"
    );
    
    /**
     * Image compression quality (0.0 - 1.0)
     */
    private double imageQuality = 0.8;
    
    /**
     * Maximum image width for resizing
     */
    private int maxImageWidth = 1920;
    
    /**
     * Maximum image height for resizing
     */
    private int maxImageHeight = 1080;
    
    /**
     * Thumbnail width
     */
    private int thumbnailWidth = 200;
    
    /**
     * Thumbnail height
     */
    private int thumbnailHeight = 300;
    
    /**
     * Use cloud storage (S3, etc.)
     */
    private boolean useCloudStorage = false;
    
    /**
     * Cloud storage configuration
     */
    private CloudStorage cloud = new CloudStorage();
    
    @Data
    public static class CloudStorage {
        /**
         * Cloud provider (s3, gcs, azure)
         */
        private String provider = "s3";
        
        /**
         * Bucket name
         */
        private String bucket;
        
        /**
         * Region
         */
        private String region = "ap-southeast-1";
        
        /**
         * Access key
         */
        private String accessKey;
        
        /**
         * Secret key
         */
        private String secretKey;
        
        /**
         * CDN URL prefix
         */
        private String cdnUrl;
    }
}
