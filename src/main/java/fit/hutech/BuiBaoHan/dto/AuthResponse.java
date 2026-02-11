package fit.hutech.BuiBaoHan.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO cho authentication (login, refresh)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * Loại response
     */
    private String type;
    
    /**
     * Access token (nếu không dùng cookie)
     */
    private String accessToken;
    
    /**
     * Refresh token (nếu không dùng cookie)
     */
    private String refreshToken;
    
    /**
     * Loại token
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * Thời gian access token hết hạn (seconds)
     */
    private long expiresIn;
    
    /**
     * Thời điểm hết hạn
     */
    private Instant expiresAt;
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Username
     */
    private String username;
    
    /**
     * Email
     */
    private String email;
    
    /**
     * Danh sách roles
     */
    private List<String> roles;
    
    /**
     * Message
     */
    private String message;
    
    /**
     * Success status
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Factory method cho success response
     */
    public static AuthResponse success(String message) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .type("success")
                .build();
    }

    /**
     * Factory method cho error response
     */
    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .type("error")
                .build();
    }
}
