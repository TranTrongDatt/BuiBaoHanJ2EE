package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Loại hợp đồng của Shipper
 */
@Getter
@AllArgsConstructor
public enum ContractType {
    FULL_TIME("Toàn thời gian"),
    PART_TIME("Bán thời gian");

    private final String displayName;
}
