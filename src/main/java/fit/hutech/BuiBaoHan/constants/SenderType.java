package fit.hutech.BuiBaoHan.constants;

/**
 * Loại người gửi tin nhắn (AI Chatbot)
 */
public enum SenderType {
    USER("Người dùng"),
    AI("Trợ lý AI"),
    SYSTEM("Hệ thống");

    public final String displayName;

    SenderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
