package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import fit.hutech.BuiBaoHan.constants.PenaltyType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho khoản phạt của Shipper
 * 
 * Các loại phạt:
 * - Đi trễ (LATE): 8,000 VND/phút
 * - Thiếu chấm công (MISSING_PUNCH): 8,000 VND/card
 * - Về sớm (EARLY_LEAVE): Tùy quy định
 * - Giao hàng thất bại (FAILED_DELIVERY): Tùy trường hợp
 * - Khác (OTHER): Admin quyết định
 * 
 * Trạng thái:
 * - PENDING: Chờ xử lý (sẽ tính vào lương)
 * - APPLIED: Đã áp dụng (đã khấu trừ)
 * - WAIVED: Miễn phạt (Admin xử lý)
 */
@Entity
@Table(name = "shipper_penalty")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Quan hệ ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    /**
     * Liên kết với ngày chấm công (nếu có)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    /**
     * Liên kết với kỳ lương (khi đã tính vào lương)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_id")
    private ShipperSalary salary;

    // ==================== Thông tin phạt ====================
    
    /**
     * Ngày phạt
     */
    @Column(name = "penalty_date", nullable = false)
    private LocalDate penaltyDate;

    /**
     * Loại phạt
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", length = 30, nullable = false)
    private PenaltyType penaltyType;

    // ==================== Chi tiết phạt ====================
    
    /**
     * Số phút đi trễ (nếu loại LATE)
     */
    @Builder.Default
    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    /**
     * Số card thiếu (nếu loại MISSING_PUNCH)
     */
    @Builder.Default
    @Column(name = "missing_punch_count")
    private Integer missingPunchCount = 0;

    /**
     * Số tiền phạt
     */
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Mô tả chi tiết
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ==================== Trạng thái ====================
    
    /**
     * Trạng thái phạt
     * - PENDING: Chờ xử lý
     * - APPLIED: Đã áp dụng vào lương
     * - WAIVED: Miễn phạt
     */
    @Builder.Default
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    /**
     * Admin miễn phạt
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by")
    private User waivedBy;

    /**
     * Lý do miễn phạt
     */
    @Column(name = "waived_reason", columnDefinition = "TEXT")
    private String waivedReason;

    /**
     * Thời điểm miễn phạt
     */
    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    // ==================== Timestamp ====================
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== Helper Methods ====================
    
    /**
     * Tạo khoản phạt đi trễ
     */
    public static ShipperPenalty createLatePenalty(User shipper, Attendance attendance, 
            int lateMinutes, BigDecimal penaltyPerMinute) {
        return ShipperPenalty.builder()
                .shipper(shipper)
                .attendance(attendance)
                .penaltyDate(attendance.getWorkDate())
                .penaltyType(PenaltyType.LATE)
                .lateMinutes(lateMinutes)
                .amount(penaltyPerMinute.multiply(BigDecimal.valueOf(lateMinutes)))
                .description(String.format("Đi trễ %d phút", lateMinutes))
                .status("PENDING")
                .build();
    }

    /**
     * Tạo khoản phạt thiếu chấm công
     */
    public static ShipperPenalty createMissingPunchPenalty(User shipper, Attendance attendance, 
            int missingCount, BigDecimal penaltyPerPunch) {
        return ShipperPenalty.builder()
                .shipper(shipper)
                .attendance(attendance)
                .penaltyDate(attendance.getWorkDate())
                .penaltyType(PenaltyType.MISSING_PUNCH)
                .missingPunchCount(missingCount)
                .amount(penaltyPerPunch.multiply(BigDecimal.valueOf(missingCount)))
                .description(String.format("Thiếu %d lần chấm công", missingCount))
                .status("PENDING")
                .build();
    }

    /**
     * Miễn phạt
     */
    public void waive(User admin, String reason) {
        this.status = "WAIVED";
        this.waivedBy = admin;
        this.waivedReason = reason;
        this.waivedAt = LocalDateTime.now();
    }

    /**
     * Đánh dấu đã áp dụng vào lương
     */
    public void apply(ShipperSalary salary) {
        this.status = "APPLIED";
        this.salary = salary;
    }

    /**
     * Kiểm tra đã xử lý chưa
     */
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    /**
     * Kiểm tra đã miễn phạt chưa
     */
    public boolean isWaived() {
        return "WAIVED".equals(status);
    }
}
