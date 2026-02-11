package fit.hutech.BuiBaoHan.constants;

public enum ShippingStatus {
    PENDING("Chờ lấy hàng"),
    PICKED_UP("Đã lấy hàng"),
    IN_TRANSIT("Đang vận chuyển"),
    OUT_FOR_DELIVERY("Đang giao hàng"),
    DELIVERED("Đã giao hàng"),
    FAILED("Giao hàng thất bại"),
    RETURNED("Đã hoàn trả");

    public final String displayName;

    ShippingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
