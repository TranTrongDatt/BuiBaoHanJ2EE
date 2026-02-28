package fit.hutech.BuiBaoHan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.security.CustomOAuth2User;
import fit.hutech.BuiBaoHan.security.JwtAuthenticationFilter;
import fit.hutech.BuiBaoHan.security.RateLimitFilter;
import fit.hutech.BuiBaoHan.security.RecaptchaLoginFilter;
import fit.hutech.BuiBaoHan.services.OAuthService;
import fit.hutech.BuiBaoHan.services.UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final OAuthService oAuthService;
    private final IUserRepository userRepository;
    private UserService userService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private RateLimitFilter rateLimitFilter;
    private RecaptchaLoginFilter recaptchaLoginFilter;
    
    @Value("${jwt.cookie.name:MV_ACCESS_TOKEN}")
    private String accessTokenCookieName;
    
    @Value("${jwt.cookie.refresh-name:MV_REFRESH_TOKEN}")
    private String refreshTokenCookieName;
    
    public SecurityConfig(OAuthService oAuthService, IUserRepository userRepository) {
        this.oAuthService = oAuthService;
        this.userRepository = userRepository;
    }
    
    @Autowired
    @Lazy
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    
    @Autowired
    @Lazy
    public void setJwtAuthenticationFilter(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    
    @Autowired
    @Lazy
    public void setRateLimitFilter(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    @Autowired
    @Lazy
    public void setRecaptchaLoginFilter(RecaptchaLoginFilter recaptchaLoginFilter) {
        this.recaptchaLoginFilter = recaptchaLoginFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security filter chain cho Browser API (session-based)
     * Các API được gọi từ browser đã đăng nhập → dùng session auth + CSRF
     * Áp dụng cho: /api/chat/**, /api/wishlist/**
     */
    @Bean
    @Order(0)
    public SecurityFilterChain browserApiFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");
        
        return http
                .securityMatcher("/api/chat/**", "/api/wishlist/**")
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/chat/**", "/api/wishlist/**").authenticated()
                )
                .build();
    }

    /**
     * Security filter chain cho REST API với JWT (stateless)
     * Áp dụng cho các endpoint /api/** (trừ /api/chat/**, /api/wishlist/** đã xử lý ở trên)
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public API endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/captcha/**").permitAll()
                        .requestMatchers("/api/captcha/**").permitAll() // Slider captcha
                        // Debug API - cho phép authenticated users (CHỈ DÙNG CHO DEV)
                        .requestMatchers("/api/debug/**").authenticated()
                        // Admin APIs
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_ADMIN")
                        // API Books: GET cho tất cả authenticated, POST/PUT/DELETE chỉ ADMIN
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/books/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/books/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/books/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/books/**").hasAnyAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                // Add Rate Limit filter first
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Security filter chain cho Web Application (session-based)
     * Áp dụng cho các endpoint còn lại
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF token handler for proper SPA support
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName("_csrf");
        
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler)
                        // Ignore CSRF for API endpoints (handled separately)
                        .ignoringRequestMatchers("/api/**", "/ws/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/uploads/**", "/error", "/403", "/webjars/**").permitAll()
                        .requestMatchers("/register", "/login", "/oauth/**").permitAll()
                        .requestMatchers("/forgot-password", "/reset-password").permitAll()
                        // Debug endpoint - CHỈ DÙNG CHO DEV
                        .requestMatchers("/debug/**").authenticated()
                        // WebSocket endpoints
                        .requestMatchers("/ws/**", "/ws").permitAll()
                        // Chat pages - yêu cầu đăng nhập (web pages only, not API)
                        .requestMatchers("/chat/**").authenticated()
                        // NOTE: /api/chat/** is handled by apiFilterChain with permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/books/edit/**", "/books/add", "/books/delete/**").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/profile/**").authenticated()
                        .requestMatchers("/books/**", "/cart/**", "/categories/**", "/").permitAll()
                        .anyRequest().authenticated()
                )
                // reCAPTCHA v2 filter — verify checkbox trước khi Spring Security xử lý login
                .addFilterBefore(recaptchaLoginFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuthService)
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");
                            
                            if (email == null) {
                                response.sendRedirect("/login?error");
                                return;
                            }
                            
                            // Tạo hoặc lấy user từ DB
                            userService.saveOauthUser(email, name != null ? name : email);
                            
                            // Load user đầy đủ từ DB (với roles)
                            User user = userRepository.findByEmail(email).orElse(null);
                            if (user == null) {
                                response.sendRedirect("/login?error");
                                return;
                            }
                            
                            // Kiểm tra tài khoản bị cấm/khóa
                            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                                // Đăng xuất OAuth session
                                SecurityContextHolder.clearContext();
                                request.getSession().invalidate();
                                response.sendRedirect("/login?locked");
                                return;
                            }
                            
                            // Tạo CustomOAuth2User kết hợp OAuth info + DB roles
                            CustomOAuth2User customUser = new CustomOAuth2User(oAuth2User, user);
                            
                            // Tạo authentication mới với roles từ DB
                            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
                                    customUser,
                                    customUser.getAuthorities(),
                                    ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
                            );
                            
                            // Set vào SecurityContext và PERSIST vào session
                            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                            securityContext.setAuthentication(newAuth);
                            SecurityContextHolder.setContext(securityContext);
                            
                            // Lưu SecurityContext vào HTTP session (bắt buộc với Spring Security mới)
                            SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
                            contextRepository.saveContext(securityContext, request, response);
                            
                            response.sendRedirect("/");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        // Delete session cookie
                        .deleteCookies("JSESSIONID", accessTokenCookieName, refreshTokenCookieName, "remember-me")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("MiniVerseSecureRememberMeKey2024!")
                        .rememberMeCookieName("MV_REMEMBER_ME")
                        .tokenValiditySeconds(30 * 24 * 60 * 60) // 30 days
                        .userDetailsService(userService)
                        .useSecureCookie(true)
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                )
                // Session management
                .sessionManagement(session -> session
                        .maximumSessions(5) // Max 5 sessions per user
                        .maxSessionsPreventsLogin(false) // Allow new login, invalidate old
                )
                .build();
    }
}
