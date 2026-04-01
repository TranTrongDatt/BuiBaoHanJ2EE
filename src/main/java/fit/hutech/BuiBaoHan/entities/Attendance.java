package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.AttendanceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho ngày chấm công của Shipper
 * 
 * Mỗi shipper có 1 record/ngày làm việc
 * Unique constraint: (shipper_id, work_date)
 */
@Entity
@Table(name = "attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shipper_id", "work_date"}, name = "uk_attendance_shipper_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Quan hệ với Shipper ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private ShipperProfile shipper;

    /**
     * Ngày làm việc
     */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // ==================== Giờ làm thực tế ====================
    
    /**
     * Số giờ làm thực tế (sau khi trừ nghỉ trưa)
     */
    @Builder.Default
    @Column(name = "actual_work_hours", precision = 4, scale = 2)
    private BigDecimal actualWorkHours = BigDecimal.ZERO;

    /**
     * Số giờ tăng ca (sau 8h chuẩn)
     */
    @Builder.Default
    @Column(name = "overtime_hours", precision = 4, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    /**
     * Thời gian nghỉ trưa (phút)
     */
    @Builder.Default
    @Column(name = "break_duration")
    private Integer breakDuration = 0;

    // ==================== Trạng thái ====================
    
    /**
     * Trạng thái ngày chấm công
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AttendanceStatus status = AttendanceStatus.INCOMPLETE;

    /**
     * Số phút đi trễ (so với giờ check-in quy định)
     */
    @Builder.Default
    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    /**
     * Số lần thiếu chấm công
     * Quy định: tối thiểu 2 lần (CHECK_IN + CHECK_OUT)
     */
    @Builder.Default
    @Column(name = "missing_punches")
    private Integer missingPunches = 0;

    // ==================== Thống kê đơn hàng ====================
    
    /**
     * Tổng số đơn giao trong ngày
     */
    @Builder.Default
    @Column(name = "total_orders")
    private Integer totalOrders = 0;

    /**
     * Số đơn giao thành công
     */
    @Builder.Default
    @Column(name = "successful_orders")
    private Integer successfulOrders = 0;

    /**
     * Số đơn giao thất bại
     */
    @Builder.Default
    @Column(name = "failed_orders")
    private Integer failedOrders = 0;

    // ==================== Thu nhập ngày ====================
    
    /**
     * Lương cơ bản = actualWorkHours × hourlyRate
     */
    @Builder.Default
    @Column(name = "base_earning", precision = 12, scale = 2)
    private BigDecimal baseEarning = BigDecimal.ZERO;

    /**
     * Lương tăng ca = overtimeHours × hourlyRate × 2
     */
    @Builder.Default
    @Column(name = "overtime_earning", precision = 12, scale = 2)
    private BigDecimal overtimeEarning = BigDecimal.ZERO;

    /**
     * Phụ cấp ăn trưa (chỉ được nếu làm đủ 8h)
     */
    @Builder.Default
    @Column(name = "meal_allowance", precision = 12, scale = 2)
    private BigDecimal mealAllowance = BigDecimal.ZERO;

    /**
     * Thưởng giao hàng (bonus từ đơn thành công)
     */
    @Builder.Default
    @Column(name = "delivery_bonus", precision = 12, scale = 2)
    private BigDecimal deliveryBonus = BigDecimal.ZERO;

    /**
     * Tổng tiền phạt (trễ + thiếu chấm công)
     */
    @Builder.Default
    @Column(name = "penalty_amount", precision = 12, scale = 2)
    private BigDecimal penaltyAmount = BigDecimal.ZERO;

    /**
     * Thu nhập ròng ngày = base + OT + meal + bonus - penalty
     */
    @Builder.Default
    @Column(name = "net_earning", precision = 12, scale = 2)
    private BigDecimal netEarning = BigDecimal.ZERO;

    // ==================== Ghi chú ====================
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ==================== Timestamps ====================
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Quan hệ với AttendancePunch ====================
    
    @Builder.Default
    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AttendancePunch> punches = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "attendance", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipperPenalty> penalties = new ArrayList<>();

    // ==================== Helper Methods ====================
    
    /**
     * Thêm punch vào attendance
     */
    public void addPunch(AttendancePunch punch) {
        punches.add(punch);
        punch.setAttendance(this);
    }

    /**
     * Tính toán thu nhập ròng
     */
    public void calculateNetEarning() {
        this.netEarning = this.baseEarning
                .add(this.overtimeEarning)
                .add(this.mealAllowance)
                .add(this.deliveryBonus)
                .subtract(this.penaltyAmount);
    }

    /**
     * Tính số giờ làm từ các punch records
     * @param config Cấu hình shipper để lấy các tham số
     */
    public void calculateWorkHours(ShipperConfig config) {
        if (punches == null || punches.size() < 2) {
            this.actualWorkHours = BigDecimal.ZERO;
            this.overtimeHours = BigDecimal.ZERO;
            return;
        }

        // Tìm CHECK_IN và CHECK_OUT
        AttendancePunch checkIn = punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_IN)
                .findFirst().orElse(null);
        AttendancePunch checkOut = punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_OUT)
                .findFirst().orElse(null);

        if (checkIn == null || checkOut == null) {
            return;
        }

        // Tính tổng thời gian (phút)
        long totalMinutes = java.time.Duration.between(
                checkIn.getPunchTime(), checkOut.getPunchTime()
        ).toMinutes();

        // Trừ thời gian nghỉ
        int breakMins = java.util.Objects.requireNonNullElse(breakDuration, 0);
        totalMinutes -= breakMins;

        // Chuyển sang giờ
        BigDecimal totalHours = BigDecimal.valueOf(totalMinutes / 60.0)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Giới hạn tối đa
        int maxHours = config.getMaxHoursPerDay();
        if (totalHours.compareTo(BigDecimal.valueOf(maxHours)) > 0) {
            totalHours = BigDecimal.valueOf(maxHours);
        }

        this.actualWorkHours = totalHours;

        // Tính overtime (sau 8h chuẩn)
        int standardHours = config.getStandardHoursPerDay();
        if (totalHours.compareTo(BigDecimal.valueOf(standardHours)) > 0) {
            this.overtimeHours = totalHours.subtract(BigDecimal.valueOf(standardHours));
        } else {
            this.overtimeHours = BigDecimal.ZERO;
        }
    }

    /**
     * Kiểm tra đã check-in chưa
     */
    public boolean hasCheckedIn() {
        return punches.stream()
                .anyMatch(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_IN);
    }

    /**
     * Kiểm tra đã check-out chưa
     */
    public boolean hasCheckedOut() {
        return punches.stream()
                .anyMatch(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_OUT);
    }

    /**
     * Đếm số lần đã chấm công
     */
    public int getPunchCount() {
        return punches != null ? punches.size() : 0;
    }

    /**
     * Kiểm tra đã có loại punch này chưa
     */
    public boolean hasPunchType(String punchTypeName) {
        return punches.stream()
                .anyMatch(p -> p.getPunchType().name().equals(punchTypeName));
    }

    /**
     * Xác định punch type tiếp theo cần làm
     */
    public fit.hutech.BuiBaoHan.constants.PunchType getNextPunchType() {
        if (!hasCheckedIn()) {
            return fit.hutech.BuiBaoHan.constants.PunchType.CHECK_IN;
        }
        if (!hasPunchType("BREAK_START")) {
            return fit.hutech.BuiBaoHan.constants.PunchType.BREAK_START;
        }
        if (!hasPunchType("BREAK_END")) {
            return fit.hutech.BuiBaoHan.constants.PunchType.BREAK_END;
        }
        if (!hasCheckedOut()) {
            return fit.hutech.BuiBaoHan.constants.PunchType.CHECK_OUT;
        }
        return null; // Đã hoàn thành tất cả
    }

    /**
     * Lấy thời gian check-in
     */
    public java.time.LocalDateTime getCheckInTime() {
        return punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_IN)
                .map(AttendancePunch::getPunchTime)
                .findFirst().orElse(null);
    }

    /**
     * Lấy thời gian check-out
     */
    public java.time.LocalDateTime getCheckOutTime() {
        return punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.CHECK_OUT)
                .map(AttendancePunch::getPunchTime)
                .findFirst().orElse(null);
    }

    /**
     * Lấy thời gian bắt đầu nghỉ
     */
    public java.time.LocalDateTime getBreakStartTime() {
        return punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.BREAK_START)
                .map(AttendancePunch::getPunchTime)
                .findFirst().orElse(null);
    }

    /**
     * Lấy thời gian kết thúc nghỉ
     */
    public java.time.LocalDateTime getBreakEndTime() {
        return punches.stream()
                .filter(p -> p.getPunchType() == fit.hutech.BuiBaoHan.constants.PunchType.BREAK_END)
                .map(AttendancePunch::getPunchTime)
                .findFirst().orElse(null);
    }

    /**
     * Tính toán thu nhập ròng với config
     * @param config Cấu hình shipper
     */
    public void calculateNetEarning(ShipperConfig config) {
        // Tính lương cơ bản
        this.baseEarning = this.actualWorkHours.multiply(config.getHourlyRate());
        
        // Tính lương tăng ca
        this.overtimeEarning = this.overtimeHours.multiply(config.getOvertimeHourlyRate());
        
        // Tính phụ cấp ăn (nếu làm đủ giờ tiêu chuẩn)
        if (this.actualWorkHours.compareTo(BigDecimal.valueOf(config.getStandardHoursPerDay())) >= 0) {
            this.mealAllowance = config.getMealAllowanceDaily();
        }
        
        // Tính phạt
        if (this.lateMinutes != null && this.lateMinutes > 0) {
            BigDecimal latePenalty = config.getLatePenaltyPerMinute().multiply(BigDecimal.valueOf(this.lateMinutes));
            this.penaltyAmount = this.penaltyAmount.add(latePenalty);
        }
        
        // Tính thu nhập ròng
        calculateNetEarning();
    }
}
