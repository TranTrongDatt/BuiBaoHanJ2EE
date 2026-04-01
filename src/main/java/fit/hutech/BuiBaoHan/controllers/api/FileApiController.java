package fit.hutech.BuiBaoHan.controllers.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.FileUploadResponse;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API Controller cho File Upload/Download.
 * Demo cho đề tài: "Spring Boot File Download and Upload REST API"
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileApiController {

    private final FileStorageService fileStorageService;

    /**
     * Upload single file
     * @param file MultipartFile
     * @param type Loại file: image, video, pdf (default: image)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File không được để trống"));
        }

        String fileUrl = switch (type) {
            case "video" -> fileStorageService.storeVideo(file);
            case "pdf" -> fileStorageService.storeDescriptionPdf(file);
            default -> fileStorageService.storeImage(file, "uploads");
        };

        FileUploadResponse response = FileUploadResponse.builder()
                .fileName(file.getOriginalFilename())
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storageMode(fileStorageService.isCloudinaryAvailable() ? "cloudinary" : "local")
                .build();

        log.info("File uploaded: {} ({} bytes, type: {})", file.getOriginalFilename(), file.getSize(), type);
        return ResponseEntity.ok(ApiResponse.success("Upload thành công", response));
    }

    /**
     * Upload multiple files
     * @param files Danh sách MultipartFile
     * @param type Loại file: image, video, pdf
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "type", defaultValue = "image") String type) throws IOException {

        if (files.length == 0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Danh sách file không được trống"));
        }

        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            String resolvedType = type != null ? type : "image";
            String fileUrl = switch (resolvedType) {
                case "video" -> fileStorageService.storeVideo(file);
                case "pdf" -> fileStorageService.storeDescriptionPdf(file);
                default -> fileStorageService.storeImage(file, "uploads");
            };

            responses.add(FileUploadResponse.builder()
                    .fileName(fileName)
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .storageMode(fileStorageService.isCloudinaryAvailable() ? "cloudinary" : "local")
                    .build());
        }

        log.info("Uploaded {} files (type: {})", responses.size(), type);
        return ResponseEntity.ok(ApiResponse.success("Upload " + responses.size() + " file thành công", responses));
    }
}
