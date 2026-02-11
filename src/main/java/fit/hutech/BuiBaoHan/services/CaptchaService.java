package fit.hutech.BuiBaoHan.services;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.extern.slf4j.Slf4j;

/**
 * Service xác thực Google reCAPTCHA v2 (Checkbox "I'm not a robot").
 * 
 * Khi user tick checkbox, nếu nghi ngờ sẽ hiện thử thách hình ảnh.
 * Frontend gửi token "g-recaptcha-response" → service verify với Google API.
 */
@Service
@Slf4j
public class CaptchaService {

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    
    /** Google public test site key — luôn pass, không cần gọi API */
    private static final String GOOGLE_TEST_SITE_KEY = "6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI";

    @Value("${google.recaptcha.secret:}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;

    @Value("${google.recaptcha.site-key:}")
    private String siteKey;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Lấy site key để frontend render widget
     */
    public String getSiteKey() {
        return siteKey;
    }

    /**
     * Kiểm tra reCAPTCHA có được bật không
     */
    public boolean isEnabled() {
        return recaptchaEnabled;
    }

    /**
     * Verify reCAPTCHA v2 response token.
     * 
     * @param captchaResponse Token "g-recaptcha-response" từ frontend
     * @return true nếu verification thành công
     */
    public boolean verifyCaptcha(String captchaResponse) {
        if (!recaptchaEnabled) {
            log.debug("reCAPTCHA is disabled, skipping verification");
            return true;
        }

        if (captchaResponse == null || captchaResponse.isBlank()) {
            log.warn("Empty CAPTCHA response — user chưa tick checkbox");
            return false;
        }

        // Nếu đang dùng Google test key → auto-pass, không gọi API
        if (GOOGLE_TEST_SITE_KEY.equals(siteKey)) {
            log.info("reCAPTCHA đang dùng Google TEST KEY → auto-pass (không gọi API verify)");
            return true;
        }

        if (recaptchaSecret == null || recaptchaSecret.isBlank()) {
            log.error("reCAPTCHA secret key not configured");
            return false;
        }

        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", recaptchaSecret);
            params.add("response", captchaResponse);

            RecaptchaResponse response = restClient.post()
                    .uri(URI.create(RECAPTCHA_VERIFY_URL))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(RecaptchaResponse.class);

            if (response == null) {
                log.error("Null response from reCAPTCHA API");
                return false;
            }

            log.debug("reCAPTCHA v2 verification: success={}, hostname={}", 
                    response.success(), response.hostname());

            if (!response.success()) {
                log.warn("reCAPTCHA verification failed: {}", response.errorCodes());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error verifying reCAPTCHA: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Overload: verify với action name (tương thích API cũ, v2 bỏ qua action)
     */
    public boolean verifyCaptcha(String captchaResponse, String action) {
        return verifyCaptcha(captchaResponse);
    }

    /**
     * Overload: verify với action và threshold (tương thích API cũ, v2 bỏ qua score)
     */
    public boolean verifyCaptcha(String captchaResponse, String action, double threshold) {
        return verifyCaptcha(captchaResponse);
    }

    /**
     * Response object from Google reCAPTCHA v2 API
     */
    private record RecaptchaResponse(
            boolean success,
            @JsonProperty("challenge_ts") String challengeTs,
            String hostname,
            @JsonProperty("error-codes") java.util.List<String> errorCodes
    ) {}
}
