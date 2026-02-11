package fit.hutech.BuiBaoHan.controllers.websocket;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;

import fit.hutech.BuiBaoHan.constants.NotificationType;
import fit.hutech.BuiBaoHan.entities.Notification;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.services.NotificationService;
import fit.hutech.BuiBaoHan.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket Controller for real-time notifications
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final NotificationService notificationService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Subscribe to notifications
     */
    @MessageMapping("/notifications.subscribe")
    @SendToUser("/queue/notifications-status")
    public SubscriptionResponse subscribe(Principal principal) {
        if (principal == null) {
            return new SubscriptionResponse(false, "Unauthorized", 0);
        }
        
        return userService.findByUsername(principal.getName())
                .map(user -> {
                    long unreadCount = notificationService.countUnread(user.getId());
                    return new SubscriptionResponse(true, "Subscribed successfully", unreadCount);
                })
                .orElse(new SubscriptionResponse(false, "User not found", 0));
    }

    /**
     * Mark notification as read
     */
    @MessageMapping("/notifications.read")
    public void markAsRead(@Payload MarkReadPayload payload, Principal principal) {
        if (principal == null) return;
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            notificationService.markAsRead(payload.notificationId(), user.getId());
            
            // Send updated unread count
            long unreadCount = notificationService.countUnread(user.getId());
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications-count",
                    new UnreadCountUpdate(unreadCount)
            );
        });
    }

    /**
     * Mark all notifications as read
     */
    @MessageMapping("/notifications.readAll")
    public void markAllAsRead(Principal principal) {
        if (principal == null) return;
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            notificationService.markAllAsRead(user.getId());
            
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications-count",
                    new UnreadCountUpdate(0)
            );
        });
    }

    /**
     * Delete notification
     */
    @MessageMapping("/notifications.delete")
    public void deleteNotification(@Payload DeletePayload payload, Principal principal) {
        if (principal == null) return;
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            notificationService.deleteNotification(payload.notificationId(), user.getId());
            
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notification-deleted",
                    new NotificationDeletedEvent(payload.notificationId())
            );
        });
    }

    /**
     * Get recent notifications
     */
    @MessageMapping("/notifications.recent")
    @SendToUser("/queue/notifications-list")
    public NotificationListResponse getRecentNotifications(Principal principal) {
        if (principal == null) {
            return new NotificationListResponse(List.of(), 0);
        }
        
        return userService.findByUsername(principal.getName())
                .map(user -> {
                    List<NotificationDto> notifications = notificationService.getRecentNotifications(user.getId(), 20)
                            .stream()
                            .map(NotificationDto::from)
                            .toList();
                    long unreadCount = notificationService.countUnread(user.getId());
                    return new NotificationListResponse(notifications, unreadCount);
                })
                .orElse(new NotificationListResponse(List.of(), 0));
    }

    // ==================== Public Methods for Sending Notifications ====================

    /**
     * Send notification to specific user
     */
    @Async
    public void sendNotification(User user, Notification notification) {
        try {
            NotificationDto dto = NotificationDto.from(notification);
            
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    dto
            );
            
            // Send updated count
            long unreadCount = notificationService.countUnread(user.getId());
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications-count",
                    new UnreadCountUpdate(unreadCount)
            );
            
        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Send notification to multiple users
     */
    @Async
    public void sendNotificationToUsers(List<User> users, NotificationType type, String title, String message, String link) {
        for (User user : users) {
            try {
                Notification notification = notificationService.createNotification(user.getId(), type, title, message, link);
                sendNotification(user, notification);
            } catch (Exception e) {
                log.error("Error sending notification to user {}: {}", user.getUsername(), e.getMessage());
            }
        }
    }

    /**
     * Broadcast notification to all connected users (admin announcements)
     */
    public void broadcastNotification(String type, String title, String message) {
        BroadcastNotification broadcast = new BroadcastNotification(type, title, message, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/announcements", broadcast);
    }

    /**
     * Send order status update notification
     */
    @Async
    public void sendOrderStatusNotification(User user, Long orderId, String status) {
        String title = "Order Update";
        String message = String.format("Your order #%d status has been updated to: %s", orderId, status);
        String link = "/profile/orders/" + orderId;
        
        Notification notification = notificationService.createNotification(user.getId(), NotificationType.ORDER, title, message, link);
        sendNotification(user, notification);
    }

    /**
     * Send new comment notification
     */
    @Async
    public void sendCommentNotification(User user, Long postId, String commenterName) {
        String title = "New Comment";
        String message = commenterName + " commented on your post";
        String link = "/blog/post/" + postId;
        
        Notification notification = notificationService.createNotification(user.getId(), NotificationType.COMMENT, title, message, link);
        sendNotification(user, notification);
    }

    /**
     * Send follow notification
     */
    @Async
    public void sendFollowNotification(User user, String followerName) {
        String title = "New Follower";
        String message = followerName + " started following you";
        String link = "/profile/" + followerName;
        
        Notification notification = notificationService.createNotification(user.getId(), NotificationType.FOLLOW, title, message, link);
        sendNotification(user, notification);
    }

    // ==================== Payload & Response Records ====================

    public record SubscriptionResponse(
            boolean success,
            String message,
            long unreadCount
    ) {}

    public record MarkReadPayload(Long notificationId) {}

    public record DeletePayload(Long notificationId) {}

    public record UnreadCountUpdate(long count) {}

    public record NotificationDeletedEvent(Long notificationId) {}

    public record NotificationListResponse(
            List<NotificationDto> notifications,
            long unreadCount
    ) {}

    public record NotificationDto(
            Long id,
            String type,
            String title,
            String message,
            String link,
            boolean read,
            LocalDateTime createdAt
    ) {
        public static NotificationDto from(Notification notification) {
            return new NotificationDto(
                    notification.getId(),
                    notification.getType() != null ? notification.getType().name() : null,
                    notification.getTitle(),
                    notification.getContent(),
                    notification.getActionUrl(),
                    notification.isRead(),
                    notification.getCreatedAt()
            );
        }
    }

    public record BroadcastNotification(
            String type,
            String title,
            String message,
            LocalDateTime timestamp
    ) {}
}
