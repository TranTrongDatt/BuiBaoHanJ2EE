package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.SalaryStatus;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.ShipperSalary;

/**
 * Repository cho ShipperSalary entity
 */
@Repository
public interface IShipperSalaryRepository extends JpaRepository<ShipperSalary, Long> {

    /**
     * Tìm salary theo shipper và tháng/năm
     */
    Optional<ShipperSalary> findByShipperAndSalaryYearAndSalaryMonth(ShipperProfile shipper, int year, int month);

    /**
     * Tìm salary theo shipper ID và tháng/năm
     */
    @Query("SELECT s FROM ShipperSalary s WHERE s.shipper.id = :shipperId AND s.salaryYear = :year AND s.salaryMonth = :month")
    Optional<ShipperSalary> findByShipperIdAndYearAndMonth(@Param("shipperId") Long shipperId,
                                                            @Param("year") int year,
                                                            @Param("month") int month);

    /**
     * Kiểm tra đã có salary cho shipper trong tháng chưa
     */
    boolean existsByShipperAndSalaryYearAndSalaryMonth(ShipperProfile shipper, int year, int month);

    /**
     * Lấy tất cả salary của shipper
     */
    List<ShipperSalary> findByShipperIdOrderBySalaryYearDescSalaryMonthDesc(Long shipperId);

    /**
     * Lấy salary của shipper với phân trang
     */
    Page<ShipperSalary> findByShipperIdOrderBySalaryYearDescSalaryMonthDesc(Long shipperId, Pageable pageable);

    /**
     * Lấy tất cả salary trong tháng
     */
    @Query("SELECT s FROM ShipperSalary s WHERE s.salaryYear = :year AND s.salaryMonth = :month ORDER BY s.shipper.fullName")
    List<ShipperSalary> findAllByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Lấy salary theo trạng thái
     */
    List<ShipperSalary> findByStatus(SalaryStatus status);

    /**
     * Lấy salary theo trạng thái với phân trang
     */
    Page<ShipperSalary> findByStatus(SalaryStatus status, Pageable pageable);

    /**
     * Đếm số salary theo trạng thái trong tháng
     */
    @Query("SELECT COUNT(s) FROM ShipperSalary s WHERE s.status = :status AND s.salaryYear = :year AND s.salaryMonth = :month")
    long countByStatusInMonth(@Param("status") SalaryStatus status, 
                              @Param("year") int year, 
                              @Param("month") int month);

    /**
     * Tổng lương cần trả trong tháng
     */
    @Query("SELECT COALESCE(SUM(s.netSalary), 0) FROM ShipperSalary s WHERE s.salaryYear = :year AND s.salaryMonth = :month")
    BigDecimal getTotalSalaryInMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Tổng lương đã trả trong tháng
     */
    @Query("SELECT COALESCE(SUM(s.netSalary), 0) FROM ShipperSalary s " +
           "WHERE s.status = 'PAID' AND s.salaryYear = :year AND s.salaryMonth = :month")
    BigDecimal getTotalPaidSalaryInMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Tổng lương chờ duyệt trong tháng
     */
    @Query("SELECT COALESCE(SUM(s.netSalary), 0) FROM ShipperSalary s " +
           "WHERE s.status = 'PENDING' AND s.salaryYear = :year AND s.salaryMonth = :month")
    BigDecimal getTotalPendingSalaryInMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Lấy salary chưa duyệt
     */
    @Query("SELECT s FROM ShipperSalary s WHERE s.status = 'PENDING' ORDER BY s.salaryYear DESC, s.salaryMonth DESC")
    List<ShipperSalary> findPendingSalaries();

    /**
     * Lấy salary đã duyệt chưa trả
     */
    @Query("SELECT s FROM ShipperSalary s WHERE s.status = 'APPROVED' ORDER BY s.salaryYear ASC, s.salaryMonth ASC")
    List<ShipperSalary> findApprovedSalaries();

    /**
     * Tính tổng thu nhập của shipper trong năm
     */
    @Query("SELECT COALESCE(SUM(s.netSalary), 0) FROM ShipperSalary s " +
           "WHERE s.shipper.id = :shipperId AND s.salaryYear = :year AND s.status = 'PAID'")
    BigDecimal getTotalEarningInYear(@Param("shipperId") Long shipperId, @Param("year") int year);

    /**
     * Lấy salary gần nhất của shipper
     */
    @Query("SELECT s FROM ShipperSalary s WHERE s.shipper.id = :shipperId ORDER BY s.salaryYear DESC, s.salaryMonth DESC")
    List<ShipperSalary> findRecentSalaries(@Param("shipperId") Long shipperId, Pageable pageable);

    /**
     * Thống kê salary theo năm
     */
    @Query("SELECT s.salaryMonth, SUM(s.netSalary) FROM ShipperSalary s " +
           "WHERE s.salaryYear = :year AND s.status = 'PAID' GROUP BY s.salaryMonth ORDER BY s.salaryMonth")
    List<Object[]> getSalaryStatsByYear(@Param("year") int year);

    /**
     * Alias for findByShipperIdOrderBySalaryYearDescSalaryMonthDesc
     */
    default List<ShipperSalary> findByShipperIdOrderByYearDescMonthDesc(Long shipperId) {
        return findByShipperIdOrderBySalaryYearDescSalaryMonthDesc(shipperId);
    }
}
