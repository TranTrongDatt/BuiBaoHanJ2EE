package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.NotificationType;
import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.dto.NotificationDto;
import fit.hutech.BuiBaoHan.entities.BlogPost;
import fit.hutech.BuiBaoHan.entities.Comment;
import fit.hutech.BuiBaoHan.entities.Notification;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.INotificationRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final INotificationRepository notificationRepository;
    private final IUserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Lấy notifications của user
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Lấy notifications chưa đọc
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Đếm notifications chưa đọc
     */
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Tìm notification theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * Tạo notification
     */
    public Notification createNotification(Long userId, NotificationType type, 
            String title, String content, String actionUrl) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .actionUrl(actionUrl)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Gửi real-time qua WebSocket
        sendRealTimeNotification(userId, saved);

        log.info("Created notification for user {}: {}", userId, title);
        return saved;
    }

    /**
     * Đánh dấu đã đọc
     */
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy notification ID: " + notificationId));

        // Kiểm tra quyền
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền truy cập notification này");
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Xóa notification
     */
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy notification ID: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa notification này");
        }

        notificationRepository.delete(notification);
    }

    /**
     * Xóa notifications đã đọc (cleanup)
     */
    public void deleteReadNotifications(Long userId) {
        notificationRepository.deleteByUserIdAndIsReadTrue(userId);
        log.info("Deleted read notifications for user {}", userId);
    }

    /**
     * Xóa notifications cũ (>30 ngày)
     */
    public void deleteOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteByCreatedAtBefore(threshold);
        log.info("Deleted notifications older than 30 days");
    }
    
    /**
     * Lấy notifications gần đây
     */
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findRecentByUserId(userId, pageable);
    }

    // ==================== Notification Senders ====================

    /**
     * Gửi thông báo đơn hàng
     */
    public void sendOrderNotification(Long userId, Order order) {
        createNotification(userId, NotificationType.ORDER,
                "Đơn hàng mới",
                "Đơn hàng " + order.getOrderCode() + " đã được tạo thành công",
                "/profile/orders/" + order.getId());
    }

    /**
     * Gửi thông báo trạng thái đơn hàng
     */
    public void sendOrderStatusNotification(Long userId, Order order, OrderStatus oldStatus) {
        String statusText = switch (order.getStatus()) {
            case CONFIRMED -> "đã được xác nhận";
            case SHIPPING -> "đang được giao";
            case DELIVERED -> "đã giao thành công";
            case CANCELLED -> "đã bị hủy";
            default -> "đã được cập nhật";
        };

        createNotification(userId, NotificationType.ORDER,
                "Cập nhật đơn hàng",
                "Đơn hàng " + order.getOrderCode() + " " + statusText,
                "/profile/orders/" + order.getId());
    }

    /**
     * Gửi thông báo hủy đơn
     */
    public void sendOrderCancelledNotification(Long userId, Order order) {
        createNotification(userId, NotificationType.ORDER,
                "Đơn hàng bị hủy",
                "Đơn hàng " + order.getOrderCode() + " đã bị hủy. Lý do: " + order.getCancelReason(),
                "/profile/orders/" + order.getId());
    }

    /**
     * Gửi thông báo comment mới
     */
    public void sendCommentNotification(Long userId, User commenter, BlogPost post, String content) {
        String excerpt = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        
        createNotification(userId, NotificationType.COMMENT,
                "Comment mới",
                commenter.getFullName() + " đã comment: \"" + excerpt + "\"",
                "/blog/" + post.getSlug());
    }

    /**
     * Gửi thông báo reply
     */
    public void sendReplyNotification(Long userId, User replier, Comment parent, String content) {
        String excerpt = content.length() > 50 ? content.substring(0, 50) + "..." : content;
        
        createNotification(userId, NotificationType.COMMENT,
                "Reply mới",
                replier.getFullName() + " đã reply: \"" + excerpt + "\"",
                "/blog/" + parent.getBlogPost().getSlug() + "#comment-" + parent.getId());
    }

    /**
     * Gửi thông báo like
     */
    public void sendLikeNotification(Long userId, User liker, BlogPost post) {
        createNotification(userId, NotificationType.LIKE,
                "Like mới",
                liker.getFullName() + " đã thích bài viết của bạn",
                "/blog/" + post.getSlug());
    }

    /**
     * Gửi thông báo follow
     */
    public void sendFollowNotification(Long userId, User follower) {
        createNotification(userId, NotificationType.FOLLOW,
                "Follower mới",
                follower.getFullName() + " đã bắt đầu theo dõi bạn",
                "/user/" + follower.getUsername());
    }

    /**
     * Gửi thông báo hệ thống
     */
    public void sendSystemNotification(Long userId, String title, String content) {
        createNotification(userId, NotificationType.SYSTEM, title, content, null);
    }

    /**
     * Gửi thông báo đến tất cả users (broadcast)
     */
    public void broadcastNotification(NotificationType type, String title, String content, String actionUrl) {
        List<User> allUsers = userRepository.findAll();
        
        for (User user : allUsers) {
            createNotification(user.getId(), type, title, content, actionUrl);
        }
        
        log.info("Broadcasted notification to {} users: {}", allUsers.size(), title);
    }

    // ==================== Real-time WebSocket ====================

    private void sendRealTimeNotification(Long userId, Notification notification) {
        try {
            NotificationDto dto = NotificationDto.from(notification);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    dto
            );
        } catch (MessagingException e) {
            log.warn("Failed to send real-time notification: {}", e.getMessage());
        }
    }

    // ==================== API Controller Support Methods ====================

    /**
     * Lấy notifications của user (wrapper for User object)
     */
    @Transactional(readOnly = true)
    public Page<Notification> findByUser(User user, Pageable pageable) {
        return getNotifications(user.getId(), pageable);
    }

    /**
     * Lấy notifications chưa đọc của user (wrapper for User object)
     */
    @Transactional(readOnly = true)
    public Page<Notification> findUnreadByUser(User user, Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.getId(), pageable);
    }

    /**
     * Đếm notifications chưa đọc (wrapper for User object)
     */
    @Transactional(readOnly = true)
    public int getUnreadCount(User user) {
        return (int) countUnread(user.getId());
    }

    /**
     * Lấy notifications gần đây (wrapper for User object)
     */
    @Transactional(readOnly = true)
    public List<Notification> findRecentByUser(User user, int limit) {
        return getRecentNotifications(user.getId(), limit);
    }

    /**
     * Đánh dấu đã đọc (wrapper for User object)
     */
    public Notification markAsRead(Long notificationId, User user) {
        return markAsRead(notificationId, user.getId());
    }

    /**
     * Đánh dấu tất cả đã đọc (wrapper for User object, returns count)
     */
    public int markAllAsRead(User user) {
        List<Notification> unread = getUnreadNotifications(user.getId());
        int count = unread.size();
        markAllAsRead(user.getId());
        return count;
    }

    /**
     * Xóa notification (wrapper for User object)
     */
    public void delete(Long notificationId, User user) {
        deleteNotification(notificationId, user.getId());
    }

    /**
     * Xóa tất cả notifications đã đọc (wrapper for User object, returns count)
     */
    public int deleteAllRead(User user) {
        List<Notification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), Pageable.unpaged()).getContent();
        int readCount = (int) all.stream().filter(Notification::getIsRead).count();
        deleteReadNotifications(user.getId());
        return readCount;
    }

    /**
     * Lấy notification settings của user
     */
    @Transactional(readOnly = true)
    public fit.hutech.BuiBaoHan.controllers.api.ApiNotificationController.NotificationSettings getSettings(User user) {
        // TODO: Implement actual settings storage
        return new fit.hutech.BuiBaoHan.controllers.api.ApiNotificationController.NotificationSettings(
                true, true, true, true, true, true, true);
    }

    /**
     * Cập nhật notification settings của user
     */
    public fit.hutech.BuiBaoHan.controllers.api.ApiNotificationController.NotificationSettings updateSettings(
            User user, Boolean emailEnabled, Boolean pushEnabled, Boolean orderUpdates,
            Boolean promotions, Boolean newFollowers, Boolean comments, Boolean likes) {
        // TODO: Implement actual settings storage
        return new fit.hutech.BuiBaoHan.controllers.api.ApiNotificationController.NotificationSettings(
                emailEnabled != null ? emailEnabled : true,
                pushEnabled != null ? pushEnabled : true,
                orderUpdates != null ? orderUpdates : true,
                promotions != null ? promotions : true,
                newFollowers != null ? newFollowers : true,
                comments != null ? comments : true,
                likes != null ? likes : true);
    }

    // ==================== Bank Transfer / VietQR Notifications ====================

    /**
     * Thông báo cho tất cả admin khi có đơn hàng chuyển khoản mới
     */
    public void notifyAdminsNewBankTransferOrder(Order order) {
        List<User> admins = userRepository.findAdminUsers();
        String title = "Đơn chuyển khoản mới";
        String content = "Đơn hàng " + order.getOrderCode() + " (" 
                + order.getTotalAmount().toBigInteger() + "₫) đang chờ xác nhận thanh toán chuyển khoản.";
        String actionUrl = "/admin/orders/" + order.getId();

        for (User admin : admins) {
            try {
                createNotification(admin.getId(), NotificationType.PAYMENT, title, content, actionUrl);
            } catch (Exception e) {
                log.warn("Failed to notify admin {} about bank transfer order: {}", admin.getId(), e.getMessage());
            }
        }
        log.info("Notified {} admins about bank transfer order {}", admins.size(), order.getOrderCode());
    }

    /**
     * Thông báo cho user khi admin xác nhận thanh toán
     */
    public void sendPaymentConfirmedNotification(Long userId, Order order) {
        createNotification(userId, NotificationType.PAYMENT,
                "Thanh toán đã xác nhận",
                "Đơn hàng " + order.getOrderCode() + " đã được xác nhận thanh toán thành công!",
                "/profile/orders/" + order.getId());
    }

    /**
     * Thông báo cho user khi đơn hàng bị hủy do quá hạn thanh toán
     */
    public void sendOrderExpiredNotification(Long userId, Order order) {
        createNotification(userId, NotificationType.ORDER,
                "Đơn hàng đã hủy",
                "Đơn hàng " + order.getOrderCode() + " đã bị hủy do quá hạn thanh toán (30 phút).",
                "/profile/orders/" + order.getId());
    }
}
