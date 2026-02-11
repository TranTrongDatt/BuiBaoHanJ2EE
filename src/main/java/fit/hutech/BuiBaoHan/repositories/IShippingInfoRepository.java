package fit.hutech.BuiBaoHan.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.constants.ShippingStatus;
import fit.hutech.BuiBaoHan.entities.ShippingInfo;

/**
 * Repository cho ShippingInfo entity
 */
@Repository
public interface IShippingInfoRepository extends JpaRepository<ShippingInfo, Long> {

    Optional<ShippingInfo> findByOrderId(Long orderId);

    Optional<ShippingInfo> findByTrackingNumber(String trackingNumber);

    List<ShippingInfo> findByStatus(ShippingStatus status);

    List<ShippingInfo> findByStatusIn(List<ShippingStatus> statuses);

    @Query("SELECT COUNT(s) FROM ShippingInfo s WHERE s.status = :status")
    long countByStatus(@Param("status") ShippingStatus status);

    List<ShippingInfo> findByEstimatedDeliveryDate(LocalDate date);

    @Query("SELECT s FROM ShippingInfo s WHERE s.estimatedDeliveryDate < :date AND s.status NOT IN ('DELIVERED', 'RETURNED')")
    List<ShippingInfo> findOverdueShipments(@Param("date") LocalDate date);

    @Query("SELECT s FROM ShippingInfo s WHERE s.carrier = :carrier AND s.status = :status")
    List<ShippingInfo> findByCarrierAndStatus(@Param("carrier") String carrier, @Param("status") ShippingStatus status);
}
