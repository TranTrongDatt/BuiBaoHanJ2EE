package fit.hutech.BuiBaoHan.constants;

/**
 * Loại phiếu phạt
 */
public enum FineType {
    LATE_RETURN("Trả trễ"),
    DAMAGED("Hư hỏng sách"),
    LOST("Mất sách"),
    OTHER("Khác");

    private final String displayName;

    FineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
