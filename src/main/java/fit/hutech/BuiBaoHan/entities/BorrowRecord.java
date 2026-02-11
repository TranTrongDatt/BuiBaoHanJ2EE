package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.BookCondition;
import fit.hutech.BuiBaoHan.constants.BorrowStatus;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity quản lý phiếu mượn sách (cho BorrowService)
 */
@Entity
@Table(name = "borrow_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrow_date", nullable = false)
    private LocalDateTime borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BorrowStatus status = BorrowStatus.BORROWING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "extensions")
    private Integer extensions = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với LibraryCard
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_card_id", nullable = false)
    private LibraryCard libraryCard;

    // Chi tiết phiếu mượn
    @Builder.Default
    @OneToMany(mappedBy = "borrowRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowRecordDetail> details = new ArrayList<>();

    // Helper methods
    public void addDetail(BorrowRecordDetail detail) {
        details.add(detail);
        detail.setBorrowRecord(this);
    }

    public void removeDetail(BorrowRecordDetail detail) {
        details.remove(detail);
        detail.setBorrowRecord(null);
    }

    /**
     * Helper method - returns the book from the first detail
     */
    public Book getBook() {
        if (details != null && !details.isEmpty()) {
            return details.get(0).getBook();
        }
        return null;
    }

    /**
     * Helper method - returns the condition from the first detail
     */
    public BookCondition getCondition() {
        if (details != null && !details.isEmpty()) {
            BorrowRecordDetail firstDetail = details.get(0);
            return firstDetail.getReturnCondition() != null 
                    ? firstDetail.getReturnCondition() 
                    : firstDetail.getBorrowCondition();
        }
        return null;
    }

    /**
     * Helper method - returns the user from the library card
     */
    public User getUser() {
        return libraryCard != null ? libraryCard.getUser() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowRecord that = (BorrowRecord) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
