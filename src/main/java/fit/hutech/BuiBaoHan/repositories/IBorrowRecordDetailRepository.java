package fit.hutech.BuiBaoHan.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.BorrowRecordDetail;

/**
 * Repository cho BorrowRecordDetail entity
 */
@Repository
public interface IBorrowRecordDetailRepository extends JpaRepository<BorrowRecordDetail, Long> {

    List<BorrowRecordDetail> findByBorrowRecordId(Long borrowRecordId);

    @Query("SELECT brd FROM BorrowRecordDetail brd WHERE brd.book.id = :bookId AND brd.returnDate IS NULL")
    List<BorrowRecordDetail> findActiveByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(brd) FROM BorrowRecordDetail brd WHERE brd.book.id = :bookId AND brd.returnDate IS NULL")
    int countActiveByBookId(@Param("bookId") Long bookId);
}
