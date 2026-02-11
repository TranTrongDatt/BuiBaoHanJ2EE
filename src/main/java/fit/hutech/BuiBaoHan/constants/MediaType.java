package fit.hutech.BuiBaoHan.constants;

/**
 * Loại media trong bài viết/tin nhắn
 */
public enum MediaType {
    TEXT("Văn bản"),
    IMAGE("Hình ảnh"),
    VIDEO("Video"),
    AUDIO("Âm thanh"),
    DOCUMENT("Tài liệu"),
    MIXED("Kết hợp");

    public final String displayName;

    MediaType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
