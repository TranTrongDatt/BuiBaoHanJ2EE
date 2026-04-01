package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Loại phương tiện của Shipper
 */
@Getter
@AllArgsConstructor
public enum VehicleType {
    MOTORBIKE("Xe máy"),
    EBIKE("Xe đạp điện"),
    BICYCLE("Xe đạp"),
    CAR("Ô tô");

    private final String displayName;
}
