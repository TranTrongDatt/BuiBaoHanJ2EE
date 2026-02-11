package fit.hutech.BuiBaoHan.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import fit.hutech.BuiBaoHan.services.JwtCookieService;
import fit.hutech.BuiBaoHan.services.TokenBlacklistService;
import fit.hutech.BuiBaoHan.services.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter để xác thực JWT token cho mỗi request.
 * 
 * Hỗ trợ:
 * - Authorization header: Bearer <token>
 * - HttpOnly Cookie: MV_ACCESS_TOKEN
 * - Token blacklist validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final JwtCookieService jwtCookieService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Lấy JWT từ header hoặc cookie
            String jwt = jwtCookieService.getAccessToken(request);

            if (StringUtils.hasText(jwt)) {
                // Kiểm tra token có trong blacklist không
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.debug("Token is blacklisted");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Validate token
                if (jwtUtil.validateToken(jwt)) {
                    String username = jwtUtil.getUsernameFromToken(jwt);

                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | UsernameNotFoundException ex) {
            log.error("Could not set user authentication in security context: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip filter cho các public endpoints
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.equals("/login") ||
               path.equals("/register") ||
               path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register");
    }
}
