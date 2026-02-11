package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.FineStatus;
import fit.hutech.BuiBaoHan.entities.BorrowRecord;
import fit.hutech.BuiBaoHan.entities.Fine;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBorrowRecordRepository;
import fit.hutech.BuiBaoHan.repositories.IFineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Tiền phạt
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FineService {

    private final IFineRepository fineRepository;
    private final IBorrowRecordRepository borrowRecordRepository;

    /**
     * Lấy tất cả phạt (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<Fine> getAllFines(Pageable pageable) {
        return fineRepository.findAll(pageable);
    }

    /**
     * Lấy phạt theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<Fine> getFinesByStatus(FineStatus status, Pageable pageable) {
        return fineRepository.findByStatus(status, pageable);
    }

    /**
     * Tìm phạt theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Fine> getFineById(Long id) {
        return fineRepository.findById(id);
    }

    /**
     * Lấy phạt của user
     */
    @Transactional(readOnly = true)
    public List<Fine> getFinesByUserId(Long userId) {
        return fineRepository.findByUserId(userId);
    }

    /**
     * Lấy phạt chưa thanh toán của user
     */
    @Transactional(readOnly = true)
    public List<Fine> getUnpaidFinesByUserId(Long userId) {
        return fineRepository.findUnpaidByUserId(userId);
    }

    /**
     * Tính tổng tiền phạt chưa thanh toán của user
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidAmount(Long userId) {
        return fineRepository.sumUnpaidByUserId(userId);
    }

    /**
     * Tạo phạt mới
     */
    public Fine createFine(Long borrowRecordId, BigDecimal amount, String reason) {
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + borrowRecordId));

        Fine fine = Fine.builder()
                .borrowRecord(borrowRecord)
                .amount(amount)
                .reason(reason)
                .status(FineStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Fine saved = fineRepository.save(fine);
        log.info("Created fine {} for borrow record {}, amount: {}", 
                saved.getId(), borrowRecordId, amount);
        return saved;
    }

    /**
     * Thanh toán phạt
     */
    public Fine payFine(Long fineId, String paymentMethod, String transactionId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new IllegalStateException("Phạt đã được thanh toán");
        }

        if (fine.getStatus() == FineStatus.WAIVED) {
            throw new IllegalStateException("Phạt đã được miễn");
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod(paymentMethod);
        fine.setTransactionId(transactionId);

        Fine paid = fineRepository.save(fine);
        log.info("Paid fine {}, amount: {}", fineId, fine.getAmount());
        return paid;
    }

    /**
     * Thanh toán một phần
     */
    public Fine partialPayFine(Long fineId, BigDecimal paidAmount, String paymentMethod) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));

        if (fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
            throw new IllegalStateException("Phạt không thể thanh toán thêm");
        }

        BigDecimal currentPaid = fine.getPaidAmount() != null ? fine.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal newPaidAmount = currentPaid.add(paidAmount);

        if (newPaidAmount.compareTo(fine.getAmount()) >= 0) {
            // Đã thanh toán đủ
            fine.setStatus(FineStatus.PAID);
            fine.setPaidAt(LocalDateTime.now());
            fine.setPaidAmount(fine.getAmount());
        } else {
            fine.setStatus(FineStatus.PARTIAL);
            fine.setPaidAmount(newPaidAmount);
        }

        fine.setPaymentMethod(paymentMethod);

        Fine updated = fineRepository.save(fine);
        log.info("Partial payment for fine {}, paid: {}, remaining: {}", 
                fineId, paidAmount, fine.getAmount().subtract(newPaidAmount));
        return updated;
    }

    /**
     * Miễn phạt
     */
    public Fine waiveFine(Long fineId, String reason) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new IllegalStateException("Không thể miễn phạt đã thanh toán");
        }

        fine.setStatus(FineStatus.WAIVED);
        fine.setWaivedReason(reason);
        fine.setWaivedAt(LocalDateTime.now());

        Fine waived = fineRepository.save(fine);
        log.info("Waived fine {}, reason: {}", fineId, reason);
        return waived;
    }

    /**
     * Hủy phạt
     */
    public void cancelFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new IllegalStateException("Không thể hủy phạt đã thanh toán");
        }

        fineRepository.delete(fine);
        log.info("Cancelled fine {}", fineId);
    }

    /**
     * Đếm phạt chưa thanh toán
     */
    @Transactional(readOnly = true)
    public long countUnpaidFines() {
        return fineRepository.countByStatus(FineStatus.PENDING) 
                + fineRepository.countByStatus(FineStatus.PARTIAL);
    }

    /**
     * Tổng số tiền phạt chưa thu
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidAmount() {
        BigDecimal pending = fineRepository.sumByStatus(FineStatus.PENDING);
        BigDecimal partial = fineRepository.sumRemainingByStatus(FineStatus.PARTIAL);
        
        return (pending != null ? pending : BigDecimal.ZERO)
                .add(partial != null ? partial : BigDecimal.ZERO);
    }

    /**
     * Kiểm tra user có phạt chưa thanh toán không
     */
    @Transactional(readOnly = true)
    public boolean hasUnpaidFines(Long userId) {
        return !fineRepository.findUnpaidByUserId(userId).isEmpty();
    }

    // ==================== LibraryController Wrapper Methods ====================

    /**
     * Lấy phạt chưa thanh toán của user (wrapper)
     */
    @Transactional(readOnly = true)
    public List<Fine> findUnpaidByUser(User user) {
        return getUnpaidFinesByUserId(user.getId());
    }

    /**
     * Lấy phạt đã thanh toán của user
     */
    @Transactional(readOnly = true)
    public List<Fine> findPaidByUser(User user) {
        return fineRepository.findPaidByUserId(user.getId());
    }

    /**
     * Tính tổng tiền phạt chưa thanh toán của user (wrapper)
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaidByUser(User user) {
        return getTotalUnpaidAmount(user.getId());
    }

    /**
     * Tìm phạt theo ID và user
     */
    @Transactional(readOnly = true)
    public Optional<Fine> findByIdAndUser(Long id, User user) {
        return fineRepository.findById(id)
                .filter(f -> f.getUser().getId().equals(user.getId()));
    }

    // ==================== ApiLibraryController Wrapper Methods ====================

    /**
     * Lấy phạt của user (wrapper nhận User)
     */
    @Transactional(readOnly = true)
    public List<Fine> findByUser(User user) {
        return getFinesByUserId(user.getId());
    }

    /**
     * Thanh toán phạt (wrapper với signature khác)
     */
    public Fine payFine(Long fineId, User user, String paymentMethod) {
        // Kiểm tra phạt có thuộc về user không
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));
        
        if (!fine.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Phạt không thuộc về user này");
        }
        
        return payFine(fineId, paymentMethod, null);
    }

    /**
     * Lấy phạt chưa thanh toán (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<Fine> findUnpaid(Pageable pageable) {
        return fineRepository.findByPaidFalse(pageable);
    }

    /**
     * Lấy tất cả phạt (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<Fine> findAll(Pageable pageable) {
        return getAllFines(pageable);
    }

    /**
     * Lấy phạt theo status (wrapper cho AdminLibraryController)
     */
    @Transactional(readOnly = true)
    public Page<Fine> findAll(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            FineStatus fineStatus = FineStatus.valueOf(status.toUpperCase());
            return getFinesByStatus(fineStatus, pageable);
        }
        return getAllFines(pageable);
    }

    /**
     * Admin đánh dấu phạt đã thanh toán
     */
    public Fine adminMarkPaid(Long fineId, String paymentMethod) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phạt ID: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new IllegalStateException("Phạt đã được thanh toán");
        }

        if (fine.getStatus() == FineStatus.WAIVED) {
            throw new IllegalStateException("Phạt đã được miễn");
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        fine.setPaymentMethod(paymentMethod);
        fine.setPaidAmount(fine.getAmount());

        Fine paid = fineRepository.save(fine);
        log.info("Admin marked fine {} as paid, method: {}", fineId, paymentMethod);
        return paid;
    }

    /**
     * Tạo phạt thủ công
     */
    public Fine createManualFine(Long borrowRecordId, String fineType, java.math.BigDecimal amount, String reason) {
        BorrowRecord borrowRecord = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mượn ID: " + borrowRecordId));

        Fine fine = Fine.builder()
                .borrowRecord(borrowRecord)
                .amount(amount)
                .reason(reason + " (" + fineType + ")")
                .status(FineStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Fine saved = fineRepository.save(fine);
        log.info("Created manual fine {} for borrow record {}, type: {}, amount: {}", 
                saved.getId(), borrowRecordId, fineType, amount);
        return saved;
    }

    // ==================== AdminLibraryController Methods ====================

    /**
     * Đếm số phạt chưa thanh toán
     */
    @Transactional(readOnly = true)
    public long countPending() {
        return countUnpaidFines();
    }

    /**
     * Tổng số tiền phạt chưa thu (wrapper)
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalUnpaid() {
        return getTotalUnpaidAmount();
    }

    /**
     * Tìm phạt theo thẻ
     */
    @Transactional(readOnly = true)
    public List<Fine> findByCard(LibraryCard card) {
        return getFinesByUserId(card.getUser().getId());
    }
}
