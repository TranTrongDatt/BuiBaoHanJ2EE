package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Loại phạt cho Shipper
 * 
 * Quy định phạt:
 * - Đi trễ: 8,000 VND/phút
 * - Thiếu chấm công: 8,000 VND/card
 */
@Getter
@AllArgsConstructor
public enum PenaltyType {
    LATE("Đi trễ"),
    MISSING_PUNCH("Thiếu chấm công"),
    EARLY_LEAVE("Về sớm"),
    FAILED_DELIVERY("Giao hàng thất bại"),
    OTHER("Khác");

    private final String displayName;
}
