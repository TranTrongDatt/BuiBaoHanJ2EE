package fit.hutech.BuiBaoHan.constants;

/**
 * Trạng thái tài khoản người dùng
 */
public enum UserStatus {
    ACTIVE("Đang hoạt động"),
    LOCKED("Đã khóa"),
    PENDING("Chờ xác thực"),
    BANNED("Đã cấm");

    public final String displayName;

    UserStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
