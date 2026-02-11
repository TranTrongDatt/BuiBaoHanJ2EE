package fit.hutech.BuiBaoHan.dto;

import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.constants.ShippingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record OrderCreateRequest(
        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(max = 100, message = "Tên người nhận không quá 100 ký tự")
        String receiverName,
        
        @NotBlank(message = "Số điện thoại không được để trống")
        @Size(max = 15, message = "Số điện thoại không quá 15 ký tự")
        String receiverPhone,
        
        @NotBlank(message = "Địa chỉ giao hàng không được để trống")
        String shippingAddress,
        
        ShippingType shippingType,
        
        PaymentMethod paymentMethod,
        
        String notes,
        
        String couponCode
) {
    public OrderCreateRequest {
        if (shippingType == null) {
            shippingType = ShippingType.STANDARD;
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.COD;
        }
    }
}
