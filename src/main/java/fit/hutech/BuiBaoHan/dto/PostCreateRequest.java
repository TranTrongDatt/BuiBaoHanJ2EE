package fit.hutech.BuiBaoHan.dto;

import fit.hutech.BuiBaoHan.constants.MediaType;
import fit.hutech.BuiBaoHan.constants.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PostCreateRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
        String title,
        
        @Size(max = 500, message = "Tóm tắt không quá 500 ký tự")
        String summary,
        
        @NotBlank(message = "Nội dung không được để trống")
        String content,
        
        String coverImage,
        
        MediaType mediaType,
        
        Visibility visibility,
        
        Boolean allowComments,
        
        Long bookId
) {
    public PostCreateRequest {
        if (mediaType == null) {
            mediaType = MediaType.TEXT;
        }
        if (visibility == null) {
            visibility = Visibility.PUBLIC;
        }
        if (allowComments == null) {
            allowComments = true;
        }
    }
}
