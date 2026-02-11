package fit.hutech.BuiBaoHan.constants;

/**
 * Quyền xem bài viết
 */
public enum Visibility {
    PUBLIC("Công khai"),
    FOLLOWERS("Người theo dõi"),
    PRIVATE("Riêng tư");

    public final String displayName;

    Visibility(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
