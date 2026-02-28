package fit.hutech.BuiBaoHan.security;

import java.io.Serial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import fit.hutech.BuiBaoHan.entities.User;

/**
 * Custom OAuth2User kết hợp thông tin từ Google OAuth với roles từ database.
 * 
 * Khi login bằng Google, Spring Security mặc định chỉ lấy thông tin từ Google
 * (email, name, picture) mà KHÔNG có roles từ DB → sec:authorize luôn fail.
 * 
 * Class này wrap cả 2 nguồn:
 * - OAuth2User: thông tin từ Google (attributes)
 * - User entity: roles từ database (authorities)
 * 
 * Implements Serializable để có thể lưu vào session mà không gây lag.
 */
public class CustomOAuth2User implements OAuth2User, UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    // Lưu dữ liệu serializable thay vì objects
    private final Map<String, Object> attributes;
    private final Set<String> roleNames;
    private final String username;
    private final String password;
    private final String email;
    private final String picture;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean accountNonExpired;
    private final boolean credentialsNonExpired;
    private final Long userId;

    public CustomOAuth2User(OAuth2User oauth2User, User user) {
        // Copy attributes to serializable map
        this.attributes = new HashMap<>(oauth2User.getAttributes());
        
        // Extract role names as strings
        this.roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());
        
        // Copy primitive/serializable data
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = oauth2User.getAttribute("email");
        this.picture = oauth2User.getAttribute("picture");
        this.enabled = user.isEnabled();
        this.accountNonLocked = user.isAccountNonLocked();
        this.accountNonExpired = user.isAccountNonExpired();
        this.credentialsNonExpired = user.isCredentialsNonExpired();
        this.userId = user.getId();
    }

    // ========== OAuth2User methods ==========

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return username;
    }

    // ========== UserDetails methods ==========

    /**
     * Trả về authorities từ roleNames (ROLE_ADMIN, ROLE_USER, etc.)
     * Đây là key để sec:authorize="hasAuthority('ROLE_ADMIN')" hoạt động
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleNames.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ========== Utility methods ==========

    /**
     * Lấy email từ Google OAuth attributes
     */
    public String getEmail() {
        return email;
    }

    /**
     * Lấy avatar từ Google OAuth attributes  
     */
    public String getPicture() {
        return picture;
    }

    /**
     * Lấy User ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Kiểm tra user có quyền Admin không
     */
    public boolean isAdmin() {
        return roleNames.contains("ROLE_ADMIN") || roleNames.contains("ADMIN");
    }
}
