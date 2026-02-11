package fit.hutech.BuiBaoHan.dto;

import java.util.List;

import fit.hutech.BuiBaoHan.constants.BookCondition;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ReturnRequest(
        @NotNull(message = "Slip ID không được để trống")
        Long borrowSlipId,
        
        @NotEmpty(message = "Danh sách sách trả không được trống")
        List<BookReturnItem> items,
        
        String notes
) {
    @Builder
    public record BookReturnItem(
            @NotNull(message = "Detail ID không được để trống")
            Long detailId,
            
            @NotNull(message = "Tình trạng sách trả không được để trống")
            BookCondition returnCondition,
            
            String notes
    ) {}
}
