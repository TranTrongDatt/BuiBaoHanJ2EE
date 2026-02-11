package fit.hutech.BuiBaoHan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CommentRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(max = 2000, message = "Nội dung không quá 2000 ký tự")
        String content,
        
        Long blogPostId,
        
        Long bookId,
        
        Long parentCommentId,
        
        Integer rating
) {}
