package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.constants.CardType;
import fit.hutech.BuiBaoHan.entities.LibraryCard;

/**
 * Repository cho LibraryCard entity
 */
@Repository
public interface ILibraryCardRepository extends JpaRepository<LibraryCard, Long> {

    Optional<LibraryCard> findByCardNumber(String cardNumber);

    Optional<LibraryCard> findByUserId(Long userId);

    boolean existsByCardNumber(String cardNumber);

    boolean existsByUserId(Long userId);

    List<LibraryCard> findByStatus(CardStatus status);

    List<LibraryCard> findByCardType(CardType cardType);

    Page<LibraryCard> findByStatus(CardStatus status, Pageable pageable);

    @Query("SELECT lc FROM LibraryCard lc WHERE lc.expiryDate < CURRENT_TIMESTAMP AND lc.status = 'ACTIVE'")
    List<LibraryCard> findExpiredCards();

    @Query("SELECT lc FROM LibraryCard lc WHERE lc.expiryDate BETWEEN CURRENT_TIMESTAMP AND :date")
    List<LibraryCard> findCardsExpiringBefore(@Param("date") java.time.LocalDateTime date);

    @Query("SELECT lc FROM LibraryCard lc LEFT JOIN FETCH lc.user WHERE lc.id = :id")
    Optional<LibraryCard> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT COUNT(lc) FROM LibraryCard lc WHERE lc.status = :status")
    long countByStatus(@Param("status") CardStatus status);
    
    // Report methods
    @Query("SELECT COUNT(lc) FROM LibraryCard lc WHERE DATE(lc.issueDate) BETWEEN :start AND :end")
    long countByIssueDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT lc.cardType, COUNT(lc) FROM LibraryCard lc GROUP BY lc.cardType")
    List<Object[]> countByCardType();
    
    @Query("SELECT COUNT(lc) FROM LibraryCard lc WHERE lc.status = 'ACTIVE' AND DATE(lc.expiryDate) BETWEEN :start AND :end")
    long countExpiringCards(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
