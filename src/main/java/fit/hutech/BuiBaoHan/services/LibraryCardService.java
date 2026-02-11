package fit.hutech.BuiBaoHan.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.constants.CardType;
import fit.hutech.BuiBaoHan.dto.LibraryCardDto;
import fit.hutech.BuiBaoHan.entities.LibraryCard;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.ILibraryCardRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Thẻ thư viện
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LibraryCardService {

    private final ILibraryCardRepository libraryCardRepository;
    private final IUserRepository userRepository;

    /**
     * Lấy tất cả thẻ (phân trang)
     */
    @Transactional(readOnly = true)
    public Page<LibraryCard> getAllCards(Pageable pageable) {
        return libraryCardRepository.findAll(pageable);
    }

    /**
     * Lấy thẻ theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<LibraryCard> getCardsByStatus(CardStatus status) {
        return libraryCardRepository.findByStatus(status);
    }

    /**
     * Tìm thẻ theo ID
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCard> getCardById(Long id) {
        return libraryCardRepository.findById(id);
    }

    /**
     * Tìm thẻ theo số thẻ
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCard> getCardByNumber(String cardNumber) {
        return libraryCardRepository.findByCardNumber(cardNumber);
    }

    /**
     * Tìm thẻ của user
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCard> getCardByUserId(Long userId) {
        return libraryCardRepository.findByUserId(userId);
    }

    /**
     * Kiểm tra user đã có thẻ chưa
     */
    @Transactional(readOnly = true)
    public boolean userHasCard(Long userId) {
        return libraryCardRepository.findByUserId(userId).isPresent();
    }

    /**
     * Tạo thẻ mới cho user
     */
    public LibraryCard createCard(Long userId, CardType cardType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user với ID: " + userId));

        // Check if user already has a card
        if (libraryCardRepository.findByUserId(userId).isPresent()) {
            throw new IllegalStateException("User đã có thẻ thư viện");
        }

        // Generate unique card number
        String cardNumber = generateCardNumber(cardType);

        // Calculate validity dates based on card type
        LocalDateTime issueDate = LocalDateTime.now();
        LocalDateTime expiryDate = calculateExpiryDate(cardType, issueDate);

        LibraryCard card = LibraryCard.builder()
                .user(user)
                .cardNumber(cardNumber)
                .cardType(cardType)
                .status(CardStatus.ACTIVE)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .maxBooksAllowed(getMaxBooks(cardType))
                .maxBorrowDays(getMaxBorrowDays(cardType))
                .build();

        LibraryCard saved = libraryCardRepository.save(card);
        log.info("Created library card {} for user {}", cardNumber, userId);
        return saved;
    }

    /**
     * Cập nhật thẻ
     */
    public LibraryCard updateCard(Long id, LibraryCardDto dto) {
        LibraryCard existing = libraryCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + id));

        if (dto.cardType() != null) {
            existing.setCardType(dto.cardType());
            existing.setMaxBooksAllowed(getMaxBooks(dto.cardType()));
            existing.setMaxBorrowDays(getMaxBorrowDays(dto.cardType()));
        }
        if (dto.expiryDate() != null) {
            existing.setExpiryDate(dto.expiryDate());
        }

        LibraryCard updated = libraryCardRepository.save(existing);
        log.info("Updated library card: {}", id);
        return updated;
    }

    /**
     * Gia hạn thẻ
     */
    public LibraryCard renewCard(Long id) {
        LibraryCard card = libraryCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + id));

        // Calculate new expiry date
        LocalDateTime newExpiry = calculateExpiryDate(card.getCardType(), LocalDateTime.now());
        card.setExpiryDate(newExpiry);
        card.setStatus(CardStatus.ACTIVE);

        LibraryCard renewed = libraryCardRepository.save(card);
        log.info("Renewed library card: {}", card.getCardNumber());
        return renewed;
    }

    /**
     * Tạm khóa thẻ
     */
    public LibraryCard suspendCard(Long id, String reason) {
        LibraryCard card = libraryCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + id));

        card.setStatus(CardStatus.SUSPENDED);
        
        LibraryCard suspended = libraryCardRepository.save(card);
        log.info("Suspended library card: {} - Reason: {}", card.getCardNumber(), reason);
        return suspended;
    }

    /**
     * Mở khóa thẻ
     */
    public LibraryCard activateCard(Long id) {
        LibraryCard card = libraryCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + id));

        // Check if card is expired
        if (card.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Thẻ đã hết hạn, cần gia hạn trước khi kích hoạt");
        }

        card.setStatus(CardStatus.ACTIVE);
        
        LibraryCard activated = libraryCardRepository.save(card);
        log.info("Activated library card: {}", card.getCardNumber());
        return activated;
    }

    /**
     * Hủy thẻ
     */
    public LibraryCard cancelCard(Long id) {
        LibraryCard card = libraryCardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thẻ với ID: " + id));

        card.setStatus(CardStatus.CANCELLED);
        
        LibraryCard cancelled = libraryCardRepository.save(card);
        log.info("Cancelled library card: {}", card.getCardNumber());
        return cancelled;
    }

    /**
     * Kiểm tra thẻ có thể mượn sách không
     */
    @Transactional(readOnly = true)
    public boolean canBorrow(Long cardId) {
        return libraryCardRepository.findById(cardId)
                .filter(card -> card.getStatus() == CardStatus.ACTIVE 
                        && card.getExpiryDate().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    /**
     * Lấy số sách tối đa có thể mượn
     */
    @Transactional(readOnly = true)
    public int getMaxBooksAllowed(Long cardId) {
        return libraryCardRepository.findById(cardId)
                .map(LibraryCard::getMaxBooksAllowed)
                .orElse(0);
    }

    /**
     * Đếm thẻ sắp hết hạn (trong 30 ngày)
     */
    @Transactional(readOnly = true)
    public long countExpiringCards() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);
        return libraryCardRepository.countExpiringCards(startDate, endDate);
    }

    // ==================== Private Helper Methods ====================

    private String generateCardNumber(CardType cardType) {
        String prefix = switch (cardType) {
            case STANDARD -> "STD";
            case STUDENT -> "STU";
            case TEACHER -> "TCH";
            case VIP -> "VIP";
            case TEMPORARY -> "TMP";
        };
        
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String cardNumber = prefix + "-" + uuid;
        
        // Ensure uniqueness
        while (libraryCardRepository.existsByCardNumber(cardNumber)) {
            uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            cardNumber = prefix + "-" + uuid;
        }
        
        return cardNumber;
    }

    private LocalDateTime calculateExpiryDate(CardType cardType, LocalDateTime issueDate) {
        return switch (cardType) {
            case STANDARD -> issueDate.plusYears(1);   // 1 năm
            case STUDENT -> issueDate.plusYears(4);    // 4 năm
            case TEACHER -> issueDate.plusYears(5);    // 5 năm
            case VIP -> issueDate.plusYears(10);       // 10 năm
            case TEMPORARY -> issueDate.plusMonths(3); // 3 tháng
        };
    }

    private int getMaxBooks(CardType cardType) {
        return switch (cardType) {
            case STANDARD -> 5;
            case STUDENT -> 5;
            case TEACHER -> 10;
            case VIP -> 20;
            case TEMPORARY -> 2;
        };
    }

    private int getMaxBorrowDays(CardType cardType) {
        return switch (cardType) {
            case STANDARD -> 14;
            case STUDENT -> 14;
            case TEACHER -> 30;
            case VIP -> 60;
            case TEMPORARY -> 7;
        };
    }

    // ==================== LibraryController Wrapper Methods ====================

    /**
     * Tìm thẻ của user (wrapper trả về Optional)
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCard> findByUser(User user) {
        return getCardByUserId(user.getId());
    }

    /**
     * Tạo thẻ mới cho user với loại thẻ mặc định STANDARD
     */
    public LibraryCard createCard(User user) {
        return createCard(user.getId(), CardType.STANDARD);
    }

    /**
     * Tìm thẻ theo status (String) có phân trang
     */
    @Transactional(readOnly = true)
    public Page<LibraryCard> findByStatus(String status, Pageable pageable) {
        CardStatus cardStatus = CardStatus.valueOf(status.toUpperCase());
        return libraryCardRepository.findByStatus(cardStatus, pageable);
    }

    /**
     * Lấy tất cả thẻ (wrapper)
     */
    @Transactional(readOnly = true)
    public Page<LibraryCard> findAll(Pageable pageable) {
        return getAllCards(pageable);
    }

    /**
     * Tìm thẻ theo ID (wrapper)
     */
    @Transactional(readOnly = true)
    public Optional<LibraryCard> findById(Long id) {
        return getCardById(id);
    }

    /**
     * Kiểm tra user có phải chủ thẻ không (cho Spring Security)
     */
    @Transactional(readOnly = true)
    public boolean isCardOwner(Long cardId, User user) {
        return libraryCardRepository.findById(cardId)
                .map(card -> card.getUser().getId().equals(user.getId()))
                .orElse(false);
    }

    // ==================== AdminLibraryController Methods ====================

    /**
     * Đếm tất cả thẻ
     */
    @Transactional(readOnly = true)
    public long countAll() {
        return libraryCardRepository.count();
    }

    /**
     * Đếm thẻ đang hoạt động
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return libraryCardRepository.countByStatus(CardStatus.ACTIVE);
    }

    /**
     * Tìm thẻ (với tìm kiếm và lọc status)
     */
    @Transactional(readOnly = true)
    public Page<LibraryCard> findAll(String search, String status, Pageable pageable) {
        // Nếu có filter status
        if (status != null && !status.isEmpty()) {
            return findByStatus(status, pageable);
        }
        // TODO: Thêm tìm kiếm theo search nếu cần
        return findAll(pageable);
    }
}
