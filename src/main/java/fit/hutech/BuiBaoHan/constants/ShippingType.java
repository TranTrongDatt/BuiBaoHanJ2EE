package fit.hutech.BuiBaoHan.constants;

import java.math.BigDecimal;

/**
 * Loại hình vận chuyển
 */
public enum ShippingType {
    EXPRESS("Giao hỏa tốc", new BigDecimal("50000"), 1),
    STANDARD("Giao tiêu chuẩn", new BigDecimal("30000"), 3),
    ECONOMY("Giao tiết kiệm", new BigDecimal("15000"), 7),
    SAME_DAY("Giao trong ngày", new BigDecimal("80000"), 0),
    PICKUP("Nhận tại cửa hàng", BigDecimal.ZERO, 0);

    public final String displayName;
    public final BigDecimal fee;
    public final int estimatedDays;

    ShippingType(String displayName, BigDecimal fee, int estimatedDays) {
        this.displayName = displayName;
        this.fee = fee;
        this.estimatedDays = estimatedDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }
}
