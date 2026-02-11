package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.util.Objects;

import fit.hutech.BuiBaoHan.constants.BookCondition;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Chi tiết phiếu mượn
 */
@Entity
@Table(name = "borrow_slip_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowSlipDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "quantity")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    private Integer quantity = 1;

    @NotNull(message = "Tình trạng sách khi mượn không được để trống")
    @Enumerated(EnumType.STRING)
    @Column(name = "borrow_condition", length = 20, nullable = false)
    private BookCondition borrowCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_condition", length = 20)
    private BookCondition returnCondition;

    @Builder.Default
    @Column(name = "fine_amount", precision = 12, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "is_returned")
    private Boolean isReturned = false;

    // Quan hệ với BorrowSlip
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_slip_id", nullable = false)
    private BorrowSlip borrowSlip;

    // Quan hệ với Book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /**
     * Tính tiền phạt dựa trên tình trạng sách khi trả
     */
    public BigDecimal calculateFine() {
        if (returnCondition == null || book == null || book.getPrice() == null) {
            return BigDecimal.ZERO;
        }

        // So sánh tình trạng khi trả với khi mượn
        if (returnCondition.ordinal() <= borrowCondition.ordinal()) {
            return BigDecimal.ZERO; // Tình trạng tốt hơn hoặc bằng -> không phạt
        }

        // Tính phạt theo tình trạng
        return switch (returnCondition) {
            case DAMAGED -> book.getPrice().multiply(BigDecimal.valueOf(0.5)); // 50% giá sách
            case LOST -> book.getPrice(); // 100% giá sách
            case POOR -> BigDecimal.valueOf(10000); // 10,000 VND
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Đánh dấu đã trả sách
     */
    public void markAsReturned(BookCondition condition) {
        this.returnCondition = condition;
        this.isReturned = true;
        this.fineAmount = calculateFine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowSlipDetail that = (BorrowSlipDetail) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BorrowSlipDetail{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", borrowCondition=" + borrowCondition +
                '}';
    }
}
