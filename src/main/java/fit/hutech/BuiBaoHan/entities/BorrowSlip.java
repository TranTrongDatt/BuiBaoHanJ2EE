package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Phiếu mượn sách
 */
@Entity
@Table(name = "borrow_slip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowSlip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slip_code", length = 20, unique = true, nullable = false)
    private String slipCode;

    @NotNull(message = "Ngày mượn không được để trống")
    @Column(name = "borrow_date", nullable = false)
    private LocalDateTime borrowDate;

    @NotNull(message = "Ngày dự kiến trả không được để trống")
    @Column(name = "expected_return_date", nullable = false)
    private LocalDateTime expectedReturnDate;

    @Column(name = "actual_return_date")
    private LocalDateTime actualReturnDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BorrowStatus status = BorrowStatus.BORROWING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "extension_count")
    private Integer extensionCount = 0;

    @Builder.Default
    @Column(name = "max_extension")
    private Integer maxExtension = 2; // Tối đa gia hạn 2 lần

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User (người mượn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Quan hệ với Librarian (thủ thư xử lý)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "librarian_id")
    private User librarian;

    // Quan hệ với LibraryCard
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_card_id", nullable = false)
    private LibraryCard libraryCard;

    // Chi tiết phiếu mượn
    @Builder.Default
    @OneToMany(mappedBy = "borrowSlip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BorrowSlipDetail> details = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (slipCode == null || slipCode.isBlank()) {
            slipCode = generateSlipCode();
        }
        if (borrowDate == null) {
            borrowDate = LocalDateTime.now();
        }
    }

    /**
     * Generate mã phiếu mượn
     */
    private String generateSlipCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MV-BS-" + uuid;
    }

    // Helper methods
    public void addDetail(BorrowSlipDetail detail) {
        details.add(detail);
        detail.setBorrowSlip(this);
    }

    public void removeDetail(BorrowSlipDetail detail) {
        details.remove(detail);
        detail.setBorrowSlip(null);
    }

    /**
     * Kiểm tra đã quá hạn chưa
     */
    public boolean isOverdue() {
        return status == BorrowStatus.BORROWING && 
               LocalDateTime.now().isAfter(expectedReturnDate);
    }

    /**
     * Có thể gia hạn không
     */
    public boolean canExtend() {
        return status == BorrowStatus.BORROWING && 
               extensionCount < maxExtension && 
               !isOverdue();
    }

    /**
     * Gia hạn phiếu mượn
     * @param days Số ngày gia hạn thêm
     */
    public void extend(int days) {
        if (!canExtend()) {
            throw new IllegalStateException("Không thể gia hạn phiếu mượn này");
        }
        this.expectedReturnDate = this.expectedReturnDate.plusDays(days);
        this.extensionCount++;
        this.status = BorrowStatus.EXTENDED;
    }

    /**
     * Đánh dấu đã trả sách
     */
    public void markAsReturned() {
        this.actualReturnDate = LocalDateTime.now();
        this.status = BorrowStatus.RETURNED;
    }

    /**
     * Lấy tổng số sách mượn
     */
    public int getTotalBooks() {
        return details.stream()
                .mapToInt(BorrowSlipDetail::getQuantity)
                .sum();
    }

    /**
     * Tính số ngày quá hạn
     */
    public long getOverdueDays() {
        if (!isOverdue()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(expectedReturnDate, LocalDateTime.now());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BorrowSlip that = (BorrowSlip) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BorrowSlip{" +
                "id=" + id +
                ", slipCode='" + slipCode + '\'' +
                ", status=" + status +
                '}';
    }
}
