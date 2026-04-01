package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Các vai trò trong hệ thống
 */
@Getter
@AllArgsConstructor
public enum Role {
    ADMIN(1, "Quản trị viên"),
    USER(2, "Người dùng"),
    SHIPPER(3, "Nhân viên giao hàng");

    private final long value;
    private final String displayName;
}
