package fit.hutech.BuiBaoHan.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum định nghĩa các loại Role trong hệ thống
 */
@Getter
@AllArgsConstructor
public enum RoleType {
    
    ADMIN("ROLE_ADMIN", "Quản trị viên hệ thống"),
    STAFF("ROLE_STAFF", "Nhân viên"),
    USER("ROLE_USER", "Người dùng thông thường");
    
    private final String authority;
    private final String description;
    
    /**
     * Lấy tên role (để lưu vào database)
     */
    public String getRoleName() {
        return this.authority;
    }
}
