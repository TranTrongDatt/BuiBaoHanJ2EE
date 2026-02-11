package fit.hutech.BuiBaoHan.viewmodels;

import java.math.BigDecimal;

import fit.hutech.BuiBaoHan.entities.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record BookPostVm(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(min = 1, max = 255, message = "Tiêu đề phải từ 1-255 ký tự")
        String title,
        
        Long authorId,
        
        Long publisherId,
        
        @NotNull(message = "Giá không được để trống")
        @Positive(message = "Giá phải lớn hơn 0")
        BigDecimal price,
        
        BigDecimal originalPrice,
        
        String isbn,
        
        String coverImage,
        
        String description,
        
        Long categoryId) {

    public static BookPostVm from(@NotNull Book book) {
        return BookPostVm.builder()
                .title(book.getTitle())
                .authorId(book.getAuthor() != null ? book.getAuthor().getId() : null)
                .publisherId(book.getPublisher() != null ? book.getPublisher().getId() : null)
                .price(book.getPrice())
                .originalPrice(book.getOriginalPrice())
                .isbn(book.getIsbn())
                .coverImage(book.getCoverImage())
                .description(book.getDescription())
                .categoryId(book.getCategory() != null ? book.getCategory().getId() : null)
                .build();
    }
}
