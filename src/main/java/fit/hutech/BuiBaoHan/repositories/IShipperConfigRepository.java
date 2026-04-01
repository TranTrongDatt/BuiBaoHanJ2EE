package fit.hutech.BuiBaoHan.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.ShipperConfig;

/**
 * Repository cho ShipperConfig entity (singleton config)
 */
@Repository
public interface IShipperConfigRepository extends JpaRepository<ShipperConfig, Long> {

    /**
     * Lấy config hiện tại (singleton, chỉ có 1 record)
     */
    @Query("SELECT sc FROM ShipperConfig sc WHERE sc.isActive = true ORDER BY sc.id DESC")
    Optional<ShipperConfig> findCurrentConfig();

    /**
     * Alias cho findCurrentConfig
     */
    default ShipperConfig getConfig() {
        return findCurrentConfig().orElseGet(ShipperConfig::createDefault);
    }

    /**
     * Kiểm tra đã có config chưa
     */
    boolean existsByIsActiveTrue();

    /**
     * Đếm số config active
     */
    long countByIsActiveTrue();
}
