package fit.hutech.BuiBaoHan.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.entities.Publisher;
import fit.hutech.BuiBaoHan.repositories.IAuthorRepository;
import fit.hutech.BuiBaoHan.repositories.IPublisherRepository;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.CategoryService;
import fit.hutech.BuiBaoHan.viewmodels.BookGetVm;
import fit.hutech.BuiBaoHan.viewmodels.BookPostVm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ApiController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final IAuthorRepository authorRepository;
    private final IPublisherRepository publisherRepository;

    @GetMapping("/books")
    public ResponseEntity<List<BookGetVm>> getAllBooks(Integer pageNo,
            Integer pageSize, String sortBy) {
        return ResponseEntity.ok(bookService.getAllBooks(
                pageNo == null ? 0 : pageNo,
                pageSize == null ? 20 : pageSize,
                sortBy == null ? "id" : sortBy)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }

    @GetMapping("/books/id/{id}")
    public ResponseEntity<BookGetVm> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id)
                .map(BookGetVm::from)
                .orElse(null));
    }

    @GetMapping("/books/search")
    public ResponseEntity<List<BookGetVm>> searchBooks(String keyword) {
        return ResponseEntity.ok(bookService.searchBook(keyword)
                .stream()
                .map(BookGetVm::from)
                .toList());
    }

    /**
     * POST /api/v1/books - Thêm sách mới (chỉ ADMIN)
     */
    @PostMapping("/books")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookGetVm> addBook(@Valid @RequestBody BookPostVm bookVm) {
        // Tìm category, author, publisher theo ID
        Category category = bookVm.categoryId() != null ?
                categoryService.getCategoryById(bookVm.categoryId()).orElse(null) : null;
        Author author = bookVm.authorId() != null ?
                authorRepository.findById(bookVm.authorId()).orElse(null) : null;
        Publisher publisher = bookVm.publisherId() != null ?
                publisherRepository.findById(bookVm.publisherId()).orElse(null) : null;
        
        // Tạo Book entity từ ViewModel
        Book book = Book.builder()
                .title(bookVm.title())
                .author(author)
                .publisher(publisher)
                .price(bookVm.price())
                .originalPrice(bookVm.originalPrice())
                .isbn(bookVm.isbn())
                .coverImage(bookVm.coverImage())
                .description(bookVm.description())
                .category(category)
                .build();
        
        bookService.addBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookGetVm.from(book));
    }

    /**
     * PUT /api/v1/books/{id} - Cập nhật sách (chỉ ADMIN)
     */
    @PutMapping("/books/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookGetVm> updateBook(@PathVariable Long id, 
                                                 @Valid @RequestBody BookPostVm bookVm) {
        return bookService.getBookById(id)
                .map(existingBook -> {
                    // Tìm category, author, publisher theo ID
                    Category category = bookVm.categoryId() != null ?
                            categoryService.getCategoryById(bookVm.categoryId()).orElse(null) : null;
                    Author author = bookVm.authorId() != null ?
                            authorRepository.findById(bookVm.authorId()).orElse(null) : null;
                    Publisher publisher = bookVm.publisherId() != null ?
                            publisherRepository.findById(bookVm.publisherId()).orElse(null) : null;
                    
                    // Cập nhật thông tin sách
                    existingBook.setTitle(bookVm.title());
                    existingBook.setAuthor(author);
                    existingBook.setPublisher(publisher);
                    existingBook.setPrice(bookVm.price());
                    existingBook.setOriginalPrice(bookVm.originalPrice());
                    existingBook.setIsbn(bookVm.isbn());
                    existingBook.setCoverImage(bookVm.coverImage());
                    existingBook.setDescription(bookVm.description());
                    existingBook.setCategory(category);
                    
                    bookService.updateBook(existingBook);
                    return ResponseEntity.ok(BookGetVm.from(existingBook));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/v1/books/{id} - Xóa sách (chỉ ADMIN)
     */
    @DeleteMapping("/books/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(book -> {
                    bookService.deleteBookById(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
