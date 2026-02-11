package fit.hutech.BuiBaoHan.controllers;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
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

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final CartService cartService;
    private final AuthorService authorService;
    private final PublisherService publisherService;

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
            bookOpt = bookService.getBookById(Long.parseLong(slugOrId));
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
