package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.NotificationDto;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Notification;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.NotificationService;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Notifications
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiNotificationController {

    private final NotificationService notificationService;
    private final AuthResolver authResolver;

    /**
     * Get my notifications (paginated)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getMyNotifications(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean unreadOnly) {
        User user = authResolver.resolveUser(principal);
        
        Page<Notification> notifications = (unreadOnly != null && unreadOnly)
                ? notificationService.findUnreadByUser(user, pageable)
                : notificationService.findByUser(user, pageable);
        
        List<NotificationDto> dtos = notifications.getContent().stream()
                .map(NotificationDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(notifications, dtos)));
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Get recent notifications (for dropdown)
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getRecentNotifications(
            @AuthenticationPrincipal Object principal,
            @RequestParam(defaultValue = "5") int limit) {
        User user = authResolver.resolveUser(principal);
        
        List<NotificationDto> notifications = notificationService.findRecentByUser(user, limit).stream()
                .map(NotificationDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    /**
     * Mark notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long notificationId) {
        User user = authResolver.resolveUser(principal);
        try {
            Notification notification = notificationService.markAsRead(notificationId, user);
            return ResponseEntity.ok(ApiResponse.success(NotificationDto.from(notification)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int count = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("Marked " + count + " notifications as read", count));
    }

    /**
     * Delete notification
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long notificationId) {
        User user = authResolver.resolveUser(principal);
        try {
            notificationService.delete(notificationId, user);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete all read notifications
     */
    @DeleteMapping("/clear-read")
    public ResponseEntity<ApiResponse<Integer>> clearReadNotifications(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int count = notificationService.deleteAllRead(user);
        return ResponseEntity.ok(ApiResponse.success("Deleted " + count + " notifications", count));
    }

    /**
     * Get notification settings
     */
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> getSettings(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        NotificationSettings settings = notificationService.getSettings(user);
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update notification settings
     */
    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettings>> updateSettings(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) Boolean emailEnabled,
            @RequestParam(required = false) Boolean pushEnabled,
            @RequestParam(required = false) Boolean orderUpdates,
            @RequestParam(required = false) Boolean promotions,
            @RequestParam(required = false) Boolean newFollowers,
            @RequestParam(required = false) Boolean comments,
            @RequestParam(required = false) Boolean likes) {
        User user = authResolver.resolveUser(principal);
        
        NotificationSettings settings = notificationService.updateSettings(
                user, emailEnabled, pushEnabled, orderUpdates, promotions, newFollowers, comments, likes);
        return ResponseEntity.ok(ApiResponse.success("Settings updated", settings));
    }

    // ==================== Inner Records ====================

    public record NotificationSettings(
            boolean emailEnabled,
            boolean pushEnabled,
            boolean orderUpdates,
            boolean promotions,
            boolean newFollowers,
            boolean comments,
            boolean likes
    ) {}
}
