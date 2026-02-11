package fit.hutech.BuiBaoHan.constants;

/**
 * Tình trạng sách khi mượn/trả
 */
public enum BookCondition {
    NEW("Mới", 0),
    LIKE_NEW("Như mới", 0),
    GOOD("Tốt", 0),
    FAIR("Khá", 0),
    POOR("Kém", 10000),
    DAMAGED("Hư hỏng", 50000),
    LOST("Mất", 100);  // 100% giá sách

    public final String displayName;
    public final int finePenalty;  // Tiền phạt cố định hoặc % giá sách

    BookCondition(String displayName, int finePenalty) {
        this.displayName = displayName;
        this.finePenalty = finePenalty;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getFinePenalty() {
        return finePenalty;
    }
}
