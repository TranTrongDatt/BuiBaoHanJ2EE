package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.SalaryStatus;
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
 * Entity đại diện cho bảng lương tháng của Shipper
 * 
 * Công thức tính lương:
 * 
 * THU NHẬP:
 * - Lương cơ bản = Tổng giờ làm × 30,000 VND/giờ
 * - Lương tăng ca = Tổng giờ OT × 60,000 VND/giờ (x2)
 * - Phụ cấp xăng = 500,000 VND/tháng (cố định)
 * - Phụ cấp ăn = 100,000 VND × số ngày đủ điều kiện
 * 
 * KHẤU TRỪ:
 * - BHXH = 8% lương cơ bản
 * - BHYT = 1.5% lương cơ bản
 * - BHTN = 1% lương cơ bản
 * - Phạt đi trễ = Tổng phút trễ × 8,000 VND
 * - Phạt thiếu chấm công = Số card thiếu × 8,000 VND
 * 
 * NET = THU NHẬP - KHẤU TRỪ
 */
@Entity
@Table(name = "shipper_salary", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shipper_id", "salary_month", "salary_year"}, 
                      name = "uk_salary_shipper_month_year")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Quan hệ ====================
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id", nullable = false)
    private ShipperProfile shipper;

    // ==================== Kỳ lương ====================
    
    /**
     * Tháng (1-12)
     */
    @Column(name = "salary_month", nullable = false)
    private Integer salaryMonth;

    /**
     * Năm
     */
    @Column(name = "salary_year", nullable = false)
    private Integer salaryYear;

    /**
     * Ngày bắt đầu kỳ
     */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /**
     * Ngày kết thúc kỳ
     */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // ==================== Thống kê giờ làm ====================
    
    /**
     * Tổng số ngày làm việc
     */
    @Builder.Default
    @Column(name = "total_work_days")
    private Integer totalWorkDays = 0;

    /**
     * Tổng giờ làm (không tính OT)
     */
    @Builder.Default
    @Column(name = "total_work_hours", precision = 6, scale = 2)
    private BigDecimal totalWorkHours = BigDecimal.ZERO;

    /**
     * Tổng giờ tăng ca
     */
    @Builder.Default
    @Column(name = "total_overtime_hours", precision = 6, scale = 2)
    private BigDecimal totalOvertimeHours = BigDecimal.ZERO;

    // ==================== THU NHẬP ====================
    
    /**
     * Lương cơ bản = giờ × 30,000
     */
    @Builder.Default
    @Column(name = "base_salary", precision = 12, scale = 2)
    private BigDecimal baseSalary = BigDecimal.ZERO;

    /**
     * Lương tăng ca = giờ OT × 60,000
     */
    @Builder.Default
    @Column(name = "overtime_salary", precision = 12, scale = 2)
    private BigDecimal overtimeSalary = BigDecimal.ZERO;

    /**
     * Thưởng giao hàng
     */
    @Builder.Default
    @Column(name = "delivery_bonus", precision = 12, scale = 2)
    private BigDecimal deliveryBonus = BigDecimal.ZERO;

    // ==================== PHỤ CẤP ====================
    
    /**
     * Phụ cấp xăng = 500,000 VND/tháng
     */
    @Builder.Default
    @Column(name = "gas_allowance", precision = 12, scale = 2)
    private BigDecimal gasAllowance = BigDecimal.ZERO;

    /**
     * Phụ cấp ăn trưa = tổng tháng
     */
    @Builder.Default
    @Column(name = "meal_allowance", precision = 12, scale = 2)
    private BigDecimal mealAllowance = BigDecimal.ZERO;

    /**
     * Phụ cấp khác
     */
    @Builder.Default
    @Column(name = "other_allowance", precision = 12, scale = 2)
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    /**
     * TỔNG THU NHẬP (trước khấu trừ)
     */
    @Builder.Default
    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    // ==================== KHẤU TRỪ BẢO HIỂM ====================
    
    /**
     * BHXH = 8% lương cơ bản
     */
    @Builder.Default
    @Column(name = "social_insurance", precision = 12, scale = 2)
    private BigDecimal socialInsurance = BigDecimal.ZERO;

    /**
     * BHYT = 1.5% lương cơ bản
     */
    @Builder.Default
    @Column(name = "health_insurance", precision = 12, scale = 2)
    private BigDecimal healthInsurance = BigDecimal.ZERO;

    /**
     * BHTN = 1% lương cơ bản
     */
    @Builder.Default
    @Column(name = "unemployment_insurance", precision = 12, scale = 2)
    private BigDecimal unemploymentInsurance = BigDecimal.ZERO;

    /**
     * Thuế TNCN (nếu có)
     */
    @Builder.Default
    @Column(name = "personal_income_tax", precision = 12, scale = 2)
    private BigDecimal personalIncomeTax = BigDecimal.ZERO;

    // ==================== KHẤU TRỪ PHẠT ====================
    
    /**
     * Phạt đi trễ = tổng phút × 8,000
     */
    @Builder.Default
    @Column(name = "penalty_late", precision = 12, scale = 2)
    private BigDecimal penaltyLate = BigDecimal.ZERO;

    /**
     * Phạt thiếu chấm công = số card × 8,000
     */
    @Builder.Default
    @Column(name = "penalty_missing_punch", precision = 12, scale = 2)
    private BigDecimal penaltyMissingPunch = BigDecimal.ZERO;

    /**
     * Khấu trừ khác
     */
    @Builder.Default
    @Column(name = "other_deduction", precision = 12, scale = 2)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    /**
     * TỔNG KHẤU TRỪ
     */
    @Builder.Default
    @Column(name = "total_deduction", precision = 12, scale = 2)
    private BigDecimal totalDeduction = BigDecimal.ZERO;

    // ==================== NET ====================
    
    /**
     * LƯƠNG THỰC NHẬN = gross - deduction
     */
    @Builder.Default
    @Column(name = "net_salary", precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    // ==================== Trạng thái ====================
    
    /**
     * Trạng thái bảng lương
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SalaryStatus status = SalaryStatus.PENDING;

    /**
     * Admin duyệt lương
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    /**
     * Thời điểm duyệt
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Thời điểm thanh toán
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /**
     * Phương thức thanh toán
     */
    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    /**
     * Mã giao dịch thanh toán
     */
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

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

    // ==================== Quan hệ với Penalty ====================
    
    @Builder.Default
    @OneToMany(mappedBy = "salary", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipperPenalty> penalties = new ArrayList<>();

    // ==================== Helper Methods ====================
    
    /**
     * Tính gross salary
     */
    public void calculateGrossSalary() {
        this.grossSalary = BigDecimal.ZERO
                .add(baseSalary != null ? baseSalary : BigDecimal.ZERO)
                .add(overtimeSalary != null ? overtimeSalary : BigDecimal.ZERO)
                .add(deliveryBonus != null ? deliveryBonus : BigDecimal.ZERO)
                .add(gasAllowance != null ? gasAllowance : BigDecimal.ZERO)
                .add(mealAllowance != null ? mealAllowance : BigDecimal.ZERO)
                .add(otherAllowance != null ? otherAllowance : BigDecimal.ZERO);
    }

    /**
     * Tính total deduction
     */
    public void calculateTotalDeduction() {
        this.totalDeduction = BigDecimal.ZERO
                .add(socialInsurance != null ? socialInsurance : BigDecimal.ZERO)
                .add(healthInsurance != null ? healthInsurance : BigDecimal.ZERO)
                .add(unemploymentInsurance != null ? unemploymentInsurance : BigDecimal.ZERO)
                .add(personalIncomeTax != null ? personalIncomeTax : BigDecimal.ZERO)
                .add(penaltyLate != null ? penaltyLate : BigDecimal.ZERO)
                .add(penaltyMissingPunch != null ? penaltyMissingPunch : BigDecimal.ZERO)
                .add(otherDeduction != null ? otherDeduction : BigDecimal.ZERO);
    }

    /**
     * Tính net salary
     */
    public void calculateNetSalary() {
        calculateGrossSalary();
        calculateTotalDeduction();
        this.netSalary = this.grossSalary.subtract(this.totalDeduction);
    }

    /**
     * Duyệt bảng lương
     */
    public void approve(User admin) {
        this.status = SalaryStatus.APPROVED;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Đánh dấu đã thanh toán
     */
    public void markAsPaid(String method, String reference) {
        this.status = SalaryStatus.PAID;
        this.paymentMethod = method;
        this.paymentReference = reference;
        this.paidAt = LocalDateTime.now();
    }

    /**
     * Từ chối bảng lương
     */
    public void reject(String reason) {
        this.status = SalaryStatus.REJECTED;
        this.notes = reason;
    }

    /**
     * Lấy tên tháng/năm
     */
    public String getPeriodName() {
        return String.format("Tháng %d/%d", salaryMonth, salaryYear);
    }
}
