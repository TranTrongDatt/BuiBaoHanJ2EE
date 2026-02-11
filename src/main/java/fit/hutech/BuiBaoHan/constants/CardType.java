package fit.hutech.BuiBaoHan.constants;

/**
 * Loại thẻ thư viện
 */
public enum CardType {
    STANDARD("Thẻ thường", 5, 14),      // Mượn tối đa 5 sách, 14 ngày
    STUDENT("Thẻ sinh viên", 5, 14),    // Mượn tối đa 5 sách, 14 ngày
    TEACHER("Thẻ giáo viên", 10, 30),   // Mượn tối đa 10 sách, 30 ngày
    VIP("Thẻ VIP", 10, 30),             // Mượn tối đa 10 sách, 30 ngày
    TEMPORARY("Thẻ tạm thời", 2, 7);    // Mượn tối đa 2 sách, 7 ngày

    public final String displayName;
    public final int maxBooks;
    public final int maxDays;

    CardType(String displayName, int maxBooks, int maxDays) {
        this.displayName = displayName;
        this.maxBooks = maxBooks;
        this.maxDays = maxDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxBooks() {
        return maxBooks;
    }

    public int getMaxDays() {
        return maxDays;
    }
}
