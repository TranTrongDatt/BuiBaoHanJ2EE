package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.PunchType;
import fit.hutech.BuiBaoHan.entities.Attendance;
import fit.hutech.BuiBaoHan.entities.AttendancePunch;

/**
 * Repository cho AttendancePunch entity
 */
@Repository
public interface IAttendancePunchRepository extends JpaRepository<AttendancePunch, Long> {

    /**
     * Tìm tất cả punches của một attendance
     */
    List<AttendancePunch> findByAttendanceOrderByPunchTimeAsc(Attendance attendance);

    /**
     * Tìm tất cả punches theo attendance ID
     */
    List<AttendancePunch> findByAttendanceIdOrderByPunchTimeAsc(Long attendanceId);

    /**
     * Tìm punch theo attendance và loại punch
     */
    Optional<AttendancePunch> findByAttendanceAndPunchType(Attendance attendance, PunchType punchType);

    /**
     * Kiểm tra đã có loại punch này trong attendance chưa
     */
    boolean existsByAttendanceAndPunchType(Attendance attendance, PunchType punchType);

    /**
     * Kiểm tra đã có loại punch này trong attendance (theo ID) chưa
     */
    @Query("SELECT COUNT(p) > 0 FROM AttendancePunch p WHERE p.attendance.id = :attendanceId AND p.punchType = :punchType")
    boolean existsByAttendanceIdAndPunchType(@Param("attendanceId") Long attendanceId, 
                                              @Param("punchType") PunchType punchType);

    /**
     * Đếm số punch trong attendance
     */
    long countByAttendanceId(Long attendanceId);

    /**
     * Đếm số punch đi trễ trong attendance
     */
    @Query("SELECT COUNT(p) FROM AttendancePunch p WHERE p.attendance.id = :attendanceId AND p.isLate = true")
    long countLatePunchesInAttendance(@Param("attendanceId") Long attendanceId);

    /**
     * Lấy punch cuối cùng của attendance
     */
    @Query("SELECT p FROM AttendancePunch p WHERE p.attendance.id = :attendanceId ORDER BY p.punchTime DESC")
    List<AttendancePunch> findLastPunchByAttendanceId(@Param("attendanceId") Long attendanceId);

    /**
     * Lấy punch đầu tiên (CHECK_IN) của attendance
     */
    @Query("SELECT p FROM AttendancePunch p WHERE p.attendance.id = :attendanceId AND p.punchType = 'CHECK_IN'")
    Optional<AttendancePunch> findCheckInPunch(@Param("attendanceId") Long attendanceId);

    /**
     * Lấy punch cuối cùng (CHECK_OUT) của attendance
     */
    @Query("SELECT p FROM AttendancePunch p WHERE p.attendance.id = :attendanceId AND p.punchType = 'CHECK_OUT'")
    Optional<AttendancePunch> findCheckOutPunch(@Param("attendanceId") Long attendanceId);

    /**
     * Lấy tất cả punches của shipper trong ngày
     */
    @Query("SELECT p FROM AttendancePunch p WHERE p.attendance.shipper.id = :shipperId " +
           "AND DATE(p.punchTime) = :date ORDER BY p.punchTime ASC")
    List<AttendancePunch> findByShipperIdAndDate(@Param("shipperId") Long shipperId,
                                                  @Param("date") LocalDate date);

    /**
     * Lấy tất cả punches của shipper trong khoảng thời gian
     */
    @Query("SELECT p FROM AttendancePunch p WHERE p.attendance.shipper.id = :shipperId " +
           "AND p.punchTime BETWEEN :startTime AND :endTime ORDER BY p.punchTime ASC")
    List<AttendancePunch> findByShipperIdAndTimeRange(@Param("shipperId") Long shipperId,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Đếm số punch đi trễ của shipper trong tháng
     */
    @Query("SELECT COUNT(p) FROM AttendancePunch p WHERE p.attendance.shipper.id = :shipperId " +
           "AND p.isLate = true " +
           "AND YEAR(p.punchTime) = :year AND MONTH(p.punchTime) = :month")
    long countLatePunchesInMonth(@Param("shipperId") Long shipperId,
                                 @Param("year") int year,
                                 @Param("month") int month);

    /**
     * Tổng số phút đi trễ của shipper trong tháng
     */
    @Query("SELECT COALESCE(SUM(p.lateMinutes), 0) FROM AttendancePunch p WHERE p.attendance.shipper.id = :shipperId " +
           "AND YEAR(p.punchTime) = :year AND MONTH(p.punchTime) = :month")
    Integer getTotalLateMinutesInMonth(@Param("shipperId") Long shipperId,
                                       @Param("year") int year,
                                       @Param("month") int month);
}
