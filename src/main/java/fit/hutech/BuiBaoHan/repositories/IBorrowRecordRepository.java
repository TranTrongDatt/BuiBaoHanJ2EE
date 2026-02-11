package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDate;
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
import fit.hutech.BuiBaoHan.entities.BorrowRecord;

/**
 * Repository cho BorrowRecord entity
 */
@Repository
public interface IBorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {

    Page<BorrowRecord> findByStatus(BorrowStatus status, Pageable pageable);
    
    long countByStatus(BorrowStatus status);

    @Query("SELECT br FROM BorrowRecord br LEFT JOIN FETCH br.details WHERE br.id = :id")
    Optional<BorrowRecord> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT br FROM BorrowRecord br WHERE br.libraryCard.user.id = :userId")
    Page<BorrowRecord> findByLibraryCardUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT br FROM BorrowRecord br WHERE br.libraryCard.user.id = :userId AND br.status = 'BORROWING'")
    List<BorrowRecord> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.libraryCard.id = :cardId AND br.status = 'BORROWING'")
    int countCurrentlyBorrowed(@Param("cardId") Long cardId);

    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate < :today")
    List<BorrowRecord> findOverdueRecords(@Param("today") LocalDate today);

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate < :today")
    long countOverdue(@Param("today") LocalDate today);

    @Query("SELECT br FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate BETWEEN :start AND :end")
    List<BorrowRecord> findDueBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    List<BorrowRecord> findByLibraryCardId(Long libraryCardId);
    
    // Report methods
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.borrowDate BETWEEN :start AND :end")
    long countByBorrowDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.returnDate BETWEEN :start AND :end")
    long countByReturnDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate BETWEEN :start AND :end")
    long countOverdueBetweenDays(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    @Query("SELECT d.book.category.name, COUNT(d) FROM BorrowRecordDetail d GROUP BY d.book.category.name")
    List<Object[]> countByCategory();
    
    @Query(value = "SELECT b.id, b.title, COUNT(d.id) as borrow_count FROM book b " +
           "JOIN borrow_record_detail d ON b.id = d.book_id " +
           "GROUP BY b.id, b.title ORDER BY borrow_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBorrowedBooks(@Param("limit") int limit);
    
    @Query(value = "SELECT b.id, b.title, COUNT(d.id) as borrow_count FROM book b " +
           "JOIN borrow_record_detail d ON b.id = d.book_id " +
           "JOIN borrow_record br ON d.borrow_record_id = br.id " +
           "WHERE br.borrow_date BETWEEN :start AND :end " +
           "GROUP BY b.id, b.title ORDER BY borrow_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBorrowedBooksInPeriod(@Param("start") LocalDateTime start, 
                                                 @Param("end") LocalDateTime end, 
                                                 @Param("limit") int limit);
    
    @Query(value = "SELECT u.id, u.username, COUNT(br.id) as borrow_count FROM user u " +
           "JOIN library_card lc ON u.id = lc.user_id " +
           "JOIN borrow_record br ON lc.id = br.library_card_id " +
           "GROUP BY u.id, u.username ORDER BY borrow_count DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopBorrowers(@Param("limit") int limit);
    
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate < :date")
    long countOverdueBeforeDate(@Param("date") LocalDate date);

    // LibraryController support methods
    @Query("SELECT br FROM BorrowRecord br WHERE br.libraryCard.user.id = :userId AND br.status = :status")
    List<BorrowRecord> findByLibraryCardUserIdAndStatus(@Param("userId") Long userId, @Param("status") BorrowStatus status);
    
    // ==================== Dashboard methods ====================
    
    /**
     * Count all currently borrowed books (no parameter version)
     */
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWING'")
    long countCurrentlyBorrowed();
    
    /**
     * Count all overdue records (status BORROWING and dueDate < today)
     */
    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.status = 'BORROWING' AND br.dueDate < CURRENT_DATE")
    long countOverdue();
}
