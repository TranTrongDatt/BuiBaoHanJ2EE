package fit.hutech.BuiBaoHan.constants;

/**
 * Loại phòng chat
 */
public enum RoomType {
    PRIVATE("Riêng tư"),
    GROUP("Nhóm");

    public final String displayName;

    RoomType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
