package fit.hutech.BuiBaoHan.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý upload và lưu trữ file
 * Hỗ trợ cả Local Storage và Cloudinary Cloud Storage
 */
@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.max-size:10485760}") // 10MB default
    private long maxFileSize;
    
    @Value("${cloudinary.enabled:false}")
    private boolean cloudinaryEnabled;
    
    // Optional - chỉ inject khi Cloudinary được enable
    @Autowired(required = false)
    private CloudinaryService cloudinaryService;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/webm"
    );

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(uploadDir);
        try {
            // Create main upload directory
            Files.createDirectories(rootLocation);
            
            // Create all required subdirectories
            Files.createDirectories(rootLocation.resolve("images"));
            Files.createDirectories(rootLocation.resolve("avatars"));
            Files.createDirectories(rootLocation.resolve("covers"));
            Files.createDirectories(rootLocation.resolve("documents"));
            
            // Additional directories for specific features
            Files.createDirectories(rootLocation.resolve("books"));
            Files.createDirectories(rootLocation.resolve("categories"));
            Files.createDirectories(rootLocation.resolve("fields"));
            Files.createDirectories(rootLocation.resolve("blog"));
            Files.createDirectories(rootLocation.resolve("videos"));
            
            log.info("Upload directories initialized at: {}", rootLocation.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not initialize storage", e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * Upload file ảnh - tự động chọn Local hoặc Cloudinary
     * @param file File upload
     * @param subDir Thư mục con (images, avatars, covers, books, blog)
     * @return Đường dẫn/URL file đã lưu
     */
    public String storeImage(MultipartFile file, String subDir) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        
        // Sử dụng Cloudinary nếu được enable và service available
        if (isCloudinaryAvailable()) {
            log.debug("Uploading to Cloudinary: {}", file.getOriginalFilename());
            return cloudinaryService.uploadImage(file, subDir);
        }
        
        // Fallback to local storage
        return store(file, subDir);
    }
    
    /**
     * Upload avatar với auto-resize 200x200
     */
    public String storeAvatar(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        
        if (isCloudinaryAvailable()) {
            return cloudinaryService.uploadAvatar(file);
        }
        
        return store(file, "avatars");
    }
    
    /**
     * Upload ảnh bìa sách với auto-resize 400x600
     */
    public String storeBookCover(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        
        if (isCloudinaryAvailable()) {
            return cloudinaryService.uploadBookCover(file);
        }
        
        return store(file, "books");
    }
    
    /**
     * Upload ảnh blog với auto-resize 800x450
     */
    public String storeBlogImage(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        
        if (isCloudinaryAvailable()) {
            return cloudinaryService.uploadBlogImage(file);
        }
        
        return store(file, "blog");
    }
    
    /**
     * Upload ảnh category với auto-resize 300x300
     */
    public String storeCategoryImage(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_IMAGE_TYPES);
        
        if (isCloudinaryAvailable()) {
            return cloudinaryService.uploadCategoryImage(file);
        }
        
        return store(file, "categories");
    }
    
    /**
     * Check if Cloudinary is available and enabled
     */
    public boolean isCloudinaryAvailable() {
        return cloudinaryEnabled && cloudinaryService != null;
    }
    
    /**
     * Get storage mode info
     */
    public String getStorageMode() {
        return isCloudinaryAvailable() ? "Cloudinary (Cloud)" : "Local Storage";
    }

    /**
     * Upload file document
     * @param file File upload
     * @return Đường dẫn file đã lưu
     */
    public String storeDocument(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_DOCUMENT_TYPES);
        return store(file, "documents");
    }

    /**
     * Upload video - tự động chọn Local hoặc Cloudinary
     * @param file File video (MP4, WebM)
     * @return URL video đã lưu
     */
    public String storeVideo(MultipartFile file) throws IOException {
        validateFile(file, ALLOWED_VIDEO_TYPES);
        
        if (isCloudinaryAvailable()) {
            log.debug("Uploading video to Cloudinary: {}", file.getOriginalFilename());
            return cloudinaryService.uploadVideo(file);
        }
        
        return store(file, "videos");
    }

    /**
     * Upload PDF mô tả sách - luôn lưu local
     * (Cloudinary chặn raw file với tài khoản untrusted, nên PDF luôn lưu local để serve trực tiếp)
     * @param file File PDF
     * @return Đường dẫn file PDF đã lưu
     */
    public String storeDescriptionPdf(MultipartFile file) throws IOException {
        validateFile(file, List.of("application/pdf"));
        return store(file, "documents");
    }

    /**
     * Upload file chung
     */
    private String store(MultipartFile file, String subDir) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + extension;

        Path targetDir = rootLocation.resolve(subDir);
        
        // Ensure directory exists
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
            log.info("Created directory: {}", targetDir);
        }
        
        Path targetPath = targetDir.resolve(newFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File stored: {}", targetPath);
        return "/uploads/" + subDir + "/" + newFilename;
    }

    /**
     * Lấy file resource để download
     */
    public Resource loadAsResource(String filename) throws MalformedURLException {
        Path file = rootLocation.resolve(filename);
        Resource resource = new UrlResource(file.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return resource;
        }
        throw new RuntimeException("Could not read file: " + filename);
    }

    /**
     * Xóa file - hỗ trợ cả Local và Cloudinary
     * @param filePathOrUrl Đường dẫn local hoặc Cloudinary URL
     * @return true nếu xóa thành công
     */
    public boolean delete(String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.isBlank()) {
            return false;
        }
        
        // Kiểm tra nếu là Cloudinary URL
        if (isCloudinaryAvailable() && cloudinaryService.isCloudinaryUrl(filePathOrUrl)) {
            return cloudinaryService.deleteImageByUrl(filePathOrUrl);
        }
        
        // Xóa file local
        try {
            // Handle both /uploads/xxx and xxx formats
            String filename = filePathOrUrl;
            if (filename.startsWith("/uploads/")) {
                filename = filename.substring("/uploads/".length());
            }
            Path file = rootLocation.resolve(filename);
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Could not delete file: {}", filePathOrUrl, e);
            return false;
        }
    }
    
    /**
     * Xóa file cũ khi update (chỉ xóa nếu file mới khác file cũ)
     */
    public void deleteOldFileIfNeeded(String oldPath, String newPath) {
        if (oldPath != null && !oldPath.isBlank() && !oldPath.equals(newPath)) {
            delete(oldPath);
            log.debug("Deleted old file: {}", oldPath);
        }
    }

    /**
     * Kiểm tra file tồn tại
     */
    public boolean exists(String filename) {
        return Files.exists(rootLocation.resolve(filename));
    }

    /**
     * Validate file
     */
    private void validateFile(MultipartFile file, List<String> allowedTypes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File vượt quá dung lượng cho phép (" + (maxFileSize / 1024 / 1024) + "MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Loại file không được hỗ trợ: " + contentType);
        }
    }

    /**
     * Lấy extension từ filename
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Lấy đường dẫn tuyệt đối của file
     */
    public Path getFilePath(String filename) {
        return rootLocation.resolve(filename);
    }

    /**
     * Lấy URL công khai của file
     */
    public String getPublicUrl(String filename) {
        return "/uploads/" + filename;
    }
}
