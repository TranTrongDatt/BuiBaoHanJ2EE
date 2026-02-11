package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái thanh toán
 */
public enum PaymentStatus {
    PENDING("Chờ thanh toán"),
    PAID("Đã thanh toán"),
    FAILED("Thanh toán thất bại"),
    EXPIRED("Hết hạn"),
    REFUNDED("Đã hoàn tiền"),
    CANCELLED("Đã hủy");

    public final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
