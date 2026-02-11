package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.BorrowRequest;
import fit.hutech.BuiBaoHan.dto.LibraryCardDto;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.dto.ReturnRequest;
import fit.hutech.BuiBaoHan.entities.BorrowRecord;
import fit.hutech.BuiBaoHan.entities.Fine;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.BorrowService;
import fit.hutech.BuiBaoHan.services.FineService;
import fit.hutech.BuiBaoHan.services.LibraryCardService;
import fit.hutech.BuiBaoHan.services.LibraryReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Library management
 */
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class ApiLibraryController {

    private final LibraryCardService libraryCardService;
    private final BorrowService borrowService;
    private final FineService fineService;
    private final LibraryReportService libraryReportService;
    private final AuthResolver authResolver;

    // ==================== Library Card Endpoints ====================

    /**
     * Get current user's library card
     */
    @GetMapping("/my-card")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LibraryCardDto>> getMyCard(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        return libraryCardService.findByUser(user)
                .map(card -> ResponseEntity.ok(ApiResponse.success(LibraryCardDto.from(card))))
                .orElse(ResponseEntity.ok(ApiResponse.success("No library card found", null)));
    }

    /**
     * Register for a new library card
     */
    @PostMapping("/cards/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LibraryCardDto>> registerCard(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        try {
            LibraryCard card = libraryCardService.createCard(user);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(LibraryCardDto.from(card)));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Renew library card
     */
    @PostMapping("/cards/{cardId}/renew")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN') or @libraryCardService.isCardOwner(#cardId, principal)")
    public ResponseEntity<ApiResponse<LibraryCardDto>> renewCard(@PathVariable Long cardId) {
        try {
            LibraryCard card = libraryCardService.renewCard(cardId);
            return ResponseEntity.ok(ApiResponse.success("Card renewed successfully", LibraryCardDto.from(card)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Admin Card Endpoints ====================

    /**
     * Get all library cards (Admin/Librarian)
     */
    @GetMapping("/cards")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<PageResponse<LibraryCardDto>>> getAllCards(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        var page = (status != null) 
                ? libraryCardService.findByStatus(status, pageable)
                : libraryCardService.findAll(pageable);
        
        List<LibraryCardDto> dtos = page.getContent().stream()
                .map(LibraryCardDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page, dtos)));
    }

    /**
     * Get card by ID (Admin/Librarian)
     */
    @GetMapping("/cards/{cardId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LibraryCardDto>> getCardById(@PathVariable Long cardId) {
        return libraryCardService.findById(cardId)
                .map(card -> ResponseEntity.ok(ApiResponse.success(LibraryCardDto.from(card))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Library card")));
    }

    /**
     * Suspend library card (Admin/Librarian)
     */
    @PutMapping("/cards/{cardId}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LibraryCardDto>> suspendCard(
            @PathVariable Long cardId,
            @RequestParam(required = false) String reason) {
        try {
            LibraryCard card = libraryCardService.suspendCard(cardId, reason);
            return ResponseEntity.ok(ApiResponse.success("Card suspended", LibraryCardDto.from(card)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Activate library card (Admin/Librarian)
     */
    @PutMapping("/cards/{cardId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<LibraryCardDto>> activateCard(@PathVariable Long cardId) {
        try {
            LibraryCard card = libraryCardService.activateCard(cardId);
            return ResponseEntity.ok(ApiResponse.success("Card activated", LibraryCardDto.from(card)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Borrow Endpoints ====================

    /**
     * Get my borrow records
     */
    @GetMapping("/my-borrows")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BorrowRecordDto>>> getMyBorrows(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) String status) {
        User user = authResolver.resolveUser(principal);
        
        List<BorrowRecord> records = (status != null)
                ? borrowService.findByUserAndStatus(user, status)
                : borrowService.findByUser(user);
        
        List<BorrowRecordDto> dtos = records.stream()
                .map(BorrowRecordDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Borrow a book
     */
    @PostMapping("/borrow")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> borrowBook(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody BorrowRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            BorrowRecord record = borrowService.borrowBook(user, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(BorrowRecordDto.from(record)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Return a book
     */
    @PostMapping("/return")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> returnBook(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody ReturnRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            BorrowRecord record = borrowService.returnBook(user, request);
            return ResponseEntity.ok(ApiResponse.success("Book returned successfully", BorrowRecordDto.from(record)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Extend borrow period
     */
    @PostMapping("/borrows/{borrowId}/extend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> extendBorrow(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long borrowId) {
        User user = authResolver.resolveUser(principal);
        try {
            BorrowRecord record = borrowService.extendBorrow(borrowId, user);
            return ResponseEntity.ok(ApiResponse.success("Borrow period extended", BorrowRecordDto.from(record)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Admin Borrow Endpoints ====================

    /**
     * Get all borrow records (Admin/Librarian)
     */
    @GetMapping("/borrows")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<PageResponse<BorrowRecordDto>>> getAllBorrows(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String status) {
        
        var page = (status != null)
                ? borrowService.findByStatus(status, pageable)
                : borrowService.findAll(pageable);
        
        List<BorrowRecordDto> dtos = page.getContent().stream()
                .map(BorrowRecordDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page, dtos)));
    }

    /**
     * Get overdue borrows (Admin/Librarian)
     */
    @GetMapping("/borrows/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<BorrowRecordDto>>> getOverdueBorrows() {
        List<BorrowRecordDto> dtos = borrowService.findOverdue().stream()
                .map(BorrowRecordDto::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Process return by librarian (Admin/Librarian)
     */
    @PostMapping("/borrows/{borrowId}/process-return")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<BorrowRecordDto>> processReturn(
            @PathVariable Long borrowId,
            @RequestParam(required = false) String condition) {
        try {
            BorrowRecord record = borrowService.processReturn(borrowId, condition);
            return ResponseEntity.ok(ApiResponse.success("Return processed", BorrowRecordDto.from(record)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Fine Endpoints ====================

    /**
     * Get my fines
     */
    @GetMapping("/my-fines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<FineDto>>> getMyFines(
            @AuthenticationPrincipal Object principal,
            @RequestParam(required = false) Boolean unpaidOnly) {
        User user = authResolver.resolveUser(principal);
        
        List<Fine> fines = (unpaidOnly != null && unpaidOnly)
                ? fineService.findUnpaidByUser(user)
                : fineService.findByUser(user);
        
        List<FineDto> dtos = fines.stream()
                .map(FineDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    /**
     * Pay fine
     */
    @PostMapping("/fines/{fineId}/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FineDto>> payFine(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long fineId,
            @RequestParam String paymentMethod) {
        User user = authResolver.resolveUser(principal);
        try {
            Fine fine = fineService.payFine(fineId, user, paymentMethod);
            return ResponseEntity.ok(ApiResponse.success("Fine paid successfully", FineDto.from(fine)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all fines (Admin/Librarian)
     */
    @GetMapping("/fines")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<PageResponse<FineDto>>> getAllFines(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) Boolean unpaidOnly) {
        
        var page = (unpaidOnly != null && unpaidOnly)
                ? fineService.findUnpaid(pageable)
                : fineService.findAll(pageable);
        
        List<FineDto> dtos = page.getContent().stream()
                .map(FineDto::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page, dtos)));
    }

    /**
     * Waive fine (Admin only)
     */
    @PostMapping("/fines/{fineId}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FineDto>> waiveFine(
            @PathVariable Long fineId,
            @RequestParam String reason) {
        try {
            Fine fine = fineService.waiveFine(fineId, reason);
            return ResponseEntity.ok(ApiResponse.success("Fine waived", FineDto.from(fine)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Reports Endpoints ====================

    /**
     * Get library statistics (Admin/Librarian)
     */
    @GetMapping("/reports/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLibraryStats() {
        Map<String, Object> stats = libraryReportService.getOverviewStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get popular books report (Admin/Librarian)
     */
    @GetMapping("/reports/popular-books")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPopularBooksReport(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> report = libraryReportService.getPopularBooks(limit);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * Get active members report (Admin/Librarian)
     */
    @GetMapping("/reports/active-members")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveMembersReport(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> report = libraryReportService.getActiveMembers(limit);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    // ==================== Inner DTO Records ====================

    public record BorrowRecordDto(
            Long id,
            String bookTitle,
            String bookCoverImage,
            String borrowDate,
            String dueDate,
            String returnDate,
            String status,
            Integer extensions,
            String condition
    ) {
        public static BorrowRecordDto from(BorrowRecord record) {
            return new BorrowRecordDto(
                    record.getId(),
                    record.getBook().getTitle(),
                    record.getBook().getCoverImage(),
                    record.getBorrowDate().toString(),
                    record.getDueDate().toString(),
                    record.getReturnDate() != null ? record.getReturnDate().toString() : null,
                    record.getStatus().name(),
                    record.getExtensions(),
                    record.getCondition() != null ? record.getCondition().name() : null
            );
        }
    }

    public record FineDto(
            Long id,
            Long borrowRecordId,
            String bookTitle,
            String fineType,
            java.math.BigDecimal amount,
            String reason,
            String status,
            String paidAt,
            String paymentMethod
    ) {
        public static FineDto from(Fine fine) {
            return new FineDto(
                    fine.getId(),
                    fine.getBorrowRecord().getId(),
                    fine.getBorrowRecord().getBook().getTitle(),
                    fine.getFineType().name(),
                    fine.getAmount(),
                    fine.getReason(),
                    fine.getStatus().name(),
                    fine.getPaidAt() != null ? fine.getPaidAt().toString() : null,
                    fine.getPaymentMethod()
            );
        }
    }
}
