package fit.hutech.BuiBaoHan.services;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;

import fit.hutech.BuiBaoHan.config.properties.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý upload ảnh lên Cloudinary
 * 
 * Features:
 * - Upload ảnh với auto-optimization
 * - Resize ảnh tự động
 * - CDN toàn cầu
 * - Xóa ảnh khi cần
 */
@Service
@ConditionalOnBean(Cloudinary.class)
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    
    /**
     * Upload ảnh lên Cloudinary
     * 
     * @param file MultipartFile cần upload
     * @param folder Thư mục con (books, avatars, blog, categories, fields)
     * @return URL ảnh đã upload (HTTPS)
     * @throws IOException nếu upload thất bại
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        String publicId = generatePublicId(file.getOriginalFilename());
        
        Map<String, Object> options = ObjectUtils.asMap(
            "public_id", publicId,
            "folder", buildFolderPath(folder),
            "resource_type", "image",
            "quality", properties.getQuality(),
            "fetch_format", "auto",  // Auto convert to webp/avif for modern browsers
            "overwrite", true
        );
        
        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded image to Cloudinary: {} -> {}", file.getOriginalFilename(), secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new IOException("Upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload ảnh với resize tự động
     * 
     * @param file MultipartFile cần upload
     * @param folder Thư mục con
     * @param width Chiều rộng mong muốn
     * @param height Chiều cao mong muốn
     * @return URL ảnh đã upload và resize
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public String uploadImageWithResize(MultipartFile file, String folder, int width, int height) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        String publicId = generatePublicId(file.getOriginalFilename());
        
        // Eager transformation - resize ngay khi upload
        Transformation transformation = new Transformation()
            .width(width)
            .height(height)
            .crop("fill")
            .gravity("auto")  // Smart crop - focus on important part
            .quality("auto:good")
            .fetchFormat("auto");
        
        Map<String, Object> options = ObjectUtils.asMap(
            "public_id", publicId,
            "folder", buildFolderPath(folder),
            "resource_type", "image",
            "eager", transformation.generate(),
            "overwrite", true
        );
        
        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), options);
            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded and resized image ({}x{}) to Cloudinary: {}", width, height, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new IOException("Upload to Cloudinary failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload avatar với resize 200x200 và crop tròn
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        return uploadImageWithResize(file, "avatars", 200, 200);
    }
    
    /**
     * Upload ảnh bìa sách với resize 400x600
     */
    public String uploadBookCover(MultipartFile file) throws IOException {
        return uploadImageWithResize(file, "books", 400, 600);
    }
    
    /**
     * Upload ảnh blog với resize 800x450 (16:9)
     */
    public String uploadBlogImage(MultipartFile file) throws IOException {
        return uploadImageWithResize(file, "blog", 800, 450);
    }
    
    /**
     * Upload ảnh category với resize 300x300
     */
    public String uploadCategoryImage(MultipartFile file) throws IOException {
        return uploadImageWithResize(file, "categories", 300, 300);
    }
    
    /**
     * Xóa ảnh khỏi Cloudinary
     * 
     * @param publicId Public ID của ảnh (hoặc URL)
     * @return true nếu xóa thành công
     */
    @SuppressWarnings("unchecked")
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return false;
        }
        
        // Nếu là URL, extract public_id
        String id = extractPublicId(publicId);
        if (id == null) {
            id = publicId;
        }
        
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(id, ObjectUtils.emptyMap());
            boolean success = "ok".equals(result.get("result"));
            if (success) {
                log.info("Deleted image from Cloudinary: {}", id);
            } else {
                log.warn("Failed to delete image from Cloudinary: {} - Result: {}", id, result);
            }
            return success;
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", id, e);
            return false;
        }
    }
    
    /**
     * Xóa ảnh bằng URL
     */
    public boolean deleteImageByUrl(String url) {
        String publicId = extractPublicId(url);
        if (publicId != null) {
            return deleteImage(publicId);
        }
        return false;
    }
    
    /**
     * Tạo URL với transformation (resize on-the-fly)
     * Không cần re-upload, chỉ thay đổi URL
     * 
     * @param originalUrl URL gốc từ Cloudinary
     * @param width Chiều rộng mong muốn
     * @param height Chiều cao mong muốn
     * @return URL đã transform
     */
    public String getResizedUrl(String originalUrl, int width, int height) {
        if (originalUrl == null || !originalUrl.contains("cloudinary.com")) {
            return originalUrl;
        }
        
        // Insert transformation before /upload/
        // From: https://res.cloudinary.com/xxx/image/upload/v123/folder/file.jpg
        // To:   https://res.cloudinary.com/xxx/image/upload/w_200,h_200,c_fill/v123/folder/file.jpg
        String transformation = String.format("w_%d,h_%d,c_fill,q_auto,f_auto", width, height);
        return originalUrl.replace("/upload/", "/upload/" + transformation + "/");
    }
    
    /**
     * Lấy thumbnail URL (150x150)
     */
    public String getThumbnailUrl(String originalUrl) {
        return getResizedUrl(originalUrl, 150, 150);
    }
    
    /**
     * Extract public_id từ Cloudinary URL
     * 
     * URL format: https://res.cloudinary.com/cloud_name/image/upload/v123456789/folder/filename.jpg
     * Public ID: folder/filename
     */
    public String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            // Find /upload/ and extract the path after it
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                
                // Remove version if present (v123456789/)
                if (path.matches("^v\\d+/.*")) {
                    path = path.substring(path.indexOf('/') + 1);
                }
                
                // Remove file extension
                int lastDot = path.lastIndexOf('.');
                if (lastDot > 0) {
                    path = path.substring(0, lastDot);
                }
                
                return path;
            }
        } catch (Exception e) {
            log.warn("Failed to extract public_id from URL: {}", url);
        }
        
        return null;
    }
    
    /**
     * Check if URL is from Cloudinary
     */
    public boolean isCloudinaryUrl(String url) {
        return url != null && url.contains("res.cloudinary.com");
    }
    
    // ==================== Private Helper Methods ====================
    
    /**
     * Build full folder path: miniverse/books, miniverse/avatars, etc.
     */
    private String buildFolderPath(String subFolder) {
        String baseFolder = properties.getFolder();
        if (subFolder == null || subFolder.isBlank()) {
            return baseFolder;
        }
        return baseFolder + "/" + subFolder.trim().toLowerCase();
    }
    
    /**
     * Generate unique public_id from filename
     */
    private String generatePublicId(String originalFilename) {
        String baseName = originalFilename;
        if (originalFilename != null && originalFilename.contains(".")) {
            baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        }
        
        // Sanitize filename (remove special chars)
        String sanitized = baseName != null ? 
            baseName.replaceAll("[^a-zA-Z0-9-_]", "_").toLowerCase() : "image";
        
        // Add UUID suffix for uniqueness
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return sanitized + "_" + uniqueId;
    }
}
