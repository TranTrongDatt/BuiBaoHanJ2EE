package fit.hutech.BuiBaoHan.controllers;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fit.hutech.BuiBaoHan.daos.Item;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.services.AuthorService;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.CartService;
import fit.hutech.BuiBaoHan.services.CategoryService;
import fit.hutech.BuiBaoHan.services.PublisherService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final AuthorService authorService;
    private final PublisherService publisherService;

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    public String showAllBooks(@NotNull Model model,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) java.math.BigDecimal priceMin,
            @RequestParam(required = false) java.math.BigDecimal priceMax) {
        
        Page<Book> bookPage;
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(pageNo, pageSize, 
                org.springframework.data.domain.Sort.by(sortBy));
        
        // Check if any filter is applied
        boolean hasFilter = categoryId != null || priceMin != null || priceMax != null;
        
        if (hasFilter) {
            // Use advanced search for filtering
            bookPage = bookService.advancedSearch(null, categoryId, null, null, priceMin, priceMax, pageable);
            
            // Get category name for display
            if (categoryId != null) {
                categoryService.getCategoryById(categoryId).ifPresent(cat -> {
                    model.addAttribute("selectedCategoryName", cat.getName());
                });
                model.addAttribute("selectedCategoryId", categoryId);
            }
            
            // Set selected price range for UI
            if (priceMin != null || priceMax != null) {
                model.addAttribute("priceMin", priceMin);
                model.addAttribute("priceMax", priceMax);
            }
        } else {
            bookPage = bookService.getAllBooksPage(pageNo, pageSize, sortBy);
        }
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", pageNo);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("categories", categoryService.getAllCategories());
        return "book/list";
    }

    /**
     * Show book detail page by slug or ID
     * Supports both /books/{slug} and /books/{id} (numeric)
     */
    @GetMapping("/{slugOrId}")
    public String showBookDetail(@PathVariable String slugOrId, Model model) {
        java.util.Optional<Book> bookOpt;
        
        // Check if parameter is numeric (ID) or string (slug)
        if (slugOrId.matches("\\d+")) {
            bookOpt = bookService.getBookById(Long.valueOf(slugOrId));
        } else {
            bookOpt = bookService.getBookBySlug(slugOrId);
        }
        
        return bookOpt
                .map(book -> {
                    // Increment view count
                    bookService.incrementViewCount(book.getId());
                    model.addAttribute("book", book);
                    model.addAttribute("relatedBooks", bookService.getRelatedBooks(book.getId(), 4));
                    return "book/detail";
                })
                .orElse("redirect:/books");
    }

    /**
     * Proxy PDF từ Cloudinary - dùng chung cho cả download và view inline.
     * disposition = "attachment" → tải về, "inline" → xem trong trình duyệt/iframe.
     */
    private ResponseEntity<byte[]> proxyPdf(Book book, String disposition) {
        String pdfUrl = book.getDescriptionPdfUrl();
        if (pdfUrl == null || pdfUrl.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String filename = book.getTitle() != null
                ? book.getTitle().replaceAll("[^a-zA-Z0-9\\p{L} _-]", "")
                        .replaceAll("\\s+", "_") + ".pdf"
                : "book-description.pdf";

        try {
            byte[] pdfBytes;

            if (pdfUrl.startsWith("/uploads/")) {
                // File lưu local: đọc trực tiếp từ disk
                Path filePath = Paths.get(uploadDir).resolve(pdfUrl.substring("/uploads/".length()));
                if (!Files.exists(filePath)) {
                    log.warn("PDF file not found on disk: {}", filePath);
                    return ResponseEntity.notFound().build();
                }
                pdfBytes = Files.readAllBytes(filePath);
            } else {
                // File trên Cloudinary hoặc URL bên ngoài
                try (InputStream in = URI.create(pdfUrl).toURL().openStream()) {
                    pdfBytes = in.readAllBytes();
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Failed to serve PDF for book [{}]: {} - URL: {}", book.getId(), e.getMessage(), pdfUrl);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download PDF mô tả sách (Content-Disposition: attachment)
     */
    @GetMapping("/{id}/download-pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(book -> proxyPdf(book, "attachment"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Xem PDF inline trong iframe/trình duyệt (Content-Disposition: inline)
     */
    @GetMapping("/{id}/view-pdf")
    public ResponseEntity<byte[]> viewPdf(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(book -> proxyPdf(book, "inline"))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/add")
    public String addBookForm(@NotNull Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.findAll());
        model.addAttribute("publishers", publisherService.findAll());
        return "book/add";
    }

    @PostMapping("/add")
    public String addBook(
            @Valid @ModelAttribute("book") Book book,
            @NotNull BindingResult bindingResult,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) Long categoryId,
            Model model) {
        
        // Lookup entities by ID and set to book
        if (authorId != null) {
            authorService.getAuthorById(authorId).ifPresent(book::setAuthor);
        }
        if (publisherId != null) {
            publisherService.getPublisherById(publisherId).ifPresent(book::setPublisher);
        }
        if (categoryId != null) {
            categoryService.getCategoryById(categoryId).ifPresent(book::setCategory);
        }
        
        // Validate required fields
        if (book.getAuthor() == null) {
            bindingResult.rejectValue("author", "NotNull", "Vui lòng chọn tác giả");
        }
        if (book.getCategory() == null) {
            bindingResult.rejectValue("category", "NotNull", "Vui lòng chọn danh mục");
        }
        
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("authors", authorService.findAll());
            model.addAttribute("publishers", publisherService.findAll());
            return "book/add";
        }
        
        bookService.addBook(book);
        return "redirect:/books";
    }

    @PostMapping("/add-to-cart")
    public String addToCart(HttpSession session,
            @RequestParam long id,
            @RequestParam String name,
            @RequestParam double price,
            @RequestParam(defaultValue = "1") int quantity) {
        var cart = cartService.getCart(session);
        cart.addItems(new Item(id, name, price, quantity));
        cartService.updateCart(session, cart);
        return "redirect:/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable long id) {
        bookService.getBookById(id)
                .ifPresentOrElse(
                        book -> bookService.deleteBookById(id),
                        () -> { throw new IllegalArgumentException("Book not found"); });
        return "redirect:/books";
    }

    /**
     * Fallback for /books/edit without ID - redirect to book list
     */
    @GetMapping("/edit")
    public String editBookFallback() {
        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@NotNull Model model, @PathVariable long id) {
        var book = bookService.getBookById(id);
        model.addAttribute("book", book.orElseThrow(() -> new IllegalArgumentException("Book not found")));
        model.addAttribute("categories",
                categoryService.getAllCategories());
        return "book/edit";
    }

    @PostMapping("/edit")
    public String editBook(@Valid @ModelAttribute("book") Book book,
            @NotNull BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            var errors = bindingResult.getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toArray(String[]::new);
            model.addAttribute("errors", errors);
            model.addAttribute("categories",
                    categoryService.getAllCategories());
            return "book/edit";
        }
        bookService.updateBook(book);
        return "redirect:/books";
    }

    @GetMapping("/search")
    public String searchBook(
            @NotNull Model model,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "id") String sortBy) {
        var searchResults = bookService.searchBook(keyword);
        model.addAttribute("books", searchResults);
        model.addAttribute("currentPage", pageNo);
        // Tính totalPages dựa trên kết quả tìm kiếm (không trừ 1, template sẽ xử lý)
        int totalPages = (int) Math.ceil((double) searchResults.size() / pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("categories",
                categoryService.getAllCategories());
        model.addAttribute("keyword", keyword);
        return "book/list";
    }
}
