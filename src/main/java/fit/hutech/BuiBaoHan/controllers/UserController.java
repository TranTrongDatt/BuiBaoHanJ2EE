package fit.hutech.BuiBaoHan.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.services.CaptchaService;
import fit.hutech.BuiBaoHan.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final CaptchaService captchaService;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
        model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
        return "user/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
        model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(name = "g-recaptcha-response", required = false) String captchaResponse,
            Model model) {

        log.info("=== REGISTER ATTEMPT: username={}, email={} ===", user.getUsername(), user.getEmail());

        // Kiểm tra lỗi validation TRƯỚC khi verify captcha
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors: {}", bindingResult.getAllErrors());
            model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
            model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
            return "user/register";
        }

        // Kiểm tra duplicate TRƯỚC khi verify captcha (case-insensitive)
        log.info("Checking duplicate username='{}', email='{}'", user.getUsername(), user.getEmail());
        if (userService.existsByUsernameIgnoreCase(user.getUsername())) {
            log.warn("Username already exists (ignore case): {}", user.getUsername());
            model.addAttribute("error", "Tên đăng nhập '" + user.getUsername() + "' đã tồn tại!");
            model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
            model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
            return "user/register";
        }

        if (userService.existsByEmailIgnoreCase(user.getEmail())) {
            log.warn("Email already exists (ignore case): {}", user.getEmail());
            model.addAttribute("error", "Email '" + user.getEmail() + "' đã được sử dụng!");
            model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
            model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
            return "user/register";
        }

        // Verify reCAPTCHA v2
        log.info("Verifying reCAPTCHA... enabled={}, responseLength={}", 
                captchaService.isEnabled(), 
                captchaResponse != null ? captchaResponse.length() : 0);
        
        if (!captchaService.verifyCaptcha(captchaResponse)) {
            log.warn("reCAPTCHA verification failed for registration: {}", user.getUsername());
            model.addAttribute("error", "Vui lòng xác thực CAPTCHA!");
            model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
            model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
            return "user/register";
        }

        try {
            log.info("Saving user: {}", user.getUsername());
            userService.save(user);
            userService.setDefaultRole(user.getUsername());
            log.info("User registered successfully: {}", user.getUsername());
            return "redirect:/login?registered";
        } catch (Exception e) {
            log.error("Registration failed for user: {} - Error: {}", user.getUsername(), e.getMessage(), e);
            model.addAttribute("error", "Đăng ký thất bại: " + e.getMessage());
            model.addAttribute("recaptchaSiteKey", captchaService.getSiteKey());
            model.addAttribute("recaptchaEnabled", captchaService.isEnabled());
            return "user/register";
        }
    }
}
