package fit.hutech.BuiBaoHan.dto;

import java.math.BigDecimal;

import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PaymentRequest(
        @NotNull(message = "Order ID không được để trống")
        Long orderId,
        
        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod,
        
        @Positive(message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,
        
        String returnUrl,
        
        String cancelUrl,
        
        String ipAddress
) {}
