package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Loại chấm công (bấm thẻ)
 * 
 * Quy định chấm công:
 * - Tối thiểu 2 lần: CHECK_IN + CHECK_OUT (bắt buộc)
 * - Tối đa 4 lần: thêm BREAK_START + BREAK_END (nếu nghỉ trưa)
 */
@Getter
@AllArgsConstructor
public enum PunchType {
    CHECK_IN("Vào ca", 1),
    BREAK_START("Vào nghỉ", 2),
    BREAK_END("Ra nghỉ", 3),
    CHECK_OUT("Ra ca", 4);

    private final String displayName;
    private final int order; // Thứ tự trong ngày
}
