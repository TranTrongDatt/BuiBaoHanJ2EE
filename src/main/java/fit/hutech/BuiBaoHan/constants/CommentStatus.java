package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái bình luận
 */
public enum CommentStatus {
    VISIBLE("Hiển thị"),
    HIDDEN("Ẩn"),
    DELETED("Đã xóa"),
    REPORTED("Bị báo cáo"),
    REJECTED("Bị từ chối");

    public final String displayName;

    CommentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
