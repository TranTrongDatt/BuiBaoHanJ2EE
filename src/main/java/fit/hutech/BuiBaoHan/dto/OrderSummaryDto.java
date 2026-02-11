package fit.hutech.BuiBaoHan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.constants.PaymentStatus;
import fit.hutech.BuiBaoHan.constants.ShippingType;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.OrderItem;
import lombok.Builder;

@Builder
public record OrderSummaryDto(
        Long id,
        String orderCode,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        ShippingType shippingType,
        BigDecimal shippingFee,
        String trackingNumber,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        OrderStatus status,
        String notes,
        Integer totalItems,
        LocalDateTime createdAt,
        LocalDateTime shippedDate,
        LocalDateTime deliveredDate,
        List<OrderItemDto> items
) {
    public static OrderSummaryDto from(Order order) {
        return OrderSummaryDto.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingType(order.getShippingType())
                .shippingFee(order.getShippingFee())
                .trackingNumber(order.getTrackingNumber())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .notes(order.getNotes())
                .totalItems(order.getTotalItems())
                .createdAt(order.getCreatedAt())
                .shippedDate(order.getShippedDate())
                .deliveredDate(order.getDeliveredDate())
                .items(order.getItems().stream().map(OrderItemDto::from).toList())
                .build();
    }
    
    @Builder
    public record OrderItemDto(
            Long id,
            Long bookId,
            String bookTitle,
            String bookImage,
            String bookIsbn,
            BigDecimal price,
            BigDecimal originalPrice,
            Integer quantity,
            BigDecimal subtotal,
            Integer discountPercent
    ) {
        public static OrderItemDto from(OrderItem item) {
            return OrderItemDto.builder()
                    .id(item.getId())
                    .bookId(item.getBook() != null ? item.getBook().getId() : null)
                    .bookTitle(item.getBookTitle())
                    .bookImage(item.getBookImage())
                    .bookIsbn(item.getBookIsbn())
                    .price(item.getPrice())
                    .originalPrice(item.getOriginalPrice())
                    .quantity(item.getQuantity())
                    .subtotal(item.getSubtotal())
                    .discountPercent(item.getDiscountPercent())
                    .build();
        }
    }
}
