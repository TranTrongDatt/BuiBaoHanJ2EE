package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái phiếu mượn sách
 */
public enum BorrowStatus {
    BORROWING("Đang mượn"),
    RETURNED("Đã trả"),
    OVERDUE("Quá hạn"),
    EXTENDED("Đã gia hạn"),
    LOST("Đã mất");

    public final String displayName;

    BorrowStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
