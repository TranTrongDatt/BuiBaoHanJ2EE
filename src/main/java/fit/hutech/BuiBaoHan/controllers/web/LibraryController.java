package fit.hutech.BuiBaoHan.controllers.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.BorrowRecord;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.BookService;
import fit.hutech.BuiBaoHan.services.BorrowService;
import fit.hutech.BuiBaoHan.services.FineService;
import fit.hutech.BuiBaoHan.services.LibraryCardService;
import lombok.RequiredArgsConstructor;

/**
 * Library Web Controller (for regular users)
 */
@Controller
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryCardService libraryCardService;
    private final BorrowService borrowService;
    private final FineService fineService;
    private final BookService bookService;
    private final AuthResolver authResolver;

    /**
     * Library home page - shows available books for borrowing
     */
    @GetMapping
    public String libraryHome(
            Model model,
            @PageableDefault(size = 12) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sortBy) {
        
        Page<Book> books = bookService.findAvailableForLibrary(search, categoryId, sortBy, pageable);
        
        model.addAttribute("books", books);
        model.addAttribute("categories", bookService.getAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sortBy", sortBy);
        
        return "library/index";
    }

    /**
     * My library card page
     */
    @GetMapping("/my-card")
    @PreAuthorize("isAuthenticated()")
    public String myCard(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        libraryCardService.findByUser(user).ifPresentOrElse(
                card -> {
                    model.addAttribute("card", card);
                    model.addAttribute("activeBorrows", borrowService.findActiveByUser(user));
                    model.addAttribute("unpaidFines", fineService.findUnpaidByUser(user));
                },
                () -> model.addAttribute("noCard", true)
        );
        
        return "library/card";
    }

    /**
     * Register for library card
     */
    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public String registerCard(@AuthenticationPrincipal Object principal, RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        try {
            libraryCardService.createCard(user);
            redirectAttributes.addFlashAttribute("success", "Library card created successfully!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/library/my-card";
    }

    /**
     * Renew library card
     */
    @PostMapping("/my-card/renew")
    @PreAuthorize("isAuthenticated()")
    public String renewCard(@AuthenticationPrincipal Object principal, RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        try {
            libraryCardService.findByUser(user).ifPresent(card -> 
                    libraryCardService.renewCard(card.getId()));
            redirectAttributes.addFlashAttribute("success", "Card renewed successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/library/my-card";
    }

    /**
     * Borrow book page
     */
    @GetMapping("/borrow/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public String borrowBookPage(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        // Check if user has library card
        var cardOpt = libraryCardService.findByUser(user);
        if (cardOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "You need a library card to borrow books");
            return "redirect:/library/my-card";
        }
        
        LibraryCard card = cardOpt.get();
        if (!card.isActive()) {
            redirectAttributes.addFlashAttribute("error", "Your library card is not active");
            return "redirect:/library/my-card";
        }
        
        return bookService.getBookById(bookId)
                .map(book -> {
                    if (book.getLibraryStock() <= 0) {
                        redirectAttributes.addFlashAttribute("error", "Book is not available for borrowing");
                        return "redirect:/library";
                    }
                    model.addAttribute("book", book);
                    model.addAttribute("card", card);
                    model.addAttribute("activeBorrows", borrowService.findActiveByUser(user).size());
                    model.addAttribute("maxBorrows", 5); // Max books a user can borrow
                    return "library/borrow";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Book not found");
                    return "redirect:/library";
                });
    }

    /**
     * Confirm borrow
     */
    @PostMapping("/borrow/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public String borrowBook(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long bookId,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        try {
            borrowService.borrowBook(user, bookId);
            redirectAttributes.addFlashAttribute("success", "Book borrowed successfully!");
            return "redirect:/library/my-borrows";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/library/borrow/" + bookId;
        }
    }

    /**
     * My borrows page
     */
    @GetMapping("/my-borrows")
    @PreAuthorize("isAuthenticated()")
    public String myBorrows(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String status,
            Model model) {
        User user = authResolver.resolveUser(principal);
        
        if (status != null && !status.isEmpty()) {
            model.addAttribute("borrows", borrowService.findByUserAndStatus(user, status));
        } else {
            model.addAttribute("borrows", borrowService.findByUser(user));
        }
        model.addAttribute("status", status);
        
        return "library/my-borrows";
    }

    /**
     * Borrow history page
     */
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public String borrowHistory(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {
        User user = authResolver.resolveUser(principal);
        
        Page<BorrowRecord> history = borrowService.findHistoryByUser(user, pageable);
        model.addAttribute("history", history);
        
        return "library/history";
    }

    /**
     * Return book page
     */
    @GetMapping("/return/{borrowId}")
    @PreAuthorize("isAuthenticated()")
    public String returnBookPage(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long borrowId,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        return borrowService.findByIdAndUser(borrowId, user)
                .map(record -> {
                    model.addAttribute("borrowRecord", record);
                    return "library/return";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Borrow record not found");
                    return "redirect:/library/my-borrows";
                });
    }

    /**
     * Initiate return
     */
    @PostMapping("/return/{borrowId}")
    @PreAuthorize("isAuthenticated()")
    public String returnBook(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long borrowId,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        try {
            borrowService.initiateReturn(borrowId, user);
            redirectAttributes.addFlashAttribute("success", 
                    "Return initiated. Please visit the library to complete the return.");
            return "redirect:/library/my-borrows";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/library/return/" + borrowId;
        }
    }

    /**
     * Extend borrow
     */
    @PostMapping("/extend/{borrowId}")
    @PreAuthorize("isAuthenticated()")
    public String extendBorrow(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long borrowId,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        try {
            borrowService.extendBorrow(borrowId, user);
            redirectAttributes.addFlashAttribute("success", "Borrow period extended by 7 days");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/library/my-borrows";
    }

    /**
     * My fines page
     */
    @GetMapping("/my-fines")
    @PreAuthorize("isAuthenticated()")
    public String myFines(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        model.addAttribute("unpaidFines", fineService.findUnpaidByUser(user));
        model.addAttribute("paidFines", fineService.findPaidByUser(user));
        model.addAttribute("totalUnpaid", fineService.getTotalUnpaidByUser(user));
        
        return "library/fines";
    }

    /**
     * Pay fine page
     */
    @GetMapping("/fines/{fineId}/pay")
    @PreAuthorize("isAuthenticated()")
    public String payFinePage(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long fineId,
            Model model,
            RedirectAttributes redirectAttributes) {
        User user = authResolver.resolveUser(principal);
        
        return fineService.findByIdAndUser(fineId, user)
                .map(fine -> {
                    model.addAttribute("fine", fine);
                    return "library/pay-fine";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Fine not found");
                    return "redirect:/library/my-fines";
                });
    }
}
