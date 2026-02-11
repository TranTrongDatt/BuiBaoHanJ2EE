package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.BookCondition;
import fit.hutech.BuiBaoHan.constants.BorrowStatus;
import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.dto.BorrowRequest;
import fit.hutech.BuiBaoHan.dto.ReturnRequest;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.BorrowRecord;
import fit.hutech.BuiBaoHan.entities.BorrowRecordDetail;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IBorrowRecordRepository;
import fit.hutech.BuiBaoHan.repositories.ILibraryCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Mượn/Trả sách
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BorrowService {

    private final IBorrowRecordRepository borrowRecordRepository;
    private final ILibraryCardRepository libraryCardRepository;
    private final IBookRepository bookRepository;
    private final FineService fineService;

    // Cấu hình phạt
    private static final BigDecimal LATE_FEE_PER_DAY = new BigDecimal("5000");       // 5,000 VND/ngày
    private static final BigDecimal DAMAGED_FEE_PERCENT = new BigDecimal("0.30");    // 30% giá sách
    private static final BigDecimal LOST_FEE_PERCENT = new BigDecimal("1.50");       // 150% giá sách

    /**
     * Lấy tất cả phiếu mượn (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> getAllRecords(Pageable pageable) {
        return borrowRecordRepository.findAll(pageable);
    }

    /**
     * Lấy phiếu mượn theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> getRecordsByStatus(BorrowStatus status, Pageable pageable) {
        return borrowRecordRepository.findByStatus(status, pageable);
    }

    /**
     * Tìm phiếu mượn theo ID
     */
    @Transactional(readOnly = true)
    public Optional<BorrowRecord> getRecordById(Long id) {
        return borrowRecordRepository.findByIdWithDetails(id);
    }

    /**
     * Lấy lịch sử mượn của user
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> getRecordsByUserId(Long userId, Pageable pageable) {
        return borrowRecordRepository.findByLibraryCardUserId(userId, pageable);
    }

    /**
     * Lấy phiếu mượn đang active của user
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> getActiveRecordsByUserId(Long userId) {
        return borrowRecordRepository.findActiveByUserId(userId);
    }

    /**
     * Kiểm tra user có thể mượn thêm sách không
     */
    @Transactional(readOnly = true)
    public boolean canUserBorrow(Long userId, int requestedQuantity) {
        Optional<LibraryCard> cardOpt = libraryCardRepository.findByUserId(userId);
        if (cardOpt.isEmpty()) {
            return false;
        }

        LibraryCard card = cardOpt.get();
        if (card.getStatus() != CardStatus.ACTIVE || card.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        // Đếm số sách đang mượn
        int currentlyBorrowed = borrowRecordRepository.countCurrentlyBorrowed(card.getId());
        return (currentlyBorrowed + requestedQuantity) <= card.getMaxBooksAllowed();
    }

    /**
     * Tạo phiếu mượn mới
     */
    public BorrowRecord createBorrowRecord(Long userId, BorrowRequest request) {
        // Validate thẻ
        LibraryCard card = libraryCardRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User chưa có thẻ thư viện"));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Thẻ thư viện không hoạt động");
        }

        if (card.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Thẻ thư viện đã hết hạn");
        }

        // Validate số lượng
        int totalQuantity = request.items().stream()
                .mapToInt(BorrowRequest.BookBorrowItem::quantity)
                .sum();

        if (!canUserBorrow(userId, totalQuantity)) {
            throw new IllegalStateException("Vượt quá số sách được phép mượn");
        }

        // Tạo phiếu mượn
        LocalDateTime now = LocalDateTime.now();
        LocalDate dueDate = LocalDate.now().plusDays(card.getMaxBorrowDays());

        BorrowRecord record = BorrowRecord.builder()
                .libraryCard(card)
                .borrowDate(now)
                .dueDate(dueDate)
                .status(BorrowStatus.BORROWING)
                .notes(request.notes())
                .details(new ArrayList<>())
                .build();

        // Thêm chi tiết
        for (BorrowRequest.BookBorrowItem item : request.items()) {
            Book book = bookRepository.findById(item.bookId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + item.bookId()));

            // Kiểm tra tồn kho
            if (book.getStockQuantity() < item.quantity()) {
                throw new IllegalStateException("Sách '" + book.getTitle() + "' không đủ số lượng trong thư viện");
            }

            // Trừ tồn kho
            book.setStockQuantity(book.getStockQuantity() - item.quantity());
            bookRepository.save(book);

            BorrowRecordDetail detail = BorrowRecordDetail.builder()
                    .borrowRecord(record)
                    .book(book)
                    .quantity(item.quantity())
                    .borrowCondition(java.util.Objects.requireNonNullElse(item.condition(), BookCondition.GOOD))
                    .build();

            record.getDetails().add(detail);
        }

        BorrowRecord saved = borrowRecordRepository.save(record);
        log.info("Created borrow record {} for user {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Trả sách
     */
    public BorrowRecord returnBooks(Long recordId, ReturnRequest request) {
        BorrowRecord record = borrowRecordRepository.findByIdWithDetails(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + recordId));

        if (record.getStatus() != BorrowStatus.BORROWING) {
            throw new IllegalStateException("Phiếu mượn không ở trạng thái đang mượn");
        }

        LocalDateTime returnDate = LocalDateTime.now();
        BigDecimal totalFine = BigDecimal.ZERO;
        int returnedCount = 0;

        for (ReturnRequest.BookReturnItem item : request.items()) {
            BorrowRecordDetail detail = record.getDetails().stream()
                    .filter(d -> d.getId().equals(item.detailId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết ID: " + item.detailId()));

            if (detail.getReturnDate() != null) {
                continue; // Đã trả rồi
            }

            // Cập nhật chi tiết trả
            detail.setReturnDate(returnDate);
            detail.setReturnCondition(item.returnCondition());

            // Tính phạt trễ hạn
            if (LocalDate.now().isAfter(record.getDueDate())) {
                long daysLate = ChronoUnit.DAYS.between(record.getDueDate(), LocalDate.now());
                BigDecimal lateFee = LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(daysLate * detail.getQuantity()));
                totalFine = totalFine.add(lateFee);
            }

            // Tính phạt hư hỏng/mất
            BigDecimal conditionFee = calculateConditionFee(detail.getBook(), 
                    detail.getBorrowCondition(), item.returnCondition(), detail.getQuantity());
            totalFine = totalFine.add(conditionFee);

            // Hoàn kho (nếu không mất)
            if (item.returnCondition() != BookCondition.LOST) {
                Book book = detail.getBook();
                book.setStockQuantity(book.getStockQuantity() + detail.getQuantity());
                bookRepository.save(book);
            }

            returnedCount++;
        }

        // Tạo phạt nếu có
        if (totalFine.compareTo(BigDecimal.ZERO) > 0) {
            fineService.createFine(record.getId(), totalFine, "Phạt trả sách");
        }

        // Cập nhật trạng thái phiếu
        boolean allReturned = record.getDetails().stream()
                .allMatch(d -> d.getReturnDate() != null);
        
        if (allReturned) {
            record.setStatus(BorrowStatus.RETURNED);
            record.setReturnDate(returnDate);
        }

        BorrowRecord updated = borrowRecordRepository.save(record);
        log.info("Returned {} items for record {}, fine: {}", returnedCount, recordId, totalFine);
        return updated;
    }

    /**
     * Gia hạn mượn
     */
    public BorrowRecord renewBorrow(Long recordId, int additionalDays) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + recordId));

        if (record.getStatus() != BorrowStatus.BORROWING) {
            throw new IllegalStateException("Phiếu mượn không ở trạng thái đang mượn");
        }

        // Kiểm tra đã quá hạn chưa
        if (LocalDate.now().isAfter(record.getDueDate())) {
            throw new IllegalStateException("Không thể gia hạn phiếu đã quá hạn");
        }

        // Gia hạn tối đa 2 lần, mỗi lần 7 ngày
        if (additionalDays > 14) {
            additionalDays = 14;
        }

        LocalDate newDueDate = record.getDueDate().plusDays(additionalDays);
        record.setDueDate(newDueDate);

        BorrowRecord renewed = borrowRecordRepository.save(record);
        log.info("Renewed borrow record {} to {}", recordId, newDueDate);
        return renewed;
    }

    /**
     * Đánh dấu quá hạn
     */
    public void markOverdueRecords() {
        List<BorrowRecord> overdueRecords = borrowRecordRepository.findOverdueRecords(LocalDate.now());
        
        for (BorrowRecord record : overdueRecords) {
            record.setStatus(BorrowStatus.OVERDUE);
            borrowRecordRepository.save(record);
            log.info("Marked record {} as overdue", record.getId());
        }
    }

    /**
     * Đếm phiếu quá hạn
     */
    @Transactional(readOnly = true)
    public long countOverdueRecords() {
        return borrowRecordRepository.countOverdue(LocalDate.now());
    }

    /**
     * Lấy phiếu sắp đến hạn (trong 3 ngày)
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> getDueSoonRecords() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysLater = today.plusDays(3);
        return borrowRecordRepository.findDueBetween(today, threeDaysLater);
    }

    // ==================== Private Helper Methods ====================

    private BigDecimal calculateConditionFee(Book book, BookCondition borrowCondition, 
            BookCondition returnCondition, int quantity) {
        
        if (returnCondition == BookCondition.LOST) {
            // Mất sách: phạt 150% giá sách
            return book.getPrice().multiply(LOST_FEE_PERCENT).multiply(BigDecimal.valueOf(quantity));
        }

        // So sánh tình trạng
        int borrowLevel = getConditionLevel(borrowCondition);
        int returnLevel = getConditionLevel(returnCondition);

        if (returnLevel < borrowLevel) {
            // Sách xuống cấp: phạt 30% mỗi mức giảm
            int levelDrop = borrowLevel - returnLevel;
            return book.getPrice()
                    .multiply(DAMAGED_FEE_PERCENT)
                    .multiply(BigDecimal.valueOf(levelDrop * quantity));
        }

        return BigDecimal.ZERO;
    }

    private int getConditionLevel(BookCondition condition) {
        return switch (condition) {
            case NEW -> 5;
            case LIKE_NEW -> 4;
            case GOOD -> 3;
            case FAIR -> 2;
            case POOR -> 1;
            case DAMAGED, LOST -> 0;
        };
    }

    // ==================== LibraryController Wrapper Methods ====================

    /**
     * Tìm phiếu mượn theo user và status
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findByUserAndStatus(User user, String status) {
        BorrowStatus borrowStatus = BorrowStatus.valueOf(status.toUpperCase());
        return borrowRecordRepository.findByLibraryCardUserIdAndStatus(user.getId(), borrowStatus);
    }

    /**
     * Lấy tất cả phiếu mượn của user (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> findByUser(User user, Pageable pageable) {
        return getRecordsByUserId(user.getId(), pageable);
    }

    /**
     * Lấy tất cả phiếu mượn của user (không phân trang)
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findByUser(User user) {
        return getActiveRecordsByUserId(user.getId());
    }

    /**
     * Lấy phiếu mượn đang active của user (wrapper)
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findActiveByUser(User user) {
        return getActiveRecordsByUserId(user.getId());
    }

    /**
     * Lấy lịch sử mượn của user (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> findHistoryByUser(User user, Pageable pageable) {
        return getRecordsByUserId(user.getId(), pageable);
    }

    /**
     * Tìm phiếu mượn theo ID và user
     */
    @Transactional(readOnly = true)
    public Optional<BorrowRecord> findByIdAndUser(Long id, User user) {
        return borrowRecordRepository.findByIdWithDetails(id)
                .filter(br -> br.getLibraryCard().getUser().getId().equals(user.getId()));
    }

    /**
     * Mượn một cuốn sách
     */
    public BorrowRecord borrowBook(User user, Long bookId) {
        BorrowRequest request = new BorrowRequest(
                user.getId(),
                List.of(new BorrowRequest.BookBorrowItem(bookId, 1, BookCondition.GOOD)),
                null
        );
        return createBorrowRecord(user.getId(), request);
    }

    /**
     * Khởi tạo trả sách
     */
    public BorrowRecord initiateReturn(Long recordId, User user) {
        BorrowRecord record = findByIdAndUser(recordId, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn"));
        
        // Tạo request trả sách cho tất cả sách chưa trả
        List<ReturnRequest.BookReturnItem> items = record.getDetails().stream()
                .filter(d -> d.getReturnDate() == null)
                .map(d -> new ReturnRequest.BookReturnItem(d.getId(), d.getBorrowCondition(), null))
                .toList();
        
        return returnBooks(recordId, new ReturnRequest(record.getId(), items, null));
    }

    /**
     * Gia hạn mượn sách
     */
    public BorrowRecord extendBorrow(Long recordId, User user) {
        findByIdAndUser(recordId, user)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn"));
        return renewBorrow(recordId, 7); // Gia hạn thêm 7 ngày
    }

    // ==================== ApiLibraryController Wrapper Methods ====================

    /**
     * Trả sách (wrapper nhận User + ReturnRequest)
     */
    public BorrowRecord returnBook(User user, ReturnRequest request) {
        // Lấy recordId từ request items
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Danh sách sách trả không được trống");
        }
        
        // Tìm phiếu mượn chứa chi tiết đầu tiên
        Long detailId = request.items().get(0).detailId();
        BorrowRecord record = borrowRecordRepository.findAll().stream()
                .filter(br -> br.getDetails().stream().anyMatch(d -> d.getId().equals(detailId)))
                .filter(br -> br.getLibraryCard().getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn"));
        
        return returnBooks(record.getId(), request);
    }

    /**
     * Tìm theo status (String)
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> findByStatus(String status, Pageable pageable) {
        BorrowStatus borrowStatus = BorrowStatus.valueOf(status.toUpperCase());
        return getRecordsByStatus(borrowStatus, pageable);
    }

    /**
     * Lấy tất cả phiếu mượn (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> findAll(Pageable pageable) {
        return getAllRecords(pageable);
    }

    /**
     * Lấy danh sách phiếu quá hạn
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findOverdue() {
        return borrowRecordRepository.findOverdueRecords(LocalDate.now());
    }

    /**
     * Xử lý trả sách bởi thủ thư
     */
    public BorrowRecord processReturn(Long borrowId, String condition) {
        BorrowRecord record = borrowRecordRepository.findByIdWithDetails(borrowId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + borrowId));
        
        BookCondition bookCondition = (condition != null && !condition.isEmpty()) 
                ? BookCondition.valueOf(condition.toUpperCase()) 
                : BookCondition.GOOD;
        
        // Tạo request trả sách cho tất cả sách chưa trả
        List<ReturnRequest.BookReturnItem> items = record.getDetails().stream()
                .filter(d -> d.getReturnDate() == null)
                .map(d -> new ReturnRequest.BookReturnItem(d.getId(), bookCondition, null))
                .toList();
        
        return returnBooks(borrowId, new ReturnRequest(borrowId, items, null));
    }

    /**
     * Mượn sách với BorrowRequest (wrapper)
     */
    public BorrowRecord borrowBook(User user, BorrowRequest request) {
        return createBorrowRecord(user.getId(), request);
    }

    // ==================== AdminLibraryController Methods ====================

    /**
     * Đếm số sách đang mượn
     */
    @Transactional(readOnly = true)
    public long countCurrentlyBorrowed() {
        return borrowRecordRepository.countCurrentlyBorrowed();
    }

    /**
     * Đếm số phiếu quá hạn (wrapper)
     */
    @Transactional(readOnly = true)
    public long countOverdue() {
        return countOverdueRecords();
    }

    /**
     * Lấy danh sách phiếu mượn gần đây
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findRecent(int limit) {
        return borrowRecordRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, limit, 
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "borrowDate"))
        ).getContent();
    }

    /**
     * Lấy danh sách phiếu trả gần đây
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findRecentReturns(int limit) {
        return borrowRecordRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, limit, 
                    org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "returnDate"))
        ).getContent().stream()
                .filter(br -> br.getReturnDate() != null)
                .limit(limit)
                .toList();
    }

    /**
     * Tìm phiếu mượn theo thẻ
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findByCard(LibraryCard card) {
        return borrowRecordRepository.findByLibraryCardId(card.getId());
    }

    /**
     * Tìm phiếu mượn đang active theo thẻ
     */
    @Transactional(readOnly = true)
    public List<BorrowRecord> findActiveByCard(LibraryCard card) {
        return borrowRecordRepository.findActiveByUserId(card.getUser().getId());
    }

    /**
     * Tìm phiếu mượn với tìm kiếm và lọc status
     */
    @Transactional(readOnly = true)
    public Page<BorrowRecord> findAll(String search, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return findByStatus(status, pageable);
        }
        // TODO: Thêm tìm kiếm theo search nếu cần
        return findAll(pageable);
    }

    /**
     * Thêm ghi chú cho phiếu mượn
     */
    public void addNotes(Long recordId, String notes) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + recordId));
        
        String existingNotes = record.getNotes() != null ? record.getNotes() + "\n" : "";
        record.setNotes(existingNotes + notes);
        borrowRecordRepository.save(record);
        log.info("Added notes to borrow record {}", recordId);
    }

    /**
     * Gia hạn phiếu mượn bởi admin
     */
    public BorrowRecord adminExtend(Long recordId, int days) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + recordId));
        
        if (record.getStatus() != BorrowStatus.BORROWING && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new IllegalStateException("Phiếu mượn không thể gia hạn");
        }
        
        LocalDate newDueDate = record.getDueDate().plusDays(days);
        record.setDueDate(newDueDate);
        
        // Nếu đang quá hạn và gia hạn đủ thì chuyển về BORROWING
        if (record.getStatus() == BorrowStatus.OVERDUE && newDueDate.isAfter(LocalDate.now())) {
            record.setStatus(BorrowStatus.BORROWING);
        }
        
        BorrowRecord extended = borrowRecordRepository.save(record);
        log.info("Admin extended borrow record {} by {} days", recordId, days);
        return extended;
    }

    /**
     * Đánh dấu sách bị mất
     */
    public BorrowRecord markAsLost(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findByIdWithDetails(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + recordId));
        
        if (record.getStatus() == BorrowStatus.RETURNED) {
            throw new IllegalStateException("Sách đã được trả");
        }
        
        record.setStatus(BorrowStatus.LOST);
        record.setReturnDate(LocalDateTime.now());
        
        // Tính phạt mất sách
        BigDecimal totalFine = BigDecimal.ZERO;
        for (BorrowRecordDetail detail : record.getDetails()) {
            if (detail.getReturnDate() == null) {
                detail.setReturnDate(LocalDateTime.now());
                detail.setReturnCondition(BookCondition.LOST);
                totalFine = totalFine.add(
                        detail.getBook().getPrice()
                                .multiply(LOST_FEE_PERCENT)
                                .multiply(BigDecimal.valueOf(detail.getQuantity()))
                );
            }
        }
        
        // Tạo phạt
        if (totalFine.compareTo(BigDecimal.ZERO) > 0) {
            fineService.createFine(record.getId(), totalFine, "Phạt mất sách");
        }
        
        BorrowRecord updated = borrowRecordRepository.save(record);
        log.info("Marked borrow record {} as lost, fine: {}", recordId, totalFine);
        return updated;
    }
}
