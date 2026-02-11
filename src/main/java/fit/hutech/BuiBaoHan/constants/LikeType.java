package fit.hutech.BuiBaoHan.constants;

/**
 * Loại reaction/like
 */
public enum LikeType {
    LIKE("Thích"),
    LOVE("Yêu thích"),
    HAHA("Haha"),
    WOW("Wow"),
    SAD("Buồn"),
    ANGRY("Phẫn nộ");

    public final String displayName;

    LikeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
