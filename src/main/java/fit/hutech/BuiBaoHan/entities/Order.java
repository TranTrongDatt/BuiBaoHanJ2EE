package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.constants.PaymentStatus;
import fit.hutech.BuiBaoHan.constants.ShippingType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Đơn hàng
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", length = 30, unique = true, nullable = false)
    private String orderCode;

    // Thông tin người nhận
    @NotBlank(message = "Tên người nhận không được để trống")
    @Column(name = "receiver_name", length = 100, nullable = false)
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Column(name = "receiver_phone", length = 15, nullable = false)
    private String receiverPhone;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Column(name = "shipping_address", columnDefinition = "TEXT", nullable = false)
    private String shippingAddress;

    // Thông tin giao hàng
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_type", length = 20)
    private ShippingType shippingType = ShippingType.STANDARD;

    @Builder.Default
    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "tracking_number", length = 50)
    private String trackingNumber;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    // Thông tin thanh toán
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    // Giá trị đơn hàng
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal; // Tổng tiền hàng

    @Builder.Default
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Giảm giá

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount; // Tổng thanh toán

    // Trạng thái đơn hàng
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "confirmed_date")
    private LocalDateTime confirmedDate;

    @Column(name = "cancelled_date")
    private LocalDateTime cancelledDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Chi tiết đơn hàng
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (orderCode == null || orderCode.isBlank()) {
            orderCode = generateOrderCode();
        }
        calculateTotals();
    }

    /**
     * Generate mã đơn hàng
     */
    private String generateOrderCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MV-ORD-" + uuid;
    }

    // Helper methods
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Tính toán tổng tiền
     */
    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (shippingFee == null) {
            shippingFee = shippingType != null ? shippingType.getFee() : BigDecimal.ZERO;
        }

        this.totalAmount = subtotal.subtract(discountAmount).add(shippingFee);
    }

    /**
     * Lấy tổng số sản phẩm
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    /**
     * Đánh dấu đã thanh toán
     */
    public void markAsPaid(String transactionId) {
        this.paymentStatus = PaymentStatus.PAID;
        this.paidDate = LocalDateTime.now();
        this.transactionId = transactionId;
    }

    /**
     * Xác nhận đơn hàng
     */
    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * Hủy đơn hàng
     */
    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledDate = LocalDateTime.now();
    }

    /**
     * Đánh dấu đang giao hàng
     */
    public void ship(String trackingNumber) {
        this.status = OrderStatus.SHIPPING;
        this.trackingNumber = trackingNumber;
        this.shippedDate = LocalDateTime.now();
    }

    /**
     * Đánh dấu đã giao hàng
     */
    public void deliver() {
        this.status = OrderStatus.DELIVERED;
        this.deliveredDate = LocalDateTime.now();
    }

    /**
     * Hoàn thành đơn hàng
     */
    public void complete() {
        this.status = OrderStatus.COMPLETED;
        if (paymentMethod == PaymentMethod.COD && paymentStatus != PaymentStatus.PAID) {
            markAsPaid("COD-" + orderCode);
        }
    }

    /**
     * Kiểm tra có thể hủy không
     */
    public boolean canCancel() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    // ==================== Alias Getters for API compatibility ====================

    /**
     * Alias for getItems() - returns order items list
     */
    public List<OrderItem> getOrderItems() {
        return items;
    }

    /**
     * Alias for getOrderCode() - returns order number
     */
    public String getOrderNumber() {
        return orderCode;
    }

    /**
     * Alias for getDiscountAmount() - returns discount
     */
    public BigDecimal getDiscount() {
        return discountAmount;
    }

    /**
     * Alias for getTotalAmount() - returns total
     */
    public BigDecimal getTotal() {
        return totalAmount;
    }

    /**
     * Alias for getReceiverName() - returns shipping recipient name
     */
    public String getShippingName() {
        return receiverName;
    }

    /**
     * Alias for getReceiverPhone() - returns shipping phone
     */
    public String getShippingPhone() {
        return receiverPhone;
    }

    /**
     * Alias for getNotes() - returns order note
     */
    public String getNote() {
        return notes;
    }

    /**
     * Alias for getConfirmedDate() - returns confirmed timestamp
     */
    public LocalDateTime getConfirmedAt() {
        return confirmedDate;
    }

    /**
     * Alias for getShippedDate() - returns shipped timestamp
     */
    public LocalDateTime getShippedAt() {
        return shippedDate;
    }

    /**
     * Alias for getDeliveredDate() - returns delivered timestamp
     */
    public LocalDateTime getDeliveredAt() {
        return deliveredDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderCode='" + orderCode + '\'' +
                ", status=" + status +
                '}';
    }
}
