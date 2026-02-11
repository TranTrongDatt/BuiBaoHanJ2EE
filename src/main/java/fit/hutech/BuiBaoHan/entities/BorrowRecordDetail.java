package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity chi tiết phiếu mượn sách (cho BorrowService)
 */
@Entity
@Table(name = "borrow_record_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRecordDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "quantity")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    private Integer quantity = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "borrow_condition", length = 20)
    private BookCondition borrowCondition = BookCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_condition", length = 20)
    private BookCondition returnCondition;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Quan hệ với BorrowRecord
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrow_record_id", nullable = false)
    private BorrowRecord borrowRecord;

    // Quan hệ với Book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowRecordDetail that = (BorrowRecordDetail) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
