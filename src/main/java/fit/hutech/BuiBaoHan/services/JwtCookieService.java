package fit.hutech.BuiBaoHan.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý JWT trong HttpOnly Cookies.
 * 
 * Security features:
 * - HttpOnly: Không thể access từ JavaScript (XSS protection)
 * - Secure: Chỉ gửi qua HTTPS
 * - SameSite=Strict: Chống CSRF
 * - Path specific: Refresh token chỉ gửi đến /api/auth/refresh
 */
@Service
@Slf4j
public class JwtCookieService {

    @Value("${jwt.cookie.name:MV_ACCESS_TOKEN}")
    private String accessTokenCookieName;

    @Value("${jwt.cookie.refresh-name:MV_REFRESH_TOKEN}")
    private String refreshTokenCookieName;

    @Value("${jwt.cookie.secure:true}")
    private boolean secureCookie;

    @Value("${jwt.cookie.same-site:Strict}")
    private String sameSite;

    @Value("${jwt.cookie.domain:}")
    private String domain;

    @Value("${jwt.expiration:86400000}")
    private long accessTokenExpiration; // 24 hours in ms

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshTokenExpiration; // 7 days in ms

    /**
     * Tạo cookie chứa Access Token
     */
    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(accessTokenCookieName, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .domain(domain.isEmpty() ? null : domain)
                .build();
    }

    /**
     * Tạo cookie chứa Refresh Token
     * Path limited to /api/auth/refresh để giảm exposure
     */
    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(refreshTokenCookieName, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .domain(domain.isEmpty() ? null : domain)
                .build();
    }

    /**
     * Tạo cookie xóa Access Token (logout)
     */
    public ResponseCookie createAccessTokenDeletionCookie() {
        return ResponseCookie.from(accessTokenCookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .domain(domain.isEmpty() ? null : domain)
                .build();
    }

    /**
     * Tạo cookie xóa Refresh Token (logout)
     */
    public ResponseCookie createRefreshTokenDeletionCookie() {
        return ResponseCookie.from(refreshTokenCookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/api/auth/refresh")
                .maxAge(0)
                .domain(domain.isEmpty() ? null : domain)
                .build();
    }

    /**
     * Lấy Access Token từ request cookies
     */
    public String getAccessTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, accessTokenCookieName);
    }

    /**
     * Lấy Refresh Token từ request cookies
     */
    public String getRefreshTokenFromCookies(HttpServletRequest request) {
        return getTokenFromCookies(request, refreshTokenCookieName);
    }

    /**
     * Helper method để lấy token từ cookies
     */
    private String getTokenFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Lấy token từ Authorization header hoặc Cookie
     * Ưu tiên Authorization header (cho API clients)
     */
    public String getAccessToken(HttpServletRequest request) {
        // Thử lấy từ Authorization header trước
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Fallback to cookie
        return getAccessTokenFromCookies(request);
    }

    public String getAccessTokenCookieName() {
        return accessTokenCookieName;
    }

    public String getRefreshTokenCookieName() {
        return refreshTokenCookieName;
    }
}
