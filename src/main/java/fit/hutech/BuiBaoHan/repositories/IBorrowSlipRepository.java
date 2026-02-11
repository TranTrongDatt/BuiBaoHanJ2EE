package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.BorrowStatus;
import fit.hutech.BuiBaoHan.entities.BorrowSlip;

/**
 * Repository cho BorrowSlip entity
 */
@Repository
public interface IBorrowSlipRepository extends JpaRepository<BorrowSlip, Long> {

    Optional<BorrowSlip> findBySlipCode(String slipCode);

    boolean existsBySlipCode(String slipCode);

    List<BorrowSlip> findByUserId(Long userId);

    Page<BorrowSlip> findByUserId(Long userId, Pageable pageable);

    List<BorrowSlip> findByStatus(BorrowStatus status);

    Page<BorrowSlip> findByStatus(BorrowStatus status, Pageable pageable);

    @Query("SELECT bs FROM BorrowSlip bs WHERE bs.user.id = :userId AND bs.status = :status")
    List<BorrowSlip> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") BorrowStatus status);

    @Query("SELECT bs FROM BorrowSlip bs WHERE bs.status = 'BORROWING' " +
           "AND bs.expectedReturnDate < CURRENT_TIMESTAMP")
    List<BorrowSlip> findOverdueSlips();

    @Query("SELECT bs FROM BorrowSlip bs WHERE bs.status = 'BORROWING' " +
           "AND bs.expectedReturnDate BETWEEN CURRENT_TIMESTAMP AND :date")
    List<BorrowSlip> findSlipsDueBefore(@Param("date") LocalDateTime date);

    @Query("SELECT bs FROM BorrowSlip bs LEFT JOIN FETCH bs.details WHERE bs.id = :id")
    Optional<BorrowSlip> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT bs FROM BorrowSlip bs LEFT JOIN FETCH bs.user LEFT JOIN FETCH bs.libraryCard WHERE bs.id = :id")
    Optional<BorrowSlip> findByIdWithUserAndCard(@Param("id") Long id);

    @Query("SELECT COUNT(bs) FROM BorrowSlip bs WHERE bs.user.id = :userId AND bs.status = 'BORROWING'")
    int countActiveBorrowsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(bs) FROM BorrowSlip bs WHERE bs.status = :status")
    long countByStatus(@Param("status") BorrowStatus status);

    @Query("SELECT bs FROM BorrowSlip bs WHERE bs.borrowDate BETWEEN :startDate AND :endDate")
    List<BorrowSlip> findByBorrowDateBetween(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
}
