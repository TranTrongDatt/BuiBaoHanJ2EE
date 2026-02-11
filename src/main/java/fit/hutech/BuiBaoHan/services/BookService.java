package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.BookStatus;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(isolation = Isolation.SERIALIZABLE,
        rollbackFor = {Exception.class, Throwable.class})
public class BookService {

    private final IBookRepository bookRepository;
    private final ICategoryRepository categoryRepository;
    private final SlugService slugService;

    public List<Book> getAllBooks(Integer pageNo,
            Integer pageSize,
            String sortBy) {
        return bookRepository.findAllBooks(pageNo, pageSize, sortBy).getContent();
    }
    
    /**
     * Get paginated books with full pagination info
     */
    @Transactional(readOnly = true)
    public Page<Book> getAllBooksPage(Integer pageNo, Integer pageSize, String sortBy) {
        return bookRepository.findAllBooks(pageNo, pageSize, sortBy);
    }

    /**
     * Lấy book theo ID với eager loading author, publisher, category
     * Sử dụng cho trang edit để tránh LazyInitializationException
     */
    @Transactional(readOnly = true)
    public Optional<Book> getBookById(Long id) {
        log.info("Finding book by ID: {}", id);
        if (id == null) {
            log.warn("Book ID is null!");
            return Optional.empty();
        }
        // Try custom query with eager loading first
        var result = bookRepository.findWithDetailsById(id);
        if (result.isEmpty()) {
            // Fallback to standard JPA findById
            log.warn("Custom findWithDetailsById returned empty, trying standard findById");
            result = bookRepository.findById(id);
        }
        log.info("Book with ID {} found: {}", id, result.isPresent());
        return result;
    }

    /**
     * Tìm sách theo slug
     */
    @Transactional(readOnly = true)
    public Optional<Book> getBookBySlug(String slug) {
        return bookRepository.findBySlug(slug);
    }

    /**
     * Tìm sách theo ISBN
     */
    @Transactional(readOnly = true)
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    @Transactional
    public void addBook(Book book) {
        // Generate slug if not set
        if (book.getSlug() == null || book.getSlug().isEmpty()) {
            String slug = slugService.toUniqueSlug(book.getTitle(), bookRepository::existsBySlug);
            book.setSlug(slug);
        }
        bookRepository.save(book);
        log.info("Added book: {}", book.getTitle());
    }

