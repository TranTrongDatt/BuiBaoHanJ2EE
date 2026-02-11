package fit.hutech.BuiBaoHan.viewmodels;

import java.math.BigDecimal;

import fit.hutech.BuiBaoHan.entities.Book;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BookGetVm(Long id, String title, String author, Long authorId, BigDecimal price, 
                        String category, String publisher, String coverImage, String isbn) {

    public static BookGetVm from(@NotNull Book book) {
        return BookGetVm.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor() != null ? book.getAuthor().getName() : "N/A")
                .authorId(book.getAuthor() != null ? book.getAuthor().getId() : null)
                .price(book.getPrice())
                .category(book.getCategory() != null ? book.getCategory().getName() : "N/A")
                .publisher(book.getPublisher() != null ? book.getPublisher().getName() : "N/A")
                .coverImage(book.getCoverImage())
                .isbn(book.getIsbn())
                .build();
    }
}
