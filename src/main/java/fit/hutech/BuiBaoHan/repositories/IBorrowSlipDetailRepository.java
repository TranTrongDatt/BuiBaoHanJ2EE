package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.BorrowSlipDetail;

/**
 * Repository cho BorrowSlipDetail entity
 */
@Repository
public interface IBorrowSlipDetailRepository extends JpaRepository<BorrowSlipDetail, Long> {

    List<BorrowSlipDetail> findByBorrowSlipId(Long borrowSlipId);

    List<BorrowSlipDetail> findByBookId(Long bookId);

    @Query("SELECT bsd FROM BorrowSlipDetail bsd WHERE bsd.borrowSlip.id = :borrowSlipId AND bsd.isReturned = false")
    List<BorrowSlipDetail> findUnreturnedDetailsBySlipId(@Param("borrowSlipId") Long borrowSlipId);

    @Query("SELECT bsd FROM BorrowSlipDetail bsd WHERE bsd.borrowSlip.id = :borrowSlipId AND bsd.book.id = :bookId")
    List<BorrowSlipDetail> findBySlipIdAndBookId(@Param("borrowSlipId") Long borrowSlipId, @Param("bookId") Long bookId);

    @Query("SELECT COUNT(bsd) FROM BorrowSlipDetail bsd WHERE bsd.book.id = :bookId AND bsd.isReturned = false")
    int countUnreturnedByBookId(@Param("bookId") Long bookId);

    @Query("SELECT SUM(bsd.fineAmount) FROM BorrowSlipDetail bsd WHERE bsd.borrowSlip.id = :borrowSlipId")
    java.math.BigDecimal getTotalFineBySlipId(@Param("borrowSlipId") Long borrowSlipId);
}
