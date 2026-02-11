package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái sách
 */
public enum BookStatus {
    AVAILABLE("Còn hàng"),
    OUT_OF_STOCK("Hết hàng"),
    DISCONTINUED("Ngừng kinh doanh"),
    COMING_SOON("Sắp ra mắt");

    public final String displayName;

    BookStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
