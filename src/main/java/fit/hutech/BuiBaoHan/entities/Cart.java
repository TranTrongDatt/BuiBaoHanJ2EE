package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho Giỏ hàng
 */
@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với User (một giỏ hàng thuộc một user)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Danh sách các sản phẩm trong giỏ hàng
    @Builder.Default
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    // Helper methods
    public void addItem(CartItem item) {
        // Kiểm tra nếu sách đã có trong giỏ hàng, tăng số lượng
        for (CartItem existingItem : items) {
            if (existingItem.getBook().getId().equals(item.getBook().getId())) {
                existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
                return;
            }
        }
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    public void removeItemByBookId(Long bookId) {
        items.removeIf(item -> item.getBook().getId().equals(bookId));
    }

    /**
     * Xóa toàn bộ giỏ hàng
     */
    public void clear() {
        items.clear();
    }

    /**
     * Lấy tổng số sản phẩm trong giỏ hàng
     */
    public int getTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    /**
     * Lấy tổng giá trị giỏ hàng
     */
    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Lấy tổng giá trị giỏ hàng (giá gốc)
     */
    public BigDecimal getTotalOriginalPrice() {
        return items.stream()
                .map(item -> {
                    BigDecimal price = item.getBook().getOriginalPrice();
                    if (price == null) {
                        price = item.getBook().getPrice();
                    }
                    return price != null ? price.multiply(BigDecimal.valueOf(item.getQuantity())) : BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Lấy số tiền tiết kiệm
     */
    public BigDecimal getSavings() {
        return getTotalOriginalPrice().subtract(getTotalPrice());
    }

    /**
     * Kiểm tra giỏ hàng có trống không
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return id != null && Objects.equals(id, cart.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Cart{" +
                "id=" + id +
                ", itemCount=" + items.size() +
                '}';
    }
}
