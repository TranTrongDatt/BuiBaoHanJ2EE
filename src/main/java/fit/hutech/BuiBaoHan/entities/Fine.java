package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.FineStatus;
import fit.hutech.BuiBaoHan.constants.FineType;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Phiếu phạt
 */
@Entity
@Table(name = "fine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_type", length = 20)
    private FineType fineType = FineType.LATE_RETURN;

    @NotBlank(message = "Lý do phạt không được để trống")
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @NotNull(message = "Số tiền phạt không được để trống")
    @Positive(message = "Số tiền phạt phải lớn hơn 0")
    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Builder.Default
    @Column(name = "paid")
    private Boolean paid = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private FineStatus status = FineStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @Column(name = "waived_reason", columnDefinition = "TEXT")
    private String waivedReason;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User (người bị phạt)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Quan hệ với Librarian (thủ thư lập phiếu phạt)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "librarian_id")
    private User librarian;

    // Quan hệ với BorrowSlip (phiếu mượn liên quan)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_slip_id")
    private BorrowSlip borrowSlip;

    // Quan hệ với BorrowRecord (phiếu mượn liên quan - mới)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_record_id")
    private BorrowRecord borrowRecord;

    /**
     * Đánh dấu đã thanh toán
     */
    public void markAsPaid(String paymentMethod) {
        this.paid = true;
        this.paidDate = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
    }

    /**
     * Kiểm tra đã thanh toán chưa
     */
    public boolean isPaid() {
        return paid != null && paid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fine fine = (Fine) o;
        return id != null && Objects.equals(id, fine.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Fine{" +
                "id=" + id +
                ", amount=" + amount +
                ", paid=" + paid +
                '}';
    }
}
