package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity cấu hình hệ thống Shipper (Singleton)
 * 
 * Chứa các thông số:
 * - Lương cơ bản: 30,000 VND/giờ
 * - Hệ số tăng ca: x2
 * - Giờ làm: tối thiểu 6h, tối đa 12h, chuẩn 8h
 * - Ca sớm nhất: 7:00 AM
 * - Phụ cấp xăng: 500,000 VND/tháng
 * - Phụ cấp ăn: 100,000 VND/ngày
 * - Phạt đi trễ: 8,000 VND/phút
 * - Phạt thiếu chấm công: 8,000 VND/card
 */
@Entity
@Table(name = "shipper_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Cấu hình lương ====================
    
    /**
     * Lương cơ bản theo giờ (VND)
     * Mặc định: 30,000 VND/giờ
     */
    @Builder.Default
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate = new BigDecimal("30000");

    /**
     * Hệ số nhân lương tăng ca
     * Mặc định: x2 (60,000 VND/giờ OT)
     */
    @Builder.Default
    @Column(name = "overtime_multiplier", precision = 3, scale = 2)
    private BigDecimal overtimeMultiplier = new BigDecimal("2.00");

    // ==================== Cấu hình giờ làm ====================
    
    /**
     * Số giờ làm tối thiểu mỗi ngày
     * Mặc định: 6 tiếng
     */
    @Builder.Default
    @Column(name = "min_hours_per_day")
    private Integer minHoursPerDay = 6;

    /**
     * Số giờ làm tối đa mỗi ngày
     * Mặc định: 12 tiếng
     */
    @Builder.Default
    @Column(name = "max_hours_per_day")
    private Integer maxHoursPerDay = 12;

    /**
     * Số giờ làm chuẩn (sau đó tính OT)
     * Mặc định: 8 tiếng
     */
    @Builder.Default
    @Column(name = "standard_hours_per_day")
    private Integer standardHoursPerDay = 8;

    /**
     * Giờ check-in sớm nhất
     * Mặc định: 07:00:00
     */
    @Builder.Default
    @Column(name = "earliest_check_in")
    private LocalTime earliestCheckIn = LocalTime.of(7, 0, 0);

    /**
     * Giờ check-out muộn nhất
     * Mặc định: 22:00:00
     */
    @Builder.Default
    @Column(name = "latest_check_out")
    private LocalTime latestCheckOut = LocalTime.of(22, 0, 0);

    // ==================== Cấu hình phụ cấp ====================
    
    /**
     * Phụ cấp xăng hàng tháng (VND)
     * Mặc định: 500,000 VND/tháng
     */
    @Builder.Default
    @Column(name = "gas_allowance_monthly", precision = 10, scale = 2)
    private BigDecimal gasAllowanceMonthly = new BigDecimal("500000");

    /**
     * Phụ cấp ăn trưa hàng ngày (VND)
     * Mặc định: 100,000 VND/ngày (chỉ được nếu làm đủ standard hours)
     */
    @Builder.Default
    @Column(name = "meal_allowance_daily", precision = 10, scale = 2)
    private BigDecimal mealAllowanceDaily = new BigDecimal("100000");

    // ==================== Cấu hình phạt ====================
    
    /**
     * Phạt thiếu chấm công (VND/card)
     * Mặc định: 8,000 VND/card thiếu
     */
    @Builder.Default
    @Column(name = "missing_punch_penalty", precision = 10, scale = 2)
    private BigDecimal missingPunchPenalty = new BigDecimal("8000");

    /**
     * Phạt đi trễ (VND/phút)
     * Mặc định: 8,000 VND/phút
     */
    @Builder.Default
    @Column(name = "late_penalty_per_minute", precision = 10, scale = 2)
    private BigDecimal latePenaltyPerMinute = new BigDecimal("8000");

    // ==================== Cấu hình chấm công ====================
    
    /**
     * Số lần chấm công tối thiểu mỗi ngày
     * Mặc định: 2 (CHECK_IN + CHECK_OUT)
     */
    @Builder.Default
    @Column(name = "min_punches_per_day")
    private Integer minPunchesPerDay = 2;

    /**
     * Số lần chấm công tối đa mỗi ngày
     * Mặc định: 4 (CHECK_IN + BREAK_START + BREAK_END + CHECK_OUT)
     */
    @Builder.Default
    @Column(name = "max_punches_per_day")
    private Integer maxPunchesPerDay = 4;

    // ==================== Cấu hình bảo hiểm (%) ====================
    
    /**
     * Tỷ lệ BHXH (% lương)
     * Mặc định: 8%
     */
    @Builder.Default
    @Column(name = "social_insurance_rate", precision = 5, scale = 2)
    private BigDecimal socialInsuranceRate = new BigDecimal("8.00");

    /**
     * Tỷ lệ BHYT (% lương)
     * Mặc định: 1.5%
     */
    @Builder.Default
    @Column(name = "health_insurance_rate", precision = 5, scale = 2)
    private BigDecimal healthInsuranceRate = new BigDecimal("1.50");

    /**
     * Tỷ lệ BHTN (% lương)
     * Mặc định: 1%
     */
    @Builder.Default
    @Column(name = "unemployment_insurance_rate", precision = 5, scale = 2)
    private BigDecimal unemploymentInsuranceRate = new BigDecimal("1.00");

    // ==================== Timestamps ====================
    
    /**
     * Trạng thái active của config (chỉ có 1 config active tại 1 thời điểm)
     */
    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Helper Methods ====================

    /**
     * Tạo config mặc định
     */
    public static ShipperConfig createDefault() {
        return ShipperConfig.builder()
                .hourlyRate(new BigDecimal("30000"))
                .overtimeMultiplier(new BigDecimal("2.00"))
                .minHoursPerDay(6)
                .maxHoursPerDay(12)
                .standardHoursPerDay(8)
                .earliestCheckIn(LocalTime.of(7, 0, 0))
                .latestCheckOut(LocalTime.of(22, 0, 0))
                .gasAllowanceMonthly(new BigDecimal("500000"))
                .mealAllowanceDaily(new BigDecimal("100000"))
                .missingPunchPenalty(new BigDecimal("8000"))
                .latePenaltyPerMinute(new BigDecimal("8000"))
                .minPunchesPerDay(2)
                .maxPunchesPerDay(4)
                .socialInsuranceRate(new BigDecimal("8.00"))
                .healthInsuranceRate(new BigDecimal("1.50"))
                .unemploymentInsuranceRate(new BigDecimal("1.00"))
                .isActive(true)
                .build();
    }
    
    /**
     * Tính lương tăng ca theo giờ
     */
    public BigDecimal getOvertimeHourlyRate() {
        return hourlyRate.multiply(overtimeMultiplier);
    }

    /**
     * Tính tiền phạt đi trễ
     */
    public BigDecimal calculateLatePenalty(int lateMinutes) {
        if (lateMinutes <= 0) return BigDecimal.ZERO;
        return latePenaltyPerMinute.multiply(BigDecimal.valueOf(lateMinutes));
    }

    /**
     * Tính tiền phạt thiếu chấm công
     */
    public BigDecimal calculateMissingPunchPenalty(int missingPunches) {
        if (missingPunches <= 0) return BigDecimal.ZERO;
        return missingPunchPenalty.multiply(BigDecimal.valueOf(missingPunches));
    }

    /**
     * Tính lương cơ bản theo số giờ làm
     */
    public BigDecimal calculateBaseSalary(BigDecimal workHours) {
        return hourlyRate.multiply(workHours);
    }

    /**
     * Tính lương tăng ca theo số giờ OT
     */
    public BigDecimal calculateOvertimeSalary(BigDecimal overtimeHours) {
        return getOvertimeHourlyRate().multiply(overtimeHours);
    }

    /**
     * Tính tổng khấu trừ bảo hiểm (% lương)
     */
    public BigDecimal getTotalInsuranceRate() {
        return socialInsuranceRate
                .add(healthInsuranceRate)
                .add(unemploymentInsuranceRate);
    }
}
