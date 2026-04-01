package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.AttendanceStatus;
import fit.hutech.BuiBaoHan.entities.Attendance;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;

/**
 * Repository cho Attendance entity
 */
@Repository
public interface IAttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * Tìm attendance theo shipper và ngày
     */
    Optional<Attendance> findByShipperAndWorkDate(ShipperProfile shipper, LocalDate workDate);

    /**
     * Tìm attendance theo shipper ID và ngày
     */
    @Query("SELECT a FROM Attendance a WHERE a.shipper.id = :shipperId AND a.workDate = :workDate")
    Optional<Attendance> findByShipperIdAndWorkDate(@Param("shipperId") Long shipperId, 
                                                     @Param("workDate") LocalDate workDate);

    /**
     * Kiểm tra shipper đã chấm công ngày hôm nay chưa
     */
    boolean existsByShipperAndWorkDate(ShipperProfile shipper, LocalDate workDate);

    /**
     * Lấy tất cả attendance của shipper trong khoảng thời gian
     */
    @Query("SELECT a FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND a.workDate BETWEEN :startDate AND :endDate ORDER BY a.workDate DESC")
    List<Attendance> findByShipperIdAndDateRange(@Param("shipperId") Long shipperId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    /**
     * Lấy attendance theo shipper với phân trang
     */
    Page<Attendance> findByShipperIdOrderByWorkDateDesc(Long shipperId, Pageable pageable);

    /**
     * Lấy attendance với punches
     */
    @EntityGraph(attributePaths = {"punches"})
    @Query("SELECT a FROM Attendance a WHERE a.id = :id")
    Optional<Attendance> findByIdWithPunches(@Param("id") Long id);

    /**
     * Lấy attendance hôm nay của shipper với punches
     */
    @EntityGraph(attributePaths = {"punches"})
    @Query("SELECT a FROM Attendance a WHERE a.shipper.id = :shipperId AND a.workDate = :workDate")
    Optional<Attendance> findTodayAttendanceWithPunches(@Param("shipperId") Long shipperId,
                                                         @Param("workDate") LocalDate workDate);

    /**
     * Đếm số ngày làm việc của shipper trong tháng
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND a.status = 'COMPLETE' " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    long countWorkDaysInMonth(@Param("shipperId") Long shipperId,
                              @Param("year") int year,
                              @Param("month") int month);

    /**
     * Tổng số giờ làm việc trong tháng
     */
    @Query("SELECT COALESCE(SUM(a.actualWorkHours), 0) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    Double getTotalWorkHoursInMonth(@Param("shipperId") Long shipperId,
                                    @Param("year") int year,
                                    @Param("month") int month);

    /**
     * Tổng số giờ tăng ca trong tháng
     */
    @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    Double getTotalOvertimeHoursInMonth(@Param("shipperId") Long shipperId,
                                        @Param("year") int year,
                                        @Param("month") int month);

    /**
     * Alias for getTotalOvertimeHoursInMonth
     */
    default Double getTotalOvertimeInMonth(Long shipperId, int year, int month) {
        return getTotalOvertimeHoursInMonth(shipperId, year, month);
    }

    /**
     * Tổng thu nhập ròng trong tháng
     */
    @Query("SELECT COALESCE(SUM(a.netEarning), 0) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    BigDecimal getTotalNetEarningInMonth(@Param("shipperId") Long shipperId,
                                         @Param("year") int year,
                                         @Param("month") int month);

    /**
     * Đếm số ngày đi trễ trong tháng
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND a.lateMinutes > 0 " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    long countLateDaysInMonth(@Param("shipperId") Long shipperId,
                              @Param("year") int year,
                              @Param("month") int month);

    /**
     * Tổng số phút đi trễ trong tháng
     */
    @Query("SELECT COALESCE(SUM(a.lateMinutes), 0) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    Integer getTotalLateMinutesInMonth(@Param("shipperId") Long shipperId,
                                       @Param("year") int year,
                                       @Param("month") int month);

    /**
     * Tổng số lượt chấm công thiếu trong tháng
     */
    @Query("SELECT COALESCE(SUM(a.missingPunches), 0) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    Integer getTotalMissingPunchesInMonth(@Param("shipperId") Long shipperId,
                                          @Param("year") int year,
                                          @Param("month") int month);

    /**
     * Lấy attendance trong tháng
     */
    @Query("SELECT a FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month " +
           "ORDER BY a.workDate ASC")
    List<Attendance> findByShipperIdAndMonth(@Param("shipperId") Long shipperId,
                                              @Param("year") int year,
                                              @Param("month") int month);

    /**
     * Lấy attendance theo trạng thái
     */
    List<Attendance> findByStatus(AttendanceStatus status);

    /**
     * Đếm attendance theo trạng thái trong tháng
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.shipper.id = :shipperId " +
           "AND a.status = :status " +
           "AND YEAR(a.workDate) = :year AND MONTH(a.workDate) = :month")
    long countByStatusInMonth(@Param("shipperId") Long shipperId,
                              @Param("status") AttendanceStatus status,
                              @Param("year") int year,
                              @Param("month") int month);

    /**
     * Lấy attendance chưa xử lý (status = INCOMPLETE)
     */
    @Query("SELECT a FROM Attendance a WHERE a.status = 'INCOMPLETE' AND a.workDate < :today")
    List<Attendance> findIncompleteAttendances(@Param("today") LocalDate today);
}