    @Transactional
    public void updateBook(@NotNull Book book) {
        Book existingBook = bookRepository.findById(book.getId())
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + book.getId()));
        
        // Update basic info
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setPublisher(book.getPublisher());
        existingBook.setCategory(book.getCategory());
        
        // Update detail info
        existingBook.setDescription(book.getDescription());
        existingBook.setIsbn(book.getIsbn());
        existingBook.setEdition(book.getEdition());
        existingBook.setLanguage(book.getLanguage());
        existingBook.setPageCount(book.getPageCount());
        
        // Update pricing & inventory
        existingBook.setOriginalPrice(book.getOriginalPrice());
        existingBook.setPrice(book.getPrice());
        existingBook.setStockQuantity(book.getStockQuantity());
        existingBook.setLibraryStock(book.getLibraryStock());
        
        // Update dates & status
        existingBook.setPublishDate(book.getPublishDate());
        existingBook.setStatus(book.getStatus());
        existingBook.setFeatured(book.getFeatured());
        
        // Update images - để controller xử lý logic preserve images cũ
        if (book.getCoverImage() != null && !book.getCoverImage().isEmpty()) {
            existingBook.setCoverImage(book.getCoverImage());
        }
        if (book.getImagesJson() != null) {
            existingBook.setImagesJson(book.getImagesJson());
        }
        
        // Regenerate slug if null or title changed
        String existingSlug = existingBook.getSlug();
        String expectedSlug = slugService.toSlug(book.getTitle());
        if (existingSlug == null || !existingSlug.equals(expectedSlug)) {
            String newSlug = slugService.toUniqueSlug(book.getTitle(), 
                    slug -> !slug.equals(existingSlug) && bookRepository.existsBySlug(slug));
            existingBook.setSlug(newSlug);
        }
        
        bookRepository.save(existingBook);
        log.info("Updated book: {} with all fields", book.getId());
    }

    public void deleteBookById(Long id) {
        bookRepository.deleteById(id);
        log.info("Deleted book: {}", id);
    }

    public List<Book> searchBook(String keyword) {
        return bookRepository.searchBook(keyword);
    }

    /**
     * Lấy tất cả sách (đơn giản, không phân trang)
     * Sử dụng cho dropdown select trong form
     */
    @Transactional(readOnly = true)
    public List<Book> getAllBooksSimple() {
        return bookRepository.findAll(Sort.by(Sort.Direction.ASC, "title"));
    }

    // ==================== Advanced Search & Filter ====================

    /**
     * Tìm kiếm nâng cao với nhiều tiêu chí
     */
    @Transactional(readOnly = true)
    public Page<Book> advancedSearch(
            String keyword,
            Long categoryId,
            Long authorId,
            Long publisherId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        return bookRepository.advancedSearch(keyword, categoryId, authorId, publisherId, minPrice, maxPrice, pageable);
    }

    /**
     * Lấy sách theo danh mục
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooksByCategory(Long categoryId, Pageable pageable) {
        return bookRepository.findByCategoryId(categoryId, pageable);
    }

    /**
     * Lấy sách theo tác giả
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooksByAuthor(Long authorId, Pageable pageable) {
        return bookRepository.findByAuthorId(authorId, pageable);
    }

    /**
     * Lấy sách theo NXB
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooksByPublisher(Long publisherId, Pageable pageable) {
        return bookRepository.findByPublisherId(publisherId, pageable);
    }

    /**
     * Lấy sách theo field
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooksByField(Long fieldId, Pageable pageable) {
        return bookRepository.findByFieldId(fieldId, pageable);
    }

    /**
     * Lấy sách theo khoảng giá
     */
    @Transactional(readOnly = true)
    public Page<Book> getBooksByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return bookRepository.findByPriceBetween(minPrice, maxPrice, pageable);
    }

    // ==================== Inventory Management ====================

    /**
     * Cập nhật tồn kho
     */
    public Book updateStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));
        
        book.setStock(book.getStock() + quantity);
        
        if (book.getStock() < 0) {
            throw new IllegalStateException("Tồn kho không thể âm");
        }
        
        Book updated = bookRepository.save(book);
        log.info("Updated stock for book {}: {} (new stock: {})", bookId, quantity, updated.getStock());
        return updated;
    }

    /**
     * Cập nhật tồn kho thư viện
     */
    public Book updateLibraryStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));
        
        book.setLibraryStock(book.getLibraryStock() + quantity);
        
        if (book.getLibraryStock() < 0) {
            throw new IllegalStateException("Tồn kho thư viện không thể âm");
        }
        
        Book updated = bookRepository.save(book);
        log.info("Updated library stock for book {}: {} (new stock: {})", bookId, quantity, updated.getLibraryStock());
        return updated;
    }

    /**
     * Lấy sách có tồn kho thấp
     */
    @Transactional(readOnly = true)
    public List<Book> getLowStockBooks(int threshold) {
        return bookRepository.findByStockQuantityLessThan(threshold);
    }

    /**
     * Lấy sách có tồn kho thư viện thấp
     */
    @Transactional(readOnly = true)
    public List<Book> getLowLibraryStockBooks(int threshold) {
        return bookRepository.findLowLibraryStock(threshold);
    }

    // ==================== Statistics & Popular Books ====================

    /**
     * Lấy sách bán chạy
     */
    @Transactional(readOnly = true)
    public List<Book> getBestSellingBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "soldCount"));
        return bookRepository.findTopSelling(pageable).getContent();
    }

    /**
     * Lấy sách mới nhất
     */
    @Transactional(readOnly = true)
    public List<Book> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookRepository.findAll(pageable).getContent();
    }

    /**
     * Lấy sách xem nhiều nhất
     */
    @Transactional(readOnly = true)
    public List<Book> getMostViewedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount"));
        return bookRepository.findAll(pageable).getContent();
    }

    /**
     * Tăng view count
     */
    public void incrementViewCount(Long bookId) {
        bookRepository.incrementViewCount(bookId);
    }

    /**
     * Lấy sách liên quan
     */
    @Transactional(readOnly = true)
    public List<Book> getRelatedBooks(Long bookId, int limit) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null || book.getCategory() == null) {
            return List.of();
        }
        
        return bookRepository.findRelatedBooks(book.getCategory().getId(), bookId, PageRequest.of(0, limit));
    }

    /**
     * Lấy sách cùng tác giả
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksBySameAuthor(Long bookId, int limit) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null || book.getAuthor() == null) {
            return List.of();
        }
        
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findByAuthorIdExcluding(book.getAuthor().getId(), bookId, pageable).getContent();
    }

    // ==================== Utility Methods ====================

    /**
     * Kiểm tra sách tồn tại
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return bookRepository.existsById(id);
    }

    /**
     * Kiểm tra ISBN đã tồn tại
     */
    @Transactional(readOnly = true)
    public boolean existsByIsbn(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    /**
     * Đếm tổng số sách
     */
    @Transactional(readOnly = true)
    public long count() {
        return bookRepository.count();
    }

    /**
     * Đếm sách theo danh mục
     */
    @Transactional(readOnly = true)
    public long countByCategory(Long categoryId) {
        return bookRepository.countByCategoryId(categoryId);
    }

    // ==================== LibraryController Wrapper Methods ====================

    /**
     * Tìm sách có sẵn trong thư viện (stockQuantity > 0)
     */
    @Transactional(readOnly = true)
    public Page<Book> findAvailableForLibrary(String keyword, Long categoryId, String sortBy, Pageable pageable) {
        return bookRepository.findAvailableForLibrary(keyword, categoryId, pageable);
    }

    /**
     * Lấy tất cả danh mục
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    // ==================== AdminBookController Wrapper Methods ====================

    /**
     * Tìm kiếm sách theo từ khóa (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.advancedSearch(keyword, null, null, null, null, null, pageable);
    }

    /**
     * Lấy sách theo danh mục (alias)
     */
    @Transactional(readOnly = true)
    public Page<Book> findByCategory(Long categoryId, Pageable pageable) {
        return getBooksByCategory(categoryId, pageable);
    }

    /**
     * Lấy sách theo tác giả (alias)
     */
    @Transactional(readOnly = true)
    public Page<Book> findByAuthor(Long authorId, Pageable pageable) {
        return getBooksByAuthor(authorId, pageable);
    }

    /**
     * Lấy sách theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<Book> findByStatus(String status, Pageable pageable) {
        BookStatus bookStatus = BookStatus.valueOf(status.toUpperCase());
        return bookRepository.findByStatus(bookStatus, pageable);
    }

    /**
     * Lấy tất cả sách (phân trang đơn giản) với eager fetch để tránh N+1
     */
    @Transactional(readOnly = true)
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAllWithDetails(pageable);
    }

    /**
     * Xóa sách theo ID
     */
    public void deleteBook(Long id) {
        deleteBookById(id);
    }

    /**
     * Toggle trạng thái nổi bật của sách
     */
    public Book toggleFeatured(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));
        
        book.setFeatured(!Boolean.TRUE.equals(book.getFeatured()));
        Book updated = bookRepository.save(book);
        log.info("Toggled featured status for book {}: {}", bookId, updated.getFeatured());
        return updated;
    }

    /**
     * Lấy danh sách sách nổi bật cho trang chủ
     */
    @Transactional(readOnly = true)
    public List<Book> getFeaturedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Book> featured = bookRepository.findFeaturedBooks(pageable);
        if (featured.isEmpty()) {
            // Fallback: nếu chưa có sách nổi bật, lấy sách bán chạy nhất
            return bookRepository.findTopSelling(pageable).getContent();
        }
        return featured;
    }

    /**
     * Toggle trạng thái active của sách
     */
    public Book toggleActive(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + bookId));
        
        book.setActive(!Boolean.TRUE.equals(book.getActive()));
        Book updated = bookRepository.save(book);
        log.info("Toggled active status for book {}: {}", bookId, updated.getActive());
        return updated;
    }

    /**
     * Kích hoạt nhiều sách
     */
    public int bulkActivate(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            Optional<Book> bookOpt = bookRepository.findById(id);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                book.setActive(true);
                bookRepository.save(book);
                count++;
            }
        }
        log.info("Bulk activated {} books", count);
        return count;
    }

    /**
     * Vô hiệu hóa nhiều sách
     */
    public int bulkDeactivate(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            Optional<Book> bookOpt = bookRepository.findById(id);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                book.setActive(false);
                bookRepository.save(book);
                count++;
            }
        }
        log.info("Bulk deactivated {} books", count);
        return count;
    }

    /**
     * Nổi bật nhiều sách
     */
    public int bulkFeature(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            Optional<Book> bookOpt = bookRepository.findById(id);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                book.setFeatured(true);
                bookRepository.save(book);
                count++;
            }
        }
        log.info("Bulk featured {} books", count);
        return count;
    }

    /**
     * Bỏ nổi bật nhiều sách
     */
    public int bulkUnfeature(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            Optional<Book> bookOpt = bookRepository.findById(id);
            if (bookOpt.isPresent()) {
                Book book = bookOpt.get();
                book.setFeatured(false);
                bookRepository.save(book);
                count++;
            }
        }
        log.info("Bulk unfeatured {} books", count);
        return count;
    }

    /**
     * Xóa nhiều sách
     */
    public int bulkDelete(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            if (bookRepository.existsById(id)) {
                bookRepository.deleteById(id);
                count++;
            }
        }
        log.info("Bulk deleted {} books", count);
        return count;
    }

    /**
     * Import sách từ file CSV
     */
    public int importFromCsv(org.springframework.web.multipart.MultipartFile file) {
        // TODO: Implement CSV import logic
        // This is a placeholder - full implementation would parse CSV and create books
        log.info("Import from CSV requested, file: {}", file.getOriginalFilename());
        return 0;
    }
}
