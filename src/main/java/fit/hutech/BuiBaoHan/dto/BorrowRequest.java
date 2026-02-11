package fit.hutech.BuiBaoHan.dto;

import java.util.List;

import fit.hutech.BuiBaoHan.constants.BookCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BorrowRequest(
        @NotNull(message = "User ID không được để trống")
        Long userId,
        
        @NotEmpty(message = "Danh sách sách mượn không được trống")
        List<BookBorrowItem> items,
        
        String notes
) {
    @Builder
    public record BookBorrowItem(
            @NotNull(message = "Book ID không được để trống")
            Long bookId,
            
            @Min(value = 1, message = "Số lượng phải ít nhất là 1")
            int quantity,
            
            @NotNull(message = "Tình trạng sách không được để trống")
            BookCondition condition
    ) {}
}
