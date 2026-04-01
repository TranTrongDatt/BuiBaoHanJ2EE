package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import fit.hutech.BuiBaoHan.constants.PunchType;
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
 * Entity đại diện cho mỗi lần chấm công (bấm thẻ) của Shipper
 * 
 * Quy định chấm công:
 * - Tối thiểu 2 lần: CHECK_IN (vào ca) + CHECK_OUT (ra ca) → Bắt buộc
 * - Tối đa 4 lần: thêm BREAK_START (vào nghỉ) + BREAK_END (ra nghỉ) → Tùy chọn
 * 
 * Phạt:
 * - Thiếu 1 card: 8,000 VND
 * - Đi trễ: 8,000 VND/phút
 */
@Entity
@Table(name = "attendance_punch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendancePunch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Quan hệ ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    // ==================== Thông tin chấm công ====================
    
    /**
     * Loại chấm công
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "punch_type", length = 20, nullable = false)
    private PunchType punchType;

    /**
     * Thời điểm chấm công
     */
    @Column(name = "punch_time", nullable = false)
    private LocalDateTime punchTime;

    // ==================== Vị trí (cho mobile app) ====================
    
    /**
     * Vĩ độ (latitude)
     */
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    /**
     * Kinh độ (longitude)
     */
    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /**
     * Ghi chú vị trí
     */
    @Column(name = "location_note", length = 255)
    private String locationNote;

    // ==================== Trạng thái trễ ====================
    
    /**
     * Có đi trễ không (so với giờ quy định)
     */
    @Builder.Default
    @Column(name = "is_late")
    private Boolean isLate = false;

    /**
     * Số phút đi trễ
     */
    @Builder.Default
    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    // ==================== Xác thực (Admin) ====================
    
    /**
     * Đã được xác thực chưa
     */
    @Builder.Default
    @Column(name = "verified")
    private Boolean verified = false;

    /**
     * Admin xác thực
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    /**
     * Thời điểm xác thực
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    // ==================== Ghi chú ====================
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ==================== Timestamp ====================
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ==================== Helper Methods ====================
    
    /**
     * Kiểm tra có phải punch bắt buộc không
     */
    public boolean isRequiredPunch() {
        return punchType == PunchType.CHECK_IN || punchType == PunchType.CHECK_OUT;
    }

    /**
     * Kiểm tra có phải punch nghỉ trưa không
     */
    public boolean isBreakPunch() {
        return punchType == PunchType.BREAK_START || punchType == PunchType.BREAK_END;
    }

    /**
     * Verify punch bởi admin
     */
    public void verify(User admin) {
        this.verified = true;
        this.verifiedBy = admin;
        this.verifiedAt = LocalDateTime.now();
    }

    /**
     * Format thời gian chấm công
     */
    public String getFormattedPunchTime() {
        if (punchTime == null) return "";
        return punchTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
