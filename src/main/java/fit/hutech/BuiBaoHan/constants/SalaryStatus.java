package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Trạng thái bảng lương tháng
 */
@Getter
@AllArgsConstructor
public enum SalaryStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    PAID("Đã thanh toán"),
    REJECTED("Từ chối");

    private final String displayName;
}
