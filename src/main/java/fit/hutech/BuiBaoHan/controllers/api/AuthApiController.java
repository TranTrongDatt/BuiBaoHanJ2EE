package fit.hutech.BuiBaoHan.controllers.api;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.AuthResponse;
import fit.hutech.BuiBaoHan.dto.ForgotPasswordRequest;
import fit.hutech.BuiBaoHan.dto.LoginRequest;
import fit.hutech.BuiBaoHan.dto.RefreshTokenRequest;
import fit.hutech.BuiBaoHan.dto.RegisterRequest;
import fit.hutech.BuiBaoHan.dto.ResetPasswordRequest;
import fit.hutech.BuiBaoHan.entities.RefreshToken;
import fit.hutech.BuiBaoHan.entities.TokenBlacklist.BlacklistReason;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.exceptions.BadRequestException;
import fit.hutech.BuiBaoHan.exceptions.RateLimitExceededException;
import fit.hutech.BuiBaoHan.exceptions.UnauthorizedException;
import fit.hutech.BuiBaoHan.security.JwtUtil;
import fit.hutech.BuiBaoHan.services.CaptchaService;
import fit.hutech.BuiBaoHan.services.JwtCookieService;
import fit.hutech.BuiBaoHan.services.PasswordResetService;
import fit.hutech.BuiBaoHan.services.RateLimitService;
import fit.hutech.BuiBaoHan.services.RefreshTokenService;
import fit.hutech.BuiBaoHan.services.TokenBlacklistService;
import fit.hutech.BuiBaoHan.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API Controller cho Authentication.
 * 
 * Endpoints:
 * - POST /api/auth/login - Đăng nhập
 * - POST /api/auth/register - Đăng ký
 * - POST /api/auth/refresh - Refresh token
 * - POST /api/auth/logout - Đăng xuất
 * - POST /api/auth/forgot-password - Quên mật khẩu
 * - POST /api/auth/reset-password - Đặt lại mật khẩu
 * - GET /api/auth/me - Lấy thông tin user hiện tại
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtCookieService jwtCookieService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetService passwordResetService;
    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;

    /**
     * Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        
        // Rate limiting
        if (!rateLimitService.checkLoginRateLimit(clientIp)) {
            throw new RateLimitExceededException("Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau.");
        }
        
        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            
            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user, 
                    httpRequest.getHeader("User-Agent"),
                    clientIp
            );
            
            // Create cookies
            ResponseCookie accessCookie = jwtCookieService.createAccessTokenCookie(accessToken);
            ResponseCookie refreshCookie = jwtCookieService.createRefreshTokenCookie(refreshToken.getToken());
            
            // Reset rate limit on successful login
            rateLimitService.resetBucket(clientIp, RateLimitService.RateLimitType.LOGIN);
            
            // Build response
            AuthResponse response = AuthResponse.builder()
                    .type("login")
                    .success(true)
                    .message("Đăng nhập thành công")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .expiresIn(jwtUtil.getExpirationFromToken(accessToken).toInstant().toEpochMilli() - System.currentTimeMillis())
                    .expiresAt(jwtUtil.getExpirationFromToken(accessToken).toInstant())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .roles(user.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .build();
            
            log.info("User {} logged in successfully from IP {}", user.getUsername(), clientIp);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(response);
            
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user {} from IP {}", request.getUsername(), clientIp);
            throw new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }

    /**
     * Đăng ký
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        
        // Rate limiting
        if (!rateLimitService.checkRegisterRateLimit(clientIp)) {
            throw new RateLimitExceededException("Bạn đã thử đăng ký quá nhiều lần. Vui lòng thử lại sau.");
        }
        
        // Verify CAPTCHA if enabled
        if (request.getCaptchaToken() != null) {
            if (!captchaService.verifyCaptcha(request.getCaptchaToken(), "register")) {
                throw new BadRequestException("Xác thực CAPTCHA thất bại. Vui lòng thử lại.");
            }
        }
        
        // Validate password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        
        // Check if username/email exists
        if (userService.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Tên đăng nhập đã tồn tại");
        }
        if (userService.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }
        
        // Create user
        User user = userService.registerUser(request);
        
        log.info("New user registered: {} from IP {}", user.getUsername(), clientIp);
        
        return ResponseEntity.ok(AuthResponse.success("Đăng ký thành công! Vui lòng đăng nhập."));
    }

    /**
     * Refresh access token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        // Lấy refresh token từ body hoặc cookie
        String refreshTokenStr = request != null && request.getRefreshToken() != null 
                ? request.getRefreshToken() 
                : jwtCookieService.getRefreshTokenFromCookies(httpRequest);
        
        if (refreshTokenStr == null || refreshTokenStr.isBlank()) {
            throw new UnauthorizedException("Refresh token không tồn tại");
        }
        
        // Validate refresh token
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));
        
        // Check if token is revoked (potential token reuse attack)
        if (refreshToken.isRevoked()) {
            // Token reuse detected! Revoke all tokens in family
            refreshTokenService.handleTokenReuseAttack(refreshToken);
            throw new UnauthorizedException("Phát hiện sử dụng token bất thường. Vui lòng đăng nhập lại.");
        }
        
        // Check if token is expired
        if (refreshToken.isExpired()) {
            throw new UnauthorizedException("Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }
        
        User user = refreshToken.getUser();
        String clientIp = getClientIp(httpRequest);
        
        // Rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                refreshToken,
                httpRequest.getHeader("User-Agent"),
                clientIp
        );
        
        // Generate new access token
        String newAccessToken = jwtUtil.generateToken(user.getUsername());
        
        // Create new cookies
        ResponseCookie accessCookie = jwtCookieService.createAccessTokenCookie(newAccessToken);
        ResponseCookie refreshCookie = jwtCookieService.createRefreshTokenCookie(newRefreshToken.getToken());
        
        AuthResponse response = AuthResponse.builder()
                .type("refresh")
                .success(true)
                .message("Token đã được làm mới")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .expiresIn(jwtUtil.getExpirationFromToken(newAccessToken).toInstant().toEpochMilli() - System.currentTimeMillis())
                .expiresAt(jwtUtil.getExpirationFromToken(newAccessToken).toInstant())
                .userId(user.getId())
                .username(user.getUsername())
                .build();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(response);
    }

    /**
     * Đăng xuất
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        
        // Get current access token
        String accessToken = jwtCookieService.getAccessToken(request);
        String refreshToken = jwtCookieService.getRefreshTokenFromCookies(request);
        
        // Blacklist access token
        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            try {
                Instant expiry = jwtUtil.getExpirationFromToken(accessToken).toInstant();
                String username = jwtUtil.getUsernameFromToken(accessToken);
                User user = (User) userService.loadUserByUsername(username);
                
                tokenBlacklistService.blacklistAccessToken(
                        accessToken, 
                        expiry, 
                        BlacklistReason.LOGOUT, 
                        user.getId()
                );
                
                // Revoke refresh token
                if (refreshToken != null) {
                    refreshTokenService.findByToken(refreshToken)
                            .ifPresent(rt -> refreshTokenService.revokeToken(rt, "LOGOUT"));
                }
                
                log.info("User {} logged out", username);
            } catch (RuntimeException e) {
                log.warn("Error during logout: {}", e.getMessage());
            }
        }
        
        // Clear cookies
        ResponseCookie accessCookie = jwtCookieService.createAccessTokenDeletionCookie();
        ResponseCookie refreshCookie = jwtCookieService.createRefreshTokenDeletionCookie();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(AuthResponse.success("Đăng xuất thành công"));
    }

    /**
     * Yêu cầu đặt lại mật khẩu
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        
        // Rate limiting
        if (!rateLimitService.checkPasswordResetRateLimit(request.getEmail())) {
            throw new RateLimitExceededException("Bạn đã yêu cầu quá nhiều lần. Vui lòng thử lại sau 1 giờ.");
        }
        
        // Verify CAPTCHA if provided
        if (request.getCaptchaToken() != null) {
            if (!captchaService.verifyCaptcha(request.getCaptchaToken(), "forgot_password", 0.7)) {
                throw new BadRequestException("Xác thực CAPTCHA thất bại");
            }
        }
        
        try {
            passwordResetService.createPasswordResetToken(request.getEmail(), clientIp);
        } catch (Exception e) {
            // Don't reveal if email exists or not (security)
            log.warn("Password reset request for {}: {}", request.getEmail(), e.getMessage());
        }
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(AuthResponse.success(
                "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được link đặt lại mật khẩu."
        ));
    }

    /**
     * Đặt lại mật khẩu mới
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }
        
        String clientIp = getClientIp(httpRequest);
        
        passwordResetService.resetPassword(
                request.getToken(),
                request.getNewPassword(),
                clientIp
        );
        
        return ResponseEntity.ok(AuthResponse.success(
                "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập với mật khẩu mới."
        ));
    }

    /**
     * Lấy thông tin user hiện tại
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() 
                || "anonymousUser".equals(authentication.getName())) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }
        
        User user = (User) authentication.getPrincipal();
        
        AuthResponse response = AuthResponse.builder()
                .type("user_info")
                .success(true)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Validate token
     */
    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(HttpServletRequest request) {
        String accessToken = jwtCookieService.getAccessToken(request);
        
        if (accessToken == null || !jwtUtil.validateToken(accessToken)) {
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(false)
                    .message("Token không hợp lệ hoặc đã hết hạn")
                    .build());
        }
        
        if (tokenBlacklistService.isBlacklisted(accessToken)) {
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(false)
                    .message("Token đã bị thu hồi")
                    .build());
        }
        
        return ResponseEntity.ok(AuthResponse.builder()
                .success(true)
                .message("Token hợp lệ")
                .expiresAt(jwtUtil.getExpirationFromToken(accessToken).toInstant())
                .build());
    }

    /**
     * Helper method để lấy client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}
