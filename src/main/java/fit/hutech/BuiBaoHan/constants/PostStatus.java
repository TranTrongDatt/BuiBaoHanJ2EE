package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái bài viết
 */
public enum PostStatus {
    DRAFT("Bản nháp"),
    PUBLISHED("Đã xuất bản"),
    ACTIVE("Hiển thị"),
    HIDDEN("Ẩn"),
    ARCHIVED("Lưu trữ"),
    DELETED("Đã xóa"),
    PENDING("Chờ duyệt"),
    REJECTED("Bị từ chối");

    public final String displayName;

    PostStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
