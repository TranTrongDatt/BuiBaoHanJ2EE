package fit.hutech.BuiBaoHan.controllers.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.services.SliderCaptchaService;
import fit.hutech.BuiBaoHan.services.SliderCaptchaService.SliderCaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller cho Slider Captcha.
 * 
 * Endpoints:
 * - GET  /api/captcha/slider     → Lấy captcha mới (background + puzzle + token)
 * - POST /api/captcha/slider/verify → Verify vị trí slider
 */
@RestController
@RequestMapping("/api/captcha/slider")
@RequiredArgsConstructor
@Slf4j
public class SliderCaptchaController {

    private final SliderCaptchaService sliderCaptchaService;

    /**
     * Tạo slider captcha mới
     * @return SliderCaptchaResponse với background, puzzle (base64), và token
     */
    @GetMapping
    public ApiResponse<SliderCaptchaResponse> getCaptcha() {
        try {
            SliderCaptchaResponse captcha = sliderCaptchaService.generateCaptcha();
            log.debug("Generated new slider captcha: token={}", captcha.token());
            return ApiResponse.success("Captcha generated", captcha);
        } catch (Exception e) {
            log.error("Failed to generate captcha: {}", e.getMessage(), e);
            return ApiResponse.error("Failed to generate captcha: " + e.getMessage());
        }
    }

    /**
     * Verify slider captcha
     * @param token Token từ getCaptcha
     * @param positionX Vị trí X mà user kéo đến (pixels)
     * @return success/failure
     */
    @PostMapping("/verify")
    public ApiResponse<Boolean> verifyCaptcha(@RequestBody Map<String, Object> body) {
        try {
            String token = (String) body.get("token");
            int positionX = ((Number) body.get("positionX")).intValue();
            boolean success = sliderCaptchaService.verifyCaptcha(token, positionX);
            
            if (success) {
                log.debug("Slider captcha verified successfully: token={}", token);
                return ApiResponse.success("Xác thực thành công!", true);
            } else {
                log.debug("Slider captcha verification failed: token={}, posX={}", token, positionX);
                return ApiResponse.error("Xác thực thất bại. Vui lòng thử lại.");
            }
        } catch (Exception e) {
            log.error("Captcha verification error: {}", e.getMessage(), e);
            return ApiResponse.error("Lỗi xác thực: " + e.getMessage());
        }
    }
}
