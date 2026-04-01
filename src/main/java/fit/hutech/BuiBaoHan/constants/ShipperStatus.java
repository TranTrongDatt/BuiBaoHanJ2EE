package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trạng thái hoạt động của Shipper
 */
@Getter
@AllArgsConstructor
public enum ShipperStatus {
    ONLINE("Đang hoạt động"),
    OFFLINE("Ngoại tuyến"),
    BUSY("Đang giao hàng"),
    ON_BREAK("Đang nghỉ");

    private final String displayName;
}
