package fit.hutech.BuiBaoHan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import fit.hutech.BuiBaoHan.entities.Book;

/**
 * Book detail response with related data
 */
public record BookDetailResponse(
        Long id,
        String title,
        String slug,
        String isbn,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        Integer libraryStock,
        String coverImage,
        List<String> images,
        Integer pageCount,
        String language,
        LocalDate publishDate,
        Integer viewCount,
        Integer soldCount,
        Double averageRating,
        Integer reviewCount,
        
        // Related entities
        AuthorInfo author,
        PublisherInfo publisher,
        CategoryInfo category,
        FieldInfo field,
        
        // Related books
        List<BookSummary> relatedBooks,
        List<BookSummary> sameAuthorBooks,
        
        // User-specific (nullable)
        Boolean inWishlist,
        Boolean inCart,
        Integer cartQuantity
) {
    /**
     * Create from Book entity
     */
    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getSlug(),
                book.getIsbn(),
                book.getDescription(),
                book.getPrice(),
                book.getOriginalPrice(),
                book.getStock(),
                book.getLibraryStock(),
                book.getCoverImage(),
                List.of(), // images - Book entity không có field này
                book.getPageCount(),
                book.getLanguage(),
                book.getPublishDate(),
                book.getViewCount() != null ? book.getViewCount().intValue() : 0,
                book.getSoldCount() != null ? book.getSoldCount().intValue() : 0,
                0.0, // averageRating - cần tính từ reviews
                0, // reviewCount - cần tính từ reviews
                book.getAuthor() != null ? AuthorInfo.from(book.getAuthor()) : null,
                book.getPublisher() != null ? PublisherInfo.from(book.getPublisher()) : null,
                book.getCategory() != null ? CategoryInfo.from(book.getCategory()) : null,
                book.getCategory() != null && book.getCategory().getField() != null 
                        ? FieldInfo.from(book.getCategory().getField()) : null,
                null, null, null, null, null
        );
    }

    /**
     * Create with user-specific data
     */
    public BookDetailResponse withUserData(Boolean inWishlist, Boolean inCart, Integer cartQuantity) {
        return new BookDetailResponse(
                id, title, slug, isbn, description, price, originalPrice, stock, libraryStock,
                coverImage, images, pageCount, language, publishDate, viewCount, soldCount,
                averageRating, reviewCount, author, publisher, category, field,
                relatedBooks, sameAuthorBooks, inWishlist, inCart, cartQuantity
        );
    }

    /**
     * Create with related books
     */
    public BookDetailResponse withRelatedBooks(List<BookSummary> related, List<BookSummary> sameAuthor) {
        return new BookDetailResponse(
                id, title, slug, isbn, description, price, originalPrice, stock, libraryStock,
                coverImage, images, pageCount, language, publishDate, viewCount, soldCount,
                averageRating, reviewCount, author, publisher, category, field,
                related, sameAuthor, inWishlist, inCart, cartQuantity
        );
    }

    // ==================== Nested Records ====================

    public record AuthorInfo(Long id, String name, String avatar, String nationality) {
        public static AuthorInfo from(fit.hutech.BuiBaoHan.entities.Author author) {
            return new AuthorInfo(author.getId(), author.getName(), author.getAvatar(), author.getNationality());
        }
    }

    public record PublisherInfo(Long id, String name, String logo) {
        public static PublisherInfo from(fit.hutech.BuiBaoHan.entities.Publisher publisher) {
            return new PublisherInfo(publisher.getId(), publisher.getName(), publisher.getLogo());
        }
    }

    public record CategoryInfo(Long id, String name, String slug) {
        public static CategoryInfo from(fit.hutech.BuiBaoHan.entities.Category category) {
            return new CategoryInfo(category.getId(), category.getName(), category.getSlug());
        }
    }

    public record FieldInfo(Long id, String name, String slug) {
        public static FieldInfo from(fit.hutech.BuiBaoHan.entities.Field field) {
            return new FieldInfo(field.getId(), field.getName(), field.getSlug());
        }
    }

    public record BookSummary(Long id, String title, String slug, String coverImage, BigDecimal price, String authorName) {
        public static BookSummary from(Book book) {
            return new BookSummary(
                    book.getId(),
                    book.getTitle(),
                    book.getSlug(),
                    book.getCoverImage(),
                    book.getPrice(),
                    book.getAuthor() != null ? book.getAuthor().getName() : null
            );
        }
    }
}
