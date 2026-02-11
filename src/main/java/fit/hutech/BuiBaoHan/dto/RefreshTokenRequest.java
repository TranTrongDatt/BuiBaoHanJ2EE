package fit.hutech.BuiBaoHan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho refresh token
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    
    /**
     * Refresh token (có thể lấy từ body hoặc cookie)
     * Nếu không có trong body, sẽ lấy từ cookie
     */
    private String refreshToken;
}
