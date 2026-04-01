package fit.hutech.BuiBaoHan.dto;

import lombok.Builder;

@Builder
public record FileUploadResponse(
        String fileName,
        String fileUrl,
        String fileType,
        long fileSize,
        String storageMode
) {}
