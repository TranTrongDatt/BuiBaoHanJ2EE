package fit.hutech.BuiBaoHan.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter để thêm Security Headers vào response.
 * 
 * Headers:
 * - X-Content-Type-Options: nosniff (chống MIME sniffing)
 * - X-Frame-Options: DENY (chống clickjacking)
 * - X-XSS-Protection: 1; mode=block (XSS filter)
 * - Strict-Transport-Security: HSTS (force HTTPS)
 * - Content-Security-Policy: CSP
 * - Referrer-Policy: same-origin
 * - Permissions-Policy: restrict features
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.hsts.enabled:true}")
    private boolean hstsEnabled;

    @Value("${security.headers.hsts.max-age:31536000}")
    private long hstsMaxAge;

    @Value("${security.headers.csp:default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; script-src-elem 'self' https://www.google.com https://www.gstatic.com https://cdn.jsdelivr.net https://code.jquery.com https://cdn.datatables.net; style-src 'self' 'unsafe-inline'; style-src-elem 'self' https://cdn.jsdelivr.net https://fonts.googleapis.com https://cdn.datatables.net; img-src 'self' data: https:; font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; media-src 'self' https://res.cloudinary.com; frame-src 'self' https://www.google.com https://res.cloudinary.com; object-src 'self' https://res.cloudinary.com;}")
    private String contentSecurityPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Prevent MIME sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // Prevent clickjacking (SAMEORIGIN cho phép iframe cùng origin, vd: PDF viewer)
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        
        // XSS Protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // HSTS (only over HTTPS)
        if (hstsEnabled && request.isSecure()) {
            response.setHeader("Strict-Transport-Security", 
                    "max-age=" + hstsMaxAge + "; includeSubDomains; preload");
        }
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (formerly Feature Policy)
        response.setHeader("Permissions-Policy", 
                "geolocation=(self), microphone=(), camera=(), payment=(self)");
        
        // Prevent caching of sensitive pages
        if (isSensitivePath(request.getRequestURI())) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Kiểm tra path có chứa thông tin nhạy cảm không
     */
    private boolean isSensitivePath(String path) {
        return path.startsWith("/api/auth/") ||
               path.startsWith("/admin/") ||
               path.startsWith("/profile/") ||
               path.startsWith("/api/v1/users/") ||
               path.contains("/cart") ||
               path.contains("/checkout") ||
               path.contains("/orders");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply to all requests
        return false;
    }
}
