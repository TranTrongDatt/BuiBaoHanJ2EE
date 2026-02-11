package fit.hutech.BuiBaoHan.controllers.admin;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.LibraryCardDto;
import fit.hutech.BuiBaoHan.entities.BorrowRecord;
import fit.hutech.BuiBaoHan.entities.Fine;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.services.BorrowService;
import fit.hutech.BuiBaoHan.services.FineService;
import fit.hutech.BuiBaoHan.services.LibraryCardService;
import fit.hutech.BuiBaoHan.services.LibraryReportService;
import lombok.RequiredArgsConstructor;

/**
 * Admin Library Management Controller
 */
@Controller
@RequestMapping("/admin/library")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STAFF')")
@RequiredArgsConstructor
public class AdminLibraryController {

    private final LibraryCardService libraryCardService;
    private final BorrowService borrowService;
    private final FineService fineService;
    private final LibraryReportService libraryReportService;

    /**
     * Library dashboard
     */
    @GetMapping
    public String libraryDashboard(Model model) {
        // Stats
        model.addAttribute("totalCards", libraryCardService.countAll());
        model.addAttribute("activeCards", libraryCardService.countActive());
        model.addAttribute("currentlyBorrowed", borrowService.countCurrentlyBorrowed());
        model.addAttribute("overdueRecords", borrowService.countOverdue());
        model.addAttribute("pendingFines", fineService.countPending());
        model.addAttribute("totalUnpaidFines", fineService.getTotalUnpaid());
        
        // Recent activity
        model.addAttribute("recentBorrows", borrowService.findRecent(5));
        model.addAttribute("recentReturns", borrowService.findRecentReturns(5));
        model.addAttribute("overdueList", borrowService.findOverdue());
        
        return "admin/library";
    }

    // ==================== Library Cards ====================

    /**
     * List library cards
     */
    @GetMapping("/cards")
    public String listCards(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        Page<LibraryCard> cards = libraryCardService.findAll(search, status, pageable);
        
        model.addAttribute("cards", cards);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        
        return "admin/library/cards";
    }

