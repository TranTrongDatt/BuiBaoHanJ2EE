package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.FineStatus;
import fit.hutech.BuiBaoHan.entities.Fine;

/**
 * Repository cho Fine entity
 */
@Repository
public interface IFineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserId(Long userId);

    Page<Fine> findByUserId(Long userId, Pageable pageable);

    List<Fine> findByPaidFalse();

    List<Fine> findByUserIdAndPaidFalse(Long userId);

    Page<Fine> findByPaidFalse(Pageable pageable);

    @Query("SELECT f FROM Fine f WHERE f.borrowSlip.id = :borrowSlipId")
    List<Fine> findByBorrowSlipId(@Param("borrowSlipId") Long borrowSlipId);

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.user.id = :userId AND f.paid = false")
    BigDecimal getTotalUnpaidFinesByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.paid = false")
    BigDecimal getTotalUnpaidFines();

    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.paid = true")
    BigDecimal getTotalPaidFines();

    @Query("SELECT COUNT(f) FROM Fine f WHERE f.user.id = :userId AND f.paid = false")
    int countUnpaidFinesByUserId(@Param("userId") Long userId);
    
    // Report methods
    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.status = :status")
    BigDecimal sumByStatus(@Param("status") FineStatus status);
    
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status = :status")
    long countByStatus(@Param("status") FineStatus status);
    
    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.createdAt BETWEEN :start AND :end")
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.paid = true AND f.paidAt BETWEEN :start AND :end")
    long countPaidBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.status = 'WAIVED' AND f.updatedAt BETWEEN :start AND :end")
    BigDecimal sumWaivedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(f) FROM Fine f WHERE f.status = 'WAIVED' AND f.updatedAt BETWEEN :start AND :end")
    long countWaivedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Additional methods for FineService
    Page<Fine> findByStatus(FineStatus status, Pageable pageable);
    
    @Query("SELECT f FROM Fine f WHERE f.user.id = :userId AND f.paid = false")
    List<Fine> findUnpaidByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM Fine f WHERE f.user.id = :userId AND f.paid = false")
    BigDecimal sumUnpaidByUserId(@Param("userId") Long userId);
    
    @Query("SELECT SUM(f.paidAmount) FROM Fine f WHERE f.paidAt BETWEEN :start AND :end")
    BigDecimal sumPaidBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COALESCE(SUM(f.amount - COALESCE(f.paidAmount, 0)), 0) FROM Fine f WHERE f.status = :status")
    BigDecimal sumRemainingByStatus(@Param("status") FineStatus status);

    // LibraryController support methods
    @Query("SELECT f FROM Fine f WHERE f.user.id = :userId AND f.paid = true")
    List<Fine> findPaidByUserId(@Param("userId") Long userId);
}
