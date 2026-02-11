package fit.hutech.BuiBaoHan.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import fit.hutech.BuiBaoHan.services.RateLimitService;
import fit.hutech.BuiBaoHan.services.RateLimitService.RateLimitType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter để áp dụng Rate Limiting.
 * 
 * Rate limits:
 * - /api/auth/login: 5 requests/minute/IP
 * - /api/auth/register: 3 requests/minute/IP
 * - /api/auth/forgot-password: 3 requests/hour/email
 * - /api/**": 100 requests/minute/user or IP
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        
        RateLimitType rateLimitType = determineRateLimitType(path, method);
        
        if (rateLimitType != null) {
            String key = getKey(clientIp, rateLimitType);
            
            if (!rateLimitService.tryConsume(key, rateLimitType)) {
                handleRateLimitExceeded(response, request, rateLimitType);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Xác định loại rate limit dựa trên path
     */
    private RateLimitType determineRateLimitType(String path, String method) {
        if (path.equals("/api/auth/login") && "POST".equals(method)) {
            return RateLimitType.LOGIN;
        }
        if (path.equals("/api/auth/register") && "POST".equals(method)) {
            return RateLimitType.REGISTER;
        }
        if (path.equals("/api/auth/forgot-password") && "POST".equals(method)) {
            return RateLimitType.PASSWORD_RESET;
        }
        if (path.startsWith("/api/")) {
            return RateLimitType.API;
        }
        return null;
    }

    /**
     * Lấy key cho rate limiting
     * @param clientIp IP của client
     * @param type Loại rate limit
     * @return Key dùng cho rate limiting
     */
    private String getKey(String clientIp, RateLimitType type) {
        // Cho login/register sử dụng IP
        if (type == RateLimitType.LOGIN || type == RateLimitType.REGISTER) {
            return clientIp;
        }
        
        // Cho password reset sử dụng email từ request body (nếu có)
        // Note: Đọc body ở đây sẽ consume stream, cần wrapper
        // Tạm thời sử dụng IP
        if (type == RateLimitType.PASSWORD_RESET) {
            return clientIp;
        }
        
        // Cho API sử dụng user ID (nếu authenticated) hoặc IP
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        
        return clientIp;
    }

    /**
     * Lấy client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        // Check các header thường dùng bởi proxy/load balancer
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy cái đầu tiên
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Handle khi bị rate limited
     */
    private void handleRateLimitExceeded(HttpServletResponse response, 
                                          HttpServletRequest request,
                                          RateLimitType type) throws IOException {
        log.warn("Rate limit exceeded for {} on {}", getClientIp(request), request.getRequestURI());
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(type.getPeriod().getSeconds()));
        
        String message = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau " + 
                         type.getPeriod().toMinutes() + " phút.";
        
        String jsonResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"Too Many Requests\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                message,
                request.getRequestURI()
        );
        
        response.getWriter().write(jsonResponse);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter cho static resources
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.startsWith("/webjars/");
    }
}