    /**
     * View card details
     */
    @GetMapping("/cards/{id}")
    public String viewCard(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return libraryCardService.findById(id)
                .map(card -> {
                    model.addAttribute("card", card);
                    model.addAttribute("borrowHistory", borrowService.findByCard(card));
                    model.addAttribute("activeBorrows", borrowService.findActiveByCard(card));
                    model.addAttribute("fines", fineService.findByCard(card));
                    return "admin/library/card-detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Card not found");
                    return "redirect:/admin/library/cards";
                });
    }

    /**
     * Activate card
     */
    @PostMapping("/cards/{id}/activate")
    @ResponseBody
    public ApiResponse<LibraryCardDto> activateCard(@PathVariable Long id) {
        try {
            LibraryCard card = libraryCardService.activateCard(id);
            return ApiResponse.success("Card activated", LibraryCardDto.from(card));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Suspend card
     */
    @PostMapping("/cards/{id}/suspend")
    @ResponseBody
    public ApiResponse<LibraryCardDto> suspendCard(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            LibraryCard card = libraryCardService.suspendCard(id, reason);
            return ApiResponse.success("Card suspended", LibraryCardDto.from(card));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Renew card
     */
    @PostMapping("/cards/{id}/renew")
    @ResponseBody
    public ApiResponse<LibraryCardDto> renewCard(@PathVariable Long id) {
        try {
            LibraryCard card = libraryCardService.renewCard(id);
            return ApiResponse.success("Card renewed", LibraryCardDto.from(card));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== Borrow Records ====================

    /**
     * List borrow records
     */
    @GetMapping("/borrows")
    public String listBorrows(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        
        Page<BorrowRecord> borrows = borrowService.findAll(search, status, pageable);
        
        model.addAttribute("borrows", borrows);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        
        return "admin/library/borrows";
    }

    /**
     * Process return
     */
    @PostMapping("/borrows/{id}/return")
    @ResponseBody
    public ApiResponse<BorrowRecordResponse> processReturn(
            @PathVariable Long id,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String notes) {
        try {
            BorrowRecord record = borrowService.processReturn(id, condition);
            if (notes != null) {
                borrowService.addNotes(id, notes);
            }
            return ApiResponse.success("Return processed", BorrowRecordResponse.from(record));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Extend borrow period
     */
    @PostMapping("/borrows/{id}/extend")
    @ResponseBody
    public ApiResponse<BorrowRecordResponse> extendBorrow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "7") int days) {
        try {
            BorrowRecord record = borrowService.adminExtend(id, days);
            return ApiResponse.success("Borrow extended by " + days + " days", BorrowRecordResponse.from(record));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Mark as lost
     */
    @PostMapping("/borrows/{id}/lost")
    @ResponseBody
    public ApiResponse<BorrowRecordResponse> markAsLost(@PathVariable Long id) {
        try {
            BorrowRecord record = borrowService.markAsLost(id);
            return ApiResponse.success("Marked as lost", BorrowRecordResponse.from(record));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== Fines ====================

    /**
     * List fines
     */
    @GetMapping("/fines")
    public String listFines(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        Page<Fine> fines = fineService.findAll(status, pageable);
        
        model.addAttribute("fines", fines);
        model.addAttribute("status", status);
        model.addAttribute("totalUnpaid", fineService.getTotalUnpaid());
        
        return "admin/library/fines";
    }

    /**
     * Mark fine as paid
     */
    @PostMapping("/fines/{id}/pay")
    @ResponseBody
    public ApiResponse<FineResponse> markAsPaid(
            @PathVariable Long id,
            @RequestParam String paymentMethod) {
        try {
            Fine fine = fineService.adminMarkPaid(id, paymentMethod);
            return ApiResponse.success("Fine marked as paid", FineResponse.from(fine));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Waive fine
     */
    @PostMapping("/fines/{id}/waive")
    @ResponseBody
    public ApiResponse<FineResponse> waiveFine(
            @PathVariable Long id,
            @RequestParam String reason) {
        try {
            Fine fine = fineService.waiveFine(id, reason);
            return ApiResponse.success("Fine waived", FineResponse.from(fine));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Create manual fine
     */
    @PostMapping("/fines/create")
    @ResponseBody
    public ApiResponse<FineResponse> createFine(
            @RequestParam Long borrowRecordId,
            @RequestParam String fineType,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        try {
            Fine fine = fineService.createManualFine(borrowRecordId, fineType, amount, reason);
            return ApiResponse.success("Fine created", FineResponse.from(fine));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== Reports ====================

    /**
     * Library reports page
     */
    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("overviewStats", libraryReportService.getOverviewStats());
        model.addAttribute("popularBooks", libraryReportService.getPopularBooks(10));
        model.addAttribute("activeMembers", libraryReportService.getActiveMembers(10));
        model.addAttribute("overdueStats", libraryReportService.getOverdueStats());
        return "admin/library/reports";
    }

    // ==================== Inner Records ====================

    public record BorrowRecordResponse(
            Long id,
            String bookTitle,
            String userName,
            String cardNumber,
            String borrowDate,
            String dueDate,
            String returnDate,
            String status,
            String condition
    ) {
        public static BorrowRecordResponse from(BorrowRecord record) {
            return new BorrowRecordResponse(
                    record.getId(),
                    record.getBook().getTitle(),
                    record.getUser().getFullName(),
                    record.getLibraryCard().getCardNumber(),
                    record.getBorrowDate().toString(),
                    record.getDueDate().toString(),
                    record.getReturnDate() != null ? record.getReturnDate().toString() : null,
                    record.getStatus().name(),
                    record.getCondition() != null ? record.getCondition().name() : null
            );
        }
    }

    public record FineResponse(
            Long id,
            String bookTitle,
            String userName,
            String fineType,
            BigDecimal amount,
            String status,
            String reason
    ) {
        public static FineResponse from(Fine fine) {
            return new FineResponse(
                    fine.getId(),
                    fine.getBorrowRecord().getBook().getTitle(),
                    fine.getBorrowRecord().getUser().getFullName(),
                    fine.getFineType().name(),
                    fine.getAmount(),
                    fine.getStatus().name(),
                    fine.getReason()
            );
        }
    }
}
