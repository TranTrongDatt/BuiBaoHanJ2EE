package fit.hutech.BuiBaoHan.controllers.admin;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.constants.BookStatus;
import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.services.AuthorService;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.CategoryService;
import fit.hutech.BuiBaoHan.services.FileStorageService;
import fit.hutech.BuiBaoHan.services.PublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin Book Management Controller
 */
@Controller
@RequestMapping("/admin/books")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminBookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;
    private final PublisherService publisherService;
    private final FileStorageService fileStorageService;

    /**
     * Configure date binding for LocalDate fields
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    // Try multiple date formats
                    try {
                        setValue(LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE)); // yyyy-MM-dd
                    } catch (Exception e1) {
                        try {
                            setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        } catch (Exception e2) {
                            setValue(null);
                        }
                    }
                }
            }
            
            @Override
            public String getAsText() {
                LocalDate value = (LocalDate) getValue();
                return value != null ? value.format(DateTimeFormatter.ISO_LOCAL_DATE) : "";
            }
        });
    }

    /**
     * List all books
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String listBooks(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String status) {
        
        Page<Book> books;
        
        if (search != null && !search.isEmpty()) {
            books = bookService.searchBooks(search, pageable);
        } else if (categoryId != null) {
            books = bookService.findByCategory(categoryId, pageable);
        } else if (authorId != null) {
            books = bookService.findByAuthor(authorId, pageable);
        } else if (status != null) {
            books = bookService.findByStatus(status, pageable);
        } else {
            books = bookService.getAllBooks(pageable);
        }
        
        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.findAll());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("authorId", authorId);
        model.addAttribute("status", status);
        
        return "admin/books/list";
    }

    /**
     * Show add book form
     */
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("book", new Book());
        populateFormData(model);
        return "admin/books/form";
    }

    /**
     * Show edit book form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    populateFormData(model);
                    return "admin/books/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Book not found");
                    return "redirect:/admin/books";
                });
    }

    /**
     * Save book (create or update)
     */
    @PostMapping("/save")
    public String saveBook(
            @Valid @ModelAttribute("book") Book book,
            BindingResult result,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) String publisherId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) MultipartFile coverImageFile,
            @RequestParam(required = false) List<MultipartFile> additionalImages,
            @RequestParam(required = false) MultipartFile videoFile,
            @RequestParam(required = false) MultipartFile descriptionPdfFile,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.info("Received form submission - authorId: '{}', publisherId: '{}', categoryId: '{}'", 
                authorId, publisherId, categoryId);
        
        // Convert empty strings to null and parse to Long
        Long authorIdLong = parseIdFromString(authorId);
        Long publisherIdLong = parseIdFromString(publisherId);
        Long categoryIdLong = parseIdFromString(categoryId);
        
        log.info("Parsed IDs - authorIdLong: {}, publisherIdLong: {}, categoryIdLong: {}", 
                authorIdLong, publisherIdLong, categoryIdLong);
        
        // First, validate required foreign key IDs before checking binding errors
        if (authorIdLong == null) {
            result.rejectValue("author", "NotNull", "Vui lòng chọn tác giả");
            log.warn("Author ID is null - validation failed");
        }
        if (publisherIdLong == null) {
            result.rejectValue("publisher", "NotNull", "Nhà xuất bản không được để trống");
            log.warn("Publisher ID is null - validation failed");
        }
        
        if (result.hasErrors()) {
            log.error("Form validation failed with {} errors", result.getErrorCount());
            result.getAllErrors().forEach(error -> log.error("Validation error: {}", error));
            
            // Preserve submitted IDs so form can show them after validation error
            model.addAttribute("submittedAuthorId", authorIdLong);
            model.addAttribute("submittedPublisherId", publisherIdLong);
            model.addAttribute("submittedCategoryId", categoryIdLong);
            
            populateFormData(model);
            return "admin/books/form";
        }
        
        // Lookup entities by ID and set to book
        var authorOpt = authorService.getAuthorById(authorIdLong);
        if (authorOpt.isEmpty()) {
            result.rejectValue("author", "NotFound", "Tác giả không tồn tại");
            log.error("Author not found with ID: {}", authorIdLong);
            
            model.addAttribute("submittedAuthorId", authorIdLong);
            model.addAttribute("submittedPublisherId", publisherIdLong);
            model.addAttribute("submittedCategoryId", categoryIdLong);
            
            populateFormData(model);
            return "admin/books/form";
        }
        book.setAuthor(authorOpt.get());
        log.info("Set author: {} (ID: {})", authorOpt.get().getName(), authorIdLong);
        
        var publisherOpt = publisherService.getPublisherById(publisherIdLong);
        if (publisherOpt.isEmpty()) {
            result.rejectValue("publisher", "NotFound", "Nhà xuất bản không tồn tại");
            log.error("Publisher not found with ID: {}", publisherIdLong);
            
            model.addAttribute("submittedAuthorId", authorIdLong);
            model.addAttribute("submittedPublisherId", publisherIdLong);
            model.addAttribute("submittedCategoryId", categoryIdLong);
            
            populateFormData(model);
            return "admin/books/form";
        }
        book.setPublisher(publisherOpt.get());
        log.info("Set publisher: {} (ID: {})", publisherOpt.get().getName(), publisherIdLong);
        
        if (categoryIdLong != null) {
            var categoryOpt = categoryService.getCategoryById(categoryIdLong);
            if (categoryOpt.isPresent()) {
                book.setCategory(categoryOpt.get());
                log.info("Set category: {} (ID: {})", categoryOpt.get().getName(), categoryIdLong);
            }
        }
        
        // Set default values for fields that might be null from form binding
        if (book.getViewCount() == null) book.setViewCount(0L);
        if (book.getSoldCount() == null) book.setSoldCount(0L);
        if (book.getTotalQuantity() == null) book.setTotalQuantity(0);
        if (book.getStockQuantity() == null) book.setStockQuantity(0);
        if (book.getLibraryStock() == null) book.setLibraryStock(0);
        if (book.getStatus() == null) book.setStatus(BookStatus.AVAILABLE);
        if (book.getFeatured() == null) book.setFeatured(false);
        if (book.getLanguage() == null || book.getLanguage().isEmpty()) book.setLanguage("Tiếng Việt");
        
        // Convert empty ISBN to null to avoid UNIQUE constraint violation
        if (book.getIsbn() != null && book.getIsbn().isBlank()) book.setIsbn(null);
        
        // Debug logging
        log.info("Saving book: title={}, authorId={}, publisherId={}, categoryId={}", 
                book.getTitle(), authorIdLong, publisherIdLong, categoryIdLong);
        log.info("Book author: {}, publisher: {}, category: {}", 
                book.getAuthor() != null ? book.getAuthor().getName() : "null",
                book.getPublisher() != null ? book.getPublisher().getName() : "null",
                book.getCategory() != null ? book.getCategory().getName() : "null");
        
        try {
            // Handle cover image upload
            if (coverImageFile != null && !coverImageFile.isEmpty()) {
                String coverPath = fileStorageService.storeImage(coverImageFile, "books");
                book.setCoverImage(coverPath);
            }
            
            // Handle additional images
            if (additionalImages != null && !additionalImages.isEmpty()) {
                List<String> imagePaths = new java.util.ArrayList<>();
                for (MultipartFile f : additionalImages) {
                    if (!f.isEmpty()) {
                        imagePaths.add(fileStorageService.storeImage(f, "books"));
                    }
                }
                book.setImages(imagePaths);
            }
            
            // Handle video upload
            if (videoFile != null && !videoFile.isEmpty()) {
                String oldVideoUrl = book.getVideoUrl();
                String videoUrl = fileStorageService.storeVideo(videoFile);
                book.setVideoUrl(videoUrl);
                fileStorageService.deleteOldFileIfNeeded(oldVideoUrl, videoUrl);
                log.info("Uploaded video for book: {}", videoUrl);
            }
            
            // Handle description PDF upload
            if (descriptionPdfFile != null && !descriptionPdfFile.isEmpty()) {
                String oldPdfUrl = book.getDescriptionPdfUrl();
                String pdfUrl = fileStorageService.storeDescriptionPdf(descriptionPdfFile);
                book.setDescriptionPdfUrl(pdfUrl);
                fileStorageService.deleteOldFileIfNeeded(oldPdfUrl, pdfUrl);
                log.info("Uploaded description PDF for book: {}", pdfUrl);
            }
            
            if (book.getId() == null) {
                bookService.addBook(book);
                redirectAttributes.addFlashAttribute("success", "Book created successfully");
            } else {
                bookService.updateBook(book);
                redirectAttributes.addFlashAttribute("success", "Book updated successfully");
            }
            
            return "redirect:/admin/books";
        } catch (java.io.IOException | RuntimeException e) {
            log.error("Error saving book", e);
            model.addAttribute("error", "Có lỗi xảy ra khi lưu sách: " + e.getMessage());
            
            // Preserve submitted IDs on error
            model.addAttribute("submittedAuthorId", authorIdLong);
            model.addAttribute("submittedPublisherId", publisherIdLong);
            model.addAttribute("submittedCategoryId", categoryIdLong);
            
            populateFormData(model);
            return "admin/books/form";
        }
    }
    
    /**
     * Helper method to parse ID from string, converting empty/null/blank to null
     */
    @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
    private Long parseIdFromString(String idStr) {
        if (idStr == null) {
            return null;
        }
        String trimmed = idStr.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse ID from string: '{}'", idStr);
            return null;
        }
    }

    /**
     * Delete book
     */
    @PostMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Book deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting book: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    /**
     * Update book stock
     */
    @PostMapping("/{id}/stock")
    @ResponseBody
    public ApiResponse<Book> updateStock(
            @PathVariable Long id,
            @RequestParam int stock,
            @RequestParam(defaultValue = "0") int libraryStock) {
        try {
            Book book = bookService.updateStock(id, stock);
            if (libraryStock > 0) {
                book = bookService.updateLibraryStock(id, libraryStock);
            }
            return ApiResponse.success("Stock updated", book);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Toggle book featured status
     */
    @PostMapping("/{id}/toggle-featured")
    @ResponseBody
    public ApiResponse<Book> toggleFeatured(@PathVariable Long id) {
        try {
            Book book = bookService.toggleFeatured(id);
            return ApiResponse.success(book.getFeatured() ? "Book featured" : "Book unfeatured", book);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Toggle book active status
     */
    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ApiResponse<Book> toggleStatus(@PathVariable Long id) {
        try {
            Book book = bookService.toggleActive(id);
            return ApiResponse.success(book.getActive() ? "Book activated" : "Book deactivated", book);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Bulk actions
     */
    @PostMapping("/bulk-action")
    @ResponseBody
    public ApiResponse<Integer> bulkAction(
            @RequestParam List<Long> ids,
            @RequestParam String action) {
        try {
            int count = switch (action) {
                case "activate" -> bookService.bulkActivate(ids);
                case "deactivate" -> bookService.bulkDeactivate(ids);
                case "feature" -> bookService.bulkFeature(ids);
                case "unfeature" -> bookService.bulkUnfeature(ids);
                case "delete" -> bookService.bulkDelete(ids);
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            };
            return ApiResponse.success("Action completed for " + count + " books", count);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Import books from CSV
     */
    @PostMapping("/import")
    public String importBooks(
            @RequestParam MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            int count = bookService.importFromCsv(file);
            redirectAttributes.addFlashAttribute("success", "Imported " + count + " books");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    private void populateFormData(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.findAll());
        model.addAttribute("publishers", publisherService.findAll());
    }

    /**
     * Xem chi tiết sách
     */
    @GetMapping("/{id}")
    public String viewBook(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return bookService.getBookById(id)
                .map(book -> {
                    model.addAttribute("book", book);
                    model.addAttribute("currentPage", "books");
                    model.addAttribute("pageTitle", "Chi tiết Sách");
                    return "admin/books/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy sách");
                    return "redirect:/admin/books";
                });
    }
}
