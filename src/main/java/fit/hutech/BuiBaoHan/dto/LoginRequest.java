package fit.hutech.BuiBaoHan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request đăng nhập
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3-50 ký tự")
    private String username;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
    
    /**
     * Remember me option
     */
    private boolean rememberMe;
    
    /**
     * CAPTCHA token (optional, cho brute-force protection)
     */
    private String captchaToken;
}
