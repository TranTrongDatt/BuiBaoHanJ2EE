package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái phí phạt
 */
public enum FineStatus {
    PENDING("Chờ thanh toán"),
    PARTIAL("Thanh toán một phần"),
    PAID("Đã thanh toán"),
    WAIVED("Đã miễn"),
    OVERDUE("Quá hạn");

    public final String displayName;

    FineStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
