package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.ContractType;
import fit.hutech.BuiBaoHan.constants.ShipperStatus;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho ShipperProfile entity
 */
@Repository
public interface IShipperProfileRepository extends JpaRepository<ShipperProfile, Long> {

    /**
     * Tìm profile theo user
     */
    Optional<ShipperProfile> findByUser(User user);

    /**
     * Tìm profile theo user ID
     */
    Optional<ShipperProfile> findByUserId(Long userId);

    /**
     * Kiểm tra user đã có profile shipper chưa
     */
    boolean existsByUserId(Long userId);

    /**
     * Tìm profile theo số điện thoại
     */
    Optional<ShipperProfile> findByPhone(String phone);

    /**
     * Kiểm tra số điện thoại đã tồn tại
     */
    boolean existsByPhone(String phone);

    /**
     * Kiểm tra CCCD đã tồn tại
     */
    boolean existsByIdCardNumber(String idCardNumber);

    /**
     * Tìm profile theo biển số xe
     */
    Optional<ShipperProfile> findByLicensePlate(String licensePlate);

    /**
     * Kiểm tra biển số xe đã tồn tại
     */
    boolean existsByLicensePlate(String licensePlate);

    /**
     * Tìm tất cả shipper theo trạng thái
     */
    List<ShipperProfile> findByStatus(ShipperStatus status);

    /**
     * Tìm shipper theo trạng thái với phân trang
     */
    Page<ShipperProfile> findByStatus(ShipperStatus status, Pageable pageable);

    /**
     * Tìm shipper theo loại hợp đồng
     */
    List<ShipperProfile> findByContractType(ContractType contractType);

    /**
     * Đếm số shipper theo trạng thái
     */
    long countByStatus(ShipperStatus status);

    /**
     * Tìm shipper đang online và có thể nhận đơn
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.status = 'ONLINE' AND sp.isActive = true " +
           "ORDER BY sp.rating DESC, sp.successfulDeliveries DESC")
    List<ShipperProfile> findAvailableShippers();

    /**
     * Tìm shipper đang online với phân trang
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.status = 'ONLINE' AND sp.isActive = true")
    Page<ShipperProfile> findAvailableShippers(Pageable pageable);

    /**
     * Tìm shipper theo rating >= giá trị cho trước
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.rating >= :minRating AND sp.isActive = true " +
           "ORDER BY sp.rating DESC")
    List<ShipperProfile> findByMinRating(@Param("minRating") Double minRating);

    /**
     * Tìm top shipper theo số đơn hoàn thành
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.isActive = true " +
           "ORDER BY sp.successfulDeliveries DESC")
    List<ShipperProfile> findTopShippersByDeliveries(Pageable pageable);

    /**
     * Tìm top shipper theo rating
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.isActive = true " +
           "ORDER BY sp.rating DESC")
    List<ShipperProfile> findTopShippersByRating(Pageable pageable);

    /**
     * Tìm shipper đang active
     */
    List<ShipperProfile> findByIsActiveTrue();

    /**
     * Đếm shipper active
     */
    long countByIsActiveTrue();

    /**
     * Tìm kiếm shipper theo keyword (tên, số điện thoại, biển số)
     */
    @Query("SELECT sp FROM ShipperProfile sp WHERE " +
           "LOWER(sp.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "sp.phone LIKE CONCAT('%', :keyword, '%') OR " +
           "LOWER(sp.licensePlate) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ShipperProfile> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Tính tổng số đơn hoàn thành của tất cả shipper
     */
    @Query("SELECT COALESCE(SUM(sp.successfulDeliveries), 0) FROM ShipperProfile sp WHERE sp.isActive = true")
    Long getTotalCompletedDeliveries();

    /**
     * Tính trung bình rating của tất cả shipper
     */
    @Query("SELECT AVG(sp.rating) FROM ShipperProfile sp WHERE sp.isActive = true AND sp.rating > 0")
    Double getAverageRating();
}
