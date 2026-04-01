package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trạng thái ngày chấm công
 */
@Getter
@AllArgsConstructor
public enum AttendanceStatus {
    INCOMPLETE("Chưa hoàn thành"),
    COMPLETE("Hoàn thành"),
    ABSENT("Vắng mặt"),
    HOLIDAY("Nghỉ lễ"),
    LEAVE("Nghỉ phép");

    private final String displayName;
}
