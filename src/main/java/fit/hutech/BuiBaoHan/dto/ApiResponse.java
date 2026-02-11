package fit.hutech.BuiBaoHan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic DTO cho response API chung
 * @param <T> Kiểu dữ liệu của data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Đã tạo thành công")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<Void> deleted() {
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Đã xóa thành công")
                .build();
    }
    
    public static <T> ApiResponse<T> notFound(String entity) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Không tìm thấy " + entity)
                .build();
    }
    
    public static <T> ApiResponse<T> updated(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Đã cập nhật thành công")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
