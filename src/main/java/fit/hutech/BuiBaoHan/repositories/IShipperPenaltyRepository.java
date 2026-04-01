package fit.hutech.BuiBaoHan.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.PenaltyType;
import fit.hutech.BuiBaoHan.entities.Attendance;
import fit.hutech.BuiBaoHan.entities.ShipperPenalty;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;

/**
 * Repository cho ShipperPenalty entity
 */
@Repository
public interface IShipperPenaltyRepository extends JpaRepository<ShipperPenalty, Long> {

    /**
     * Tìm tất cả penalty của shipper
     */
    List<ShipperPenalty> findByShipperOrderByPenaltyDateDesc(ShipperProfile shipper);

    /**
     * Tìm penalty theo shipper ID
     */
    Page<ShipperPenalty> findByShipperIdOrderByPenaltyDateDesc(Long shipperId, Pageable pageable);

    /**
     * Tìm penalty theo shipper ID (list)
     */
    List<ShipperPenalty> findByShipperIdOrderByPenaltyDateDesc(Long shipperId);

    /**
     * Tìm penalty theo ngày
     */
    List<ShipperPenalty> findByPenaltyDate(LocalDate penaltyDate);

    /**
     * Tìm penalty theo attendance
     */
    List<ShipperPenalty> findByAttendance(Attendance attendance);

    /**
     * Tìm penalty theo attendance ID
     */
    List<ShipperPenalty> findByAttendanceId(Long attendanceId);

    /**
     * Tìm penalty theo loại
     */
    List<ShipperPenalty> findByPenaltyType(PenaltyType penaltyType);

    /**
     * Tìm penalty theo loại với phân trang
     */
    Page<ShipperPenalty> findByPenaltyType(PenaltyType penaltyType, Pageable pageable);

    /**
     * Tìm penalty chưa áp dụng
     */
    @Query("SELECT p FROM ShipperPenalty p WHERE p.status = 'PENDING'")
    List<ShipperPenalty> findPendingPenalties();

    /**
     * Tìm penalty đã áp dụng
     */
    @Query("SELECT p FROM ShipperPenalty p WHERE p.status = 'APPLIED'")
    List<ShipperPenalty> findAppliedPenalties();

    /**
     * Tìm penalty đã miễn
     */
    @Query("SELECT p FROM ShipperPenalty p WHERE p.status = 'WAIVED'")
    List<ShipperPenalty> findWaivedPenalties();

    /**
     * Lấy penalty của shipper trong khoảng thời gian
     */
    @Query("SELECT p FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND p.penaltyDate BETWEEN :startDate AND :endDate ORDER BY p.penaltyDate DESC")
    List<ShipperPenalty> findByShipperIdAndDateRange(@Param("shipperId") Long shipperId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    /**
     * Lấy penalty của shipper trong tháng
     */
    @Query("SELECT p FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month " +
           "ORDER BY p.penaltyDate DESC")
    List<ShipperPenalty> findByShipperIdAndMonth(@Param("shipperId") Long shipperId,
                                                  @Param("year") int year,
                                                  @Param("month") int month);

    /**
     * Tổng penalty của shipper trong tháng
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND (p.status = 'PENDING' OR p.status = 'APPLIED') " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month")
    BigDecimal getTotalPenaltyInMonth(@Param("shipperId") Long shipperId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    /**
     * Đếm số penalty của shipper trong tháng
     */
    @Query("SELECT COUNT(p) FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month")
    long countPenaltiesInMonth(@Param("shipperId") Long shipperId,
                               @Param("year") int year,
                               @Param("month") int month);

    /**
     * Đếm số penalty theo loại của shipper trong tháng
     */
    @Query("SELECT COUNT(p) FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND p.penaltyType = :penaltyType " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month")
    long countPenaltiesByTypeInMonth(@Param("shipperId") Long shipperId,
                                     @Param("penaltyType") PenaltyType penaltyType,
                                     @Param("year") int year,
                                     @Param("month") int month);

    /**
     * Tổng penalty đi trễ của shipper trong tháng
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND p.penaltyType = 'LATE' " +
           "AND (p.status = 'PENDING' OR p.status = 'APPLIED') " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month")
    BigDecimal getTotalLatePenaltyInMonth(@Param("shipperId") Long shipperId,
                                          @Param("year") int year,
                                          @Param("month") int month);

    /**
     * Tổng penalty chấm công thiếu của shipper trong tháng
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ShipperPenalty p WHERE p.shipper.id = :shipperId " +
           "AND p.penaltyType = 'MISSING_PUNCH' " +
           "AND (p.status = 'PENDING' OR p.status = 'APPLIED') " +
           "AND YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month")
    BigDecimal getTotalMissingPunchPenaltyInMonth(@Param("shipperId") Long shipperId,
                                                  @Param("year") int year,
                                                  @Param("month") int month);

    /**
     * Thống kê penalty theo loại trong tháng
     */
    @Query("SELECT p.penaltyType, COUNT(p), SUM(p.amount) FROM ShipperPenalty p " +
           "WHERE YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month " +
           "GROUP BY p.penaltyType")
    List<Object[]> getPenaltyStatsByMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Top shipper có nhiều penalty nhất trong tháng
     */
    @Query("SELECT p.shipper.id, p.shipper.fullName, COUNT(p), SUM(p.amount) FROM ShipperPenalty p " +
           "WHERE YEAR(p.penaltyDate) = :year AND MONTH(p.penaltyDate) = :month " +
           "GROUP BY p.shipper.id, p.shipper.fullName " +
           "ORDER BY SUM(p.amount) DESC")
    List<Object[]> getTopPenalizedShippersInMonth(@Param("year") int year, 
                                                   @Param("month") int month,
                                                   Pageable pageable);
}
