package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.BookStatus;
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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @NotBlank(message = "Title must not be blank")
    private String title;

    @Column(name = "isbn", length = 20, unique = true)
    @Size(max = 20, message = "ISBN max 20 characters")
    private String isbn;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "images", columnDefinition = "TEXT")
    private String imagesJson;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "description_pdf_url", length = 500)
    private String descriptionPdfUrl;

    @Column(name = "price", precision = 12, scale = 2)
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "page_count")
    @Min(value = 1, message = "Page count must be at least 1")
    private Integer pageCount;

    @Builder.Default
    @Column(name = "total_quantity")
    @Min(value = 0, message = "Total quantity cannot be negative")
    private Integer totalQuantity = 0;

    @Builder.Default
    @Column(name = "stock_quantity")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(name = "library_stock")
    @Min(value = 0, message = "Library stock cannot be negative")
    private Integer libraryStock = 0;

    @Column(name = "slug", length = 300, unique = true)
    private String slug;

    @Column(name = "edition")
    private Integer edition;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "language", length = 50)
    @Builder.Default
    private String language = "Tiếng Việt";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private BookStatus status = BookStatus.AVAILABLE;

    @Builder.Default
    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Builder.Default
    @Column(name = "sold_count")
    private Long soldCount = 0L;

    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với Author
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private Author author;

    // Quan hệ với Publisher
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publisher_id", nullable = false)
    @ToString.Exclude
    private Publisher publisher;

    // Quan hệ với Category
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    @ToString.Exclude
    private Category category;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private List<ItemInvoice> itemInvoices = new ArrayList<>();

    // Helper methods
    /**
     * Lấy số lượng tồn kho (alias cho stockQuantity)
     */
    public Integer getStock() {
        return stockQuantity;
    }

    /**
     * Đặt số lượng tồn kho (alias cho stockQuantity)
     */
    public void setStock(Integer stock) {
        this.stockQuantity = stock;
    }

    /**
     * Kiểm tra sách còn hàng không
     */
    public boolean isInStock() {
        return stockQuantity != null && stockQuantity > 0;
    }

    /**
     * Lấy danh sách hình ảnh bổ sung
     */
    public List<String> getImages() {
        if (imagesJson == null || imagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(java.util.Arrays.asList(imagesJson.split(",")));
    }

    /**
     * Đặt danh sách hình ảnh bổ sung
     */
    public void setImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            this.imagesJson = null;
        } else {
            this.imagesJson = String.join(",", images);
        }
    }

    /**
     * Giảm số lượng tồn kho
     */
    public void decreaseStock(int quantity) {
        if (this.stockQuantity >= quantity) {
            this.stockQuantity -= quantity;
            this.soldCount += quantity;
            if (this.stockQuantity == 0) {
                this.status = BookStatus.OUT_OF_STOCK;
            }
        } else {
            throw new IllegalArgumentException("Không đủ số lượng trong kho");
        }
    }

    /**
     * Tăng số lượng tồn kho
     */
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
        this.totalQuantity += quantity;
        if (this.status == BookStatus.OUT_OF_STOCK) {
            this.status = BookStatus.AVAILABLE;
        }
    }

    /**
     * Tính phần trăm giảm giá
     */
    public int getDiscountPercent() {
        if (originalPrice != null && originalPrice.compareTo(BigDecimal.ZERO) > 0 && price != null) {
            BigDecimal discount = originalPrice.subtract(price);
            return discount.multiply(BigDecimal.valueOf(100))
                    .divide(originalPrice, 0, java.math.RoundingMode.HALF_UP)
                    .intValue();
        }
        return 0;
    }

    /**
     * Get featured status (alias for isFeatured)
     */
    public Boolean getFeatured() {
        return this.isFeatured;
    }

    /**
     * Set featured status (alias for isFeatured)
     */
    public void setFeatured(Boolean featured) {
        this.isFeatured = featured;
    }

    /**
     * Get active status based on BookStatus
     */
    public Boolean getActive() {
        return this.status != BookStatus.DISCONTINUED;
    }

    /**
     * Set active status
     */
    public void setActive(Boolean active) {
        if (active != null && active) {
            if (this.status == BookStatus.DISCONTINUED) {
                this.status = BookStatus.AVAILABLE;
            }
        } else {
            this.status = BookStatus.DISCONTINUED;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book book)) {
            return false;
        }
        return getId() != null && Objects.equals(getId(),
                book.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
