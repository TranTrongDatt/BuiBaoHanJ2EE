package fit.hutech.BuiBaoHan.constants;

/**
 * Loại thông báo
 */
public enum NotificationType {
    ORDER("Đơn hàng"),
    BORROW("Mượn sách"),
    RETURN("Trả sách"),
    OVERDUE("Quá hạn"),
    COMMENT("Bình luận"),
    LIKE("Thích"),
    FOLLOW("Theo dõi"),
    FINE("Phạt"),
    PAYMENT("Thanh toán"),
    PROMOTION("Khuyến mãi"),
    SYSTEM("Hệ thống"),
    CHAT("Tin nhắn");

    public final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
