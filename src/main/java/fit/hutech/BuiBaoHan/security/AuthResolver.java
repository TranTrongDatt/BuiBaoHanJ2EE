package fit.hutech.BuiBaoHan.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Centralized authentication principal resolver.
 * Handles both form login (User entity) and OAuth2 login (CustomOAuth2User).
 *
 * Usage in controller:
 *   @AuthenticationPrincipal Object principal
 *   Long userId = authResolver.resolveUserId(principal);
 *   User user   = authResolver.resolveUser(principal);
 */
@Component
@RequiredArgsConstructor
public class AuthResolver {

    private final IUserRepository userRepository;

    /**
     * Extract userId from principal (works for both User and CustomOAuth2User).
     * @throws AccessDeniedException if principal type is unknown
     */
    public Long resolveUserId(Object principal) {
        if (principal instanceof User user) return user.getId();
        if (principal instanceof CustomOAuth2User oauth) return oauth.getUserId();
        throw new AccessDeniedException("Không xác định được người dùng. Vui lòng đăng nhập lại.");
    }

    /**
     * Extract userId or return null (for nullable principals / anonymous users).
     */
    public Long resolveUserIdOrNull(Object principal) {
        if (principal instanceof User user) return user.getId();
        if (principal instanceof CustomOAuth2User oauth) return oauth.getUserId();
        return null;
    }

    /**
     * Resolve full User entity. For OAuth2 login, loads from DB.
     * @throws AccessDeniedException if principal is unknown or user not found.
     */
    public User resolveUser(Object principal) {
        if (principal instanceof User user) return user;
        if (principal instanceof CustomOAuth2User oauth) {
            return userRepository.findById(oauth.getUserId())
                    .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản người dùng."));
        }
        throw new AccessDeniedException("Không xác định được người dùng. Vui lòng đăng nhập lại.");
    }

    /**
     * Resolve full User entity or return null (for nullable / anonymous context).
     */
    public User resolveUserOrNull(Object principal) {
        if (principal instanceof User user) return user;
        if (principal instanceof CustomOAuth2User oauth) {
            return userRepository.findById(oauth.getUserId()).orElse(null);
        }
        return null;
    }

    /**
     * Check if the principal represents an authenticated user.
     */
    public boolean isAuthenticated(Object principal) {
        return principal instanceof User || principal instanceof CustomOAuth2User;
    }
}
