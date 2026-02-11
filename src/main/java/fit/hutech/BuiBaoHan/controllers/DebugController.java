package fit.hutech.BuiBaoHan.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Debug controller - CHỈ DÙNG CHO DEVELOPMENT
 * Giúp debug các vấn đề về authentication và authorization
 */
@RestController
@RequiredArgsConstructor
public class DebugController {

    private final IUserRepository userRepository;

    /**
     * Hiển thị thông tin authentication hiện tại
     * Truy cập: GET /debug/auth
     */
    @GetMapping("/debug/auth")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Map<String, Object> result = new HashMap<>();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null) {
            result.put("status", "NO_AUTHENTICATION");
            result.put("message", "Chưa đăng nhập");
            return ResponseEntity.ok(result);
        }
        
        result.put("authenticated", auth.isAuthenticated());
        result.put("principal", auth.getPrincipal().toString());
        result.put("principalClass", auth.getPrincipal().getClass().getName());
        
        // Lấy authorities từ SecurityContext
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        result.put("authorities", authorities);
        
        // Nếu principal là User, lấy thêm thông tin từ User entity
        if (auth.getPrincipal() instanceof User user) {
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            
            // Lấy roles trực tiếp từ User entity
            List<String> rolesFromEntity = user.getRoles().stream()
                    .map(role -> role.getName())
                    .collect(Collectors.toList());
            result.put("rolesFromEntity", rolesFromEntity);
            
            // Lấy authorities từ User.getAuthorities()
            List<String> authoritiesFromUser = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            result.put("authoritiesFromUser", authoritiesFromUser);
        }
        
        // Kiểm tra xem có ROLE_ADMIN không
        boolean hasRoleAdmin = authorities.contains("ROLE_ADMIN");
        boolean hasAdmin = authorities.contains("ADMIN");
        result.put("hasROLE_ADMIN", hasRoleAdmin);
        result.put("hasADMIN", hasAdmin);
        result.put("shouldShowAdminButton", hasRoleAdmin || hasAdmin);
        
        return ResponseEntity.ok(result);
    }

    /**
     * Hiển thị tất cả roles trong database
     * Truy cập: GET /debug/roles
     */
    @GetMapping("/debug/roles")
    public ResponseEntity<Map<String, Object>> getAllRoles() {
        Map<String, Object> result = new HashMap<>();
        
        // Lấy tất cả users và roles của họ
        List<Map<String, Object>> usersWithRoles = userRepository.findAll().stream()
                .map(user -> {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("username", user.getUsername());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("roles", user.getRoles().stream()
                            .map(role -> Map.of(
                                    "id", role.getId(),
                                    "name", role.getName(),
                                    "authority", role.getAuthority()
                            ))
                            .collect(Collectors.toList()));
                    return userInfo;
                })
                .collect(Collectors.toList());
        
        result.put("users", usersWithRoles);
        
        return ResponseEntity.ok(result);
    }
}
