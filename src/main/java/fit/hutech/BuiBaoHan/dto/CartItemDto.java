package fit.hutech.BuiBaoHan.dto;

import java.math.BigDecimal;

import fit.hutech.BuiBaoHan.entities.CartItem;
import lombok.Builder;

@Builder
public record CartItemDto(
        Long id,
        Long bookId,
        String bookTitle,
        String bookImage,
        String authorName,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer quantity,
        BigDecimal subtotal,
        Boolean isInStock,
        Integer stockQuantity
) {
    public static CartItemDto from(CartItem item) {
        var book = item.getBook();
        return CartItemDto.builder()
                .id(item.getId())
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookImage(book.getCoverImage())
                .authorName(book.getAuthor() != null ? book.getAuthor().getName() : "N/A")
                .price(book.getPrice())
                .originalPrice(book.getOriginalPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .isInStock(book.isInStock())
                .stockQuantity(book.getStockQuantity())
                .build();
    }
}
