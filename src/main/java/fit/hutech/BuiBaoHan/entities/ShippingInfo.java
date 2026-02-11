package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.ShippingStatus;
import fit.hutech.BuiBaoHan.constants.ShippingType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Thông tin vận chuyển
 */
@Entity
@Table(name = "shipping_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_number", length = 50, unique = true)
    private String trackingNumber;

    @Column(name = "carrier", length = 100)
    private String carrier;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipping_type", length = 20)
    private ShippingType shippingType;

    @Column(name = "shipping_fee", precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30)
    private ShippingStatus status = ShippingStatus.PENDING;

    // Địa chỉ
    @Column(name = "sender_address", columnDefinition = "TEXT")
    private String senderAddress;

    @Column(name = "receiver_address", columnDefinition = "TEXT")
    private String receiverAddress;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 15)
    private String receiverPhone;

    // Thời gian giao hàng
    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    // Vị trí hiện tại
    @Column(name = "current_location", length = 255)
    private String currentLocation;

    @Column(name = "last_location_update")
    private LocalDateTime lastLocationUpdate;

    // Lịch sử trạng thái
    @Column(name = "status_history", columnDefinition = "TEXT")
    private String statusHistory;

    // Thời gian các trạng thái
    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "in_transit_at")
    private LocalDateTime inTransitAt;

    @Column(name = "out_for_delivery_at")
    private LocalDateTime outForDeliveryAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    // Thông tin thất bại
    @Column(name = "failed_reason", columnDefinition = "TEXT")
    private String failedReason;

    @Column(name = "delivery_attempts")
    private Integer deliveryAttempts;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingInfo that = (ShippingInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
