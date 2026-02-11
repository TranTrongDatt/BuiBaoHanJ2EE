package fit.hutech.BuiBaoHan.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import fit.hutech.BuiBaoHan.services.CaptchaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter chặn request POST /login để verify reCAPTCHA v2 trước khi
 * Spring Security xử lý authentication.
 * 
 * Nếu captcha không hợp lệ → redirect /login?captchaError
 * Nếu captcha OK hoặc disabled → cho đi tiếp authentication bình thường
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecaptchaLoginFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Chỉ apply cho POST /login (form login)
        if ("POST".equalsIgnoreCase(request.getMethod()) 
                && "/login".equals(request.getServletPath())) {

            String captchaResponse = request.getParameter("g-recaptcha-response");
            
            if (!captchaService.verifyCaptcha(captchaResponse)) {
                log.warn("reCAPTCHA verification failed for login attempt from IP: {}", 
                        request.getRemoteAddr());
                response.sendRedirect("/login?captchaError");
                return;
            }

            log.debug("reCAPTCHA verification passed for login from IP: {}", 
                    request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }
}
