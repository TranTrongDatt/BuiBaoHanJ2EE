package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity đại diện cho Chi tiết đơn hàng
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "quantity")
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    private Integer quantity = 1;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price; // Giá tại thời điểm đặt hàng

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice; // Giá gốc tại thời điểm đặt hàng

    // Lưu thông tin sách tại thời điểm đặt hàng (phòng trường hợp sách bị xóa/sửa)
    @Column(name = "book_title", length = 255)
    private String bookTitle;

    @Column(name = "book_isbn", length = 20)
    private String bookIsbn;

    @Column(name = "book_image", length = 255)
    private String bookImage;

    // Quan hệ với Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Quan hệ với Book
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    /**
     * Tính thành tiền
     */
    public BigDecimal getSubtotal() {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Tính số tiền tiết kiệm
     */
    public BigDecimal getSavings() {
        if (originalPrice == null || price == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal saving = originalPrice.subtract(price).multiply(BigDecimal.valueOf(quantity));
        return saving.compareTo(BigDecimal.ZERO) > 0 ? saving : BigDecimal.ZERO;
    }

    /**
     * Lấy phần trăm giảm giá
     */
    public int getDiscountPercent() {
        if (originalPrice == null || price == null || originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        BigDecimal discount = originalPrice.subtract(price)
                .divide(originalPrice, 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return discount.intValue();
    }

    /**
     * Copy thông tin từ sách vào OrderItem (snapshot)
     */
    public void snapshotFromBook(Book book) {
        if (book != null) {
            this.book = book;
            this.bookTitle = book.getTitle();
            this.bookIsbn = book.getIsbn();
            this.bookImage = book.getCoverImage();
            this.price = book.getPrice();
            this.originalPrice = book.getOriginalPrice();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return id != null && Objects.equals(id, orderItem.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", bookTitle='" + bookTitle + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
