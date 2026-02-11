package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái thẻ thư viện
 */
public enum CardStatus {
    ACTIVE("Đang hoạt động"),
    EXPIRED("Đã hết hạn"),
    BLOCKED("Đã khóa"),
    SUSPENDED("Tạm dừng"),
    CANCELLED("Đã hủy"),
    PENDING("Chờ kích hoạt");

    public final String displayName;

    CardStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
