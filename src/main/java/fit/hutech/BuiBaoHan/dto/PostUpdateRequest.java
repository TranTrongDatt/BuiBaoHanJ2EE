package fit.hutech.BuiBaoHan.dto;

import fit.hutech.BuiBaoHan.constants.MediaType;
import fit.hutech.BuiBaoHan.constants.Visibility;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record PostUpdateRequest(
        @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
        String title,
        
        @Size(max = 500, message = "Tóm tắt không quá 500 ký tự")
        String summary,
        
        String content,
        
        String coverImage,
        
        MediaType mediaType,
        
        Visibility visibility,
        
        Boolean allowComments,
        
        Boolean isPinned,
        
        Long bookId
) {}
