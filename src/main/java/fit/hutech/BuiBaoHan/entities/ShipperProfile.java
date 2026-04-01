package fit.hutech.BuiBaoHan.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import fit.hutech.BuiBaoHan.constants.ContractType;
import fit.hutech.BuiBaoHan.constants.Gender;
import fit.hutech.BuiBaoHan.constants.ShipperStatus;
import fit.hutech.BuiBaoHan.constants.VehicleType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho hồ sơ Shipper (Nhân viên giao hàng)
 * 
 * Mỗi User có role SHIPPER sẽ có 1 ShipperProfile tương ứng (1:1)
 */
@Entity
@Table(name = "shipper_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== Quan hệ với User ====================
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ==================== Thông tin cá nhân ====================
    
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    @Column(name = "full_name", length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Size(max = 15, message = "Số điện thoại không được vượt quá 15 ký tự")
    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Size(max = 20, message = "Số CCCD/CMND không được vượt quá 20 ký tự")
    @Column(name = "id_card_number", length = 20)
    private String idCardNumber;

    // ==================== Thông tin phương tiện ====================
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType;

    @Size(max = 20, message = "Biển số xe không được vượt quá 20 ký tự")
    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Size(max = 20, message = "Số bằng lái không được vượt quá 20 ký tự")
    @Column(name = "driving_license", length = 20)
    private String drivingLicense;

    // ==================== Trạng thái làm việc ====================
    
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ShipperStatus status = ShipperStatus.OFFLINE;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    // ==================== Thống kê hiệu suất ====================
    
    @Builder.Default
    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Builder.Default
    @Column(name = "successful_deliveries")
    private Integer successfulDeliveries = 0;

    @Builder.Default
    @Column(name = "failed_deliveries")
    private Integer failedDeliveries = 0;

    @Builder.Default
    @DecimalMin(value = "1.00", message = "Rating tối thiểu là 1.00")
    @DecimalMax(value = "5.00", message = "Rating tối đa là 5.00")
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating = new BigDecimal("5.00");

    // ==================== Thông tin hợp đồng ====================
    
    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", length = 20)
    private ContractType contractType;

    // ==================== Bảo hiểm ====================
    
    @Size(max = 20, message = "Số BHXH không được vượt quá 20 ký tự")
    @Column(name = "social_insurance_number", length = 20)
    private String socialInsuranceNumber;

    @Size(max = 20, message = "Số BHYT không được vượt quá 20 ký tự")
    @Column(name = "health_insurance_number", length = 20)
    private String healthInsuranceNumber;

    @Size(max = 20, message = "Số BHTN không được vượt quá 20 ký tự")
    @Column(name = "unemployment_insurance_number", length = 20)
    private String unemploymentInsuranceNumber;

    @Size(max = 20, message = "Mã số thuế không được vượt quá 20 ký tự")
    @Column(name = "tax_code", length = 20)
    private String taxCode;

    // ==================== Thông tin ngân hàng ====================

    @Size(max = 100, message = "Tên ngân hàng không được vượt quá 100 ký tự")
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Size(max = 30, message = "Số tài khoản không được vượt quá 30 ký tự")
    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Size(max = 100, message = "Tên chủ tài khoản không được vượt quá 100 ký tự")
    @Column(name = "bank_account_holder", length = 100)
    private String bankAccountHolder;

    // ==================== Thông tin bổ sung ====================

    @Size(max = 255, message = "URL avatar không được vượt quá 255 ký tự")
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "join_date")
    private LocalDate joinDate;

    // ==================== Timestamps ====================
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== Quan hệ với các entity khác ====================
    
    @Builder.Default
    @OneToMany(mappedBy = "shipper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Attendance> attendances = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "shipper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ShipperSalary> salaries = new ArrayList<>();

    // ==================== Helper Methods ====================
    
    /**
     * Tính tỷ lệ giao hàng thành công
     */
    public BigDecimal getSuccessRate() {
        if (totalDeliveries == null || totalDeliveries == 0) {
            return BigDecimal.ZERO;
        }
        int successful = java.util.Objects.requireNonNullElse(successfulDeliveries, 0);
        return BigDecimal.valueOf(successful * 100.0 / totalDeliveries)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Cập nhật thống kê sau khi giao hàng
     */
    public void recordDelivery(boolean success) {
        if (totalDeliveries == null) totalDeliveries = 0;
        if (successfulDeliveries == null) successfulDeliveries = 0;
        if (failedDeliveries == null) failedDeliveries = 0;

        totalDeliveries++;
        if (success) {
            successfulDeliveries++;
        } else {
            failedDeliveries++;
        }
    }

    /**
     * Kiểm tra shipper có đang online không
     */
    public boolean isOnline() {
        return status == ShipperStatus.ONLINE || status == ShipperStatus.BUSY;
    }

    /**
     * Kiểm tra shipper có thể nhận đơn mới không
     */
    public boolean canAcceptOrder() {
        return Boolean.TRUE.equals(isActive) && status == ShipperStatus.ONLINE;
    }

    /**
     * Lấy số đơn giao thành công (alias for successfulDeliveries)
     */
    public Integer getCompletedDeliveries() {
        return java.util.Objects.requireNonNullElse(successfulDeliveries, 0);
    }
}
