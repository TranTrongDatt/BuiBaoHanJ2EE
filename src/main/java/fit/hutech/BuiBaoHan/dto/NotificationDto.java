package fit.hutech.BuiBaoHan.dto;

import java.time.LocalDateTime;

import fit.hutech.BuiBaoHan.constants.NotificationType;
import fit.hutech.BuiBaoHan.entities.Notification;
import lombok.Builder;

@Builder
public record NotificationDto(
        Long id,
        String title,
        String content,
        NotificationType type,
        Boolean isRead,
        LocalDateTime readAt,
        String actionUrl,
        String icon,
        LocalDateTime createdAt,
        Long senderId,
        String senderName,
        String senderAvatar
) {
    public static NotificationDto from(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .icon(notification.getIconOrDefault())
                .createdAt(notification.getCreatedAt())
                .senderId(notification.getSender() != null ? notification.getSender().getId() : null)
                .senderName(notification.getSender() != null ? notification.getSender().getUsername() : null)
                .senderAvatar(notification.getSender() != null ? notification.getSender().getAvatar() : null)
                .build();
    }
}
