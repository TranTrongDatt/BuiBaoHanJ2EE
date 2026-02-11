package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.constants.CardType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Thẻ thư viện
 */
@Entity
@Table(name = "library_card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", length = 20, unique = true, nullable = false)
    private String cardNumber;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @NotNull(message = "Ngày phát hành không được để trống")
    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @NotNull(message = "Ngày hết hạn không được để trống")
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", length = 20)
    private CardType cardType = CardType.STANDARD;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Builder.Default
    @Column(name = "max_borrow_days")
    private Integer maxBorrowDays = 14;

    @Builder.Default
    @Column(name = "max_books_allowed")
    private Integer maxBooksAllowed = 5;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User (một thẻ thuộc một user)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Thủ thư phát hành thẻ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_librarian_id")
    private User issuedByLibrarian;

    @PrePersist
    public void prePersist() {
        if (cardNumber == null || cardNumber.isBlank()) {
            cardNumber = generateCardNumber();
        }
        if (issueDate == null) {
            issueDate = LocalDateTime.now();
        }
        if (expiryDate == null) {
            // Mặc định thẻ có hiệu lực 1 năm
            expiryDate = issueDate.plusYears(1);
        }
    }

    /**
     * Generate mã thẻ thư viện
     */
    private String generateCardNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MV-CARD-" + uuid;
    }

    /**
     * Kiểm tra thẻ còn hiệu lực không
     */
    public boolean isValid() {
        return status == CardStatus.ACTIVE && 
               expiryDate != null && 
               LocalDateTime.now().isBefore(expiryDate);
    }

    /**
     * Kiểm tra thẻ có trạng thái ACTIVE không
     */
    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }

    /**
     * Kiểm tra thẻ đã hết hạn chưa
     */
    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * Gia hạn thẻ thêm 1 năm
     */
    public void extend() {
        if (isExpired()) {
            this.expiryDate = LocalDateTime.now().plusYears(1);
        } else {
            this.expiryDate = this.expiryDate.plusYears(1);
        }
        this.status = CardStatus.ACTIVE;
    }

    /**
     * Lấy số sách tối đa có thể mượn
     */
    public int getMaxBooks() {
        return cardType != null ? cardType.getMaxBooks() : CardType.STANDARD.getMaxBooks();
    }

    /**
     * Lấy số ngày mượn tối đa
     */
    public int getMaxDays() {
        return cardType != null ? cardType.getMaxDays() : CardType.STANDARD.getMaxDays();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryCard that = (LibraryCard) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LibraryCard{" +
                "id=" + id +
                ", cardNumber='" + cardNumber + '\'' +
                ", cardType=" + cardType +
                ", status=" + status +
                '}';
    }
}
