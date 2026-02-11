package fit.hutech.BuiBaoHan.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.CustomOAuth2User;
import fit.hutech.BuiBaoHan.services.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Global ControllerAdvice to add common model attributes to all views.
 * This is needed because #httpServletRequest was removed in Thymeleaf 3.1+ 
 * for security reasons (CVE-2023-38286).
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final NotificationService notificationService;

    /**
     * Add current URI to all views for navigation highlighting
     */
    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    /**
     * Add current full URL (with query string) to all views
     */
    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null) {
            return request.getRequestURI() + "?" + queryString;
        }
        return request.getRequestURI();
    }

    /**
     * Add notification count for the bell badge in header
     */
    @ModelAttribute("notificationCount")
    public Long notificationCount() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                if (auth.getPrincipal() instanceof User user) {
                    return notificationService.countUnread(user.getId());
                } else if (auth.getPrincipal() instanceof CustomOAuth2User oauthUser) {
                    return notificationService.countUnread(oauthUser.getUserId());
                }
            }
        } catch (Exception e) {
            // Silently ignore - return 0 if any error
        }
        return 0L;
    }
}
