package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.ShippingStatus;
import fit.hutech.BuiBaoHan.constants.ShippingType;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.ShippingInfo;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IShippingInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Vận chuyển
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShippingService {

    private final IShippingInfoRepository shippingInfoRepository;
    private final IOrderRepository orderRepository;

    // Phí vận chuyển cơ bản
    private static final BigDecimal BASE_STANDARD_FEE = new BigDecimal("30000");
    private static final BigDecimal BASE_EXPRESS_FEE = new BigDecimal("50000");
    private static final BigDecimal BASE_SAME_DAY_FEE = new BigDecimal("80000");

    /**
     * Tìm shipping info theo ID
     */
    @Transactional(readOnly = true)
    public Optional<ShippingInfo> getShippingById(Long id) {
        return shippingInfoRepository.findById(id);
    }

    /**
     * Lấy shipping info của order
     */
    @Transactional(readOnly = true)
    public Optional<ShippingInfo> getShippingByOrderId(Long orderId) {
        return shippingInfoRepository.findByOrderId(orderId);
    }

    /**
     * Tìm theo mã vận đơn
     */
    @Transactional(readOnly = true)
    public Optional<ShippingInfo> getShippingByTrackingNumber(String trackingNumber) {
        return shippingInfoRepository.findByTrackingNumber(trackingNumber);
    }

    /**
     * Tạo shipping info cho order
     */
    public ShippingInfo createShipping(Long orderId, String carrier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        // Kiểm tra đã có shipping chưa
        if (shippingInfoRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("Đơn hàng đã có thông tin vận chuyển");
        }

        // Tính ngày giao dự kiến
        LocalDate estimatedDelivery = calculateEstimatedDelivery(order.getShippingType());

        ShippingInfo shipping = ShippingInfo.builder()
                .order(order)
                .trackingNumber(generateTrackingNumber())
                .carrier(carrier)
                .shippingType(order.getShippingType())
                .shippingFee(order.getShippingFee())
                .status(ShippingStatus.PENDING)
                .senderAddress("Kho hàng MiniVerse - 123 Nguyễn Văn Cừ, Q.5, TP.HCM")
                .receiverAddress(order.getShippingAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .estimatedDeliveryDate(estimatedDelivery)
                .createdAt(LocalDateTime.now())
                .build();

        ShippingInfo saved = shippingInfoRepository.save(shipping);
        log.info("Created shipping {} for order {}", saved.getTrackingNumber(), order.getOrderCode());
        return saved;
    }

    /**
     * Cập nhật trạng thái vận chuyển
     */
    public ShippingInfo updateShippingStatus(Long shippingId, ShippingStatus newStatus, String note) {
        ShippingInfo shipping = shippingInfoRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipping ID: " + shippingId));

        ShippingStatus oldStatus = shipping.getStatus();
        shipping.setStatus(newStatus);
        
        // Thêm note vào lịch sử
        String historyEntry = LocalDateTime.now() + " - " + newStatus + ": " + note;
        String currentHistory = shipping.getStatusHistory() != null ? shipping.getStatusHistory() : "";
        shipping.setStatusHistory(currentHistory + "\n" + historyEntry);

        // Cập nhật thời gian
        switch (newStatus) {
            case PICKED_UP -> shipping.setPickedUpAt(LocalDateTime.now());
            case IN_TRANSIT -> shipping.setInTransitAt(LocalDateTime.now());
            case OUT_FOR_DELIVERY -> shipping.setOutForDeliveryAt(LocalDateTime.now());
            case DELIVERED -> {
                shipping.setActualDeliveryDate(LocalDate.now());
                // Cập nhật order status
                Order order = shipping.getOrder();
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredDate(LocalDateTime.now());
                orderRepository.save(order);
            }
            case FAILED -> shipping.setFailedAt(LocalDateTime.now());
            case RETURNED -> shipping.setReturnedAt(LocalDateTime.now());
            default -> {}
        }

        ShippingInfo updated = shippingInfoRepository.save(shipping);
        log.info("Updated shipping {} status from {} to {}", shippingId, oldStatus, newStatus);
        return updated;
    }

    /**
     * Cập nhật vị trí hiện tại
     */
    public ShippingInfo updateCurrentLocation(Long shippingId, String location) {
        ShippingInfo shipping = shippingInfoRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipping ID: " + shippingId));

        shipping.setCurrentLocation(location);
        shipping.setLastLocationUpdate(LocalDateTime.now());

        ShippingInfo updated = shippingInfoRepository.save(shipping);
        log.info("Updated shipping {} location to {}", shippingId, location);
        return updated;
    }

    /**
     * Đánh dấu giao hàng thất bại
     */
    public ShippingInfo markDeliveryFailed(Long shippingId, String reason) {
        ShippingInfo shipping = shippingInfoRepository.findById(shippingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy shipping ID: " + shippingId));

        Integer deliveryAttempts = shipping.getDeliveryAttempts();
        int attempts = deliveryAttempts != null ? deliveryAttempts : 0;
        shipping.setDeliveryAttempts(attempts + 1);
        shipping.setStatus(ShippingStatus.FAILED);
        shipping.setFailedReason(reason);
        shipping.setFailedAt(LocalDateTime.now());

        ShippingInfo updated = shippingInfoRepository.save(shipping);
        log.info("Marked shipping {} as failed, attempt {}: {}", shippingId, attempts + 1, reason);
        return updated;
    }

    /**
     * Tính phí vận chuyển
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateShippingFee(ShippingType type, String province, BigDecimal orderTotal) {
        BigDecimal baseFee = switch (type) {
            case STANDARD -> BASE_STANDARD_FEE;
            case EXPRESS -> BASE_EXPRESS_FEE;
            case SAME_DAY -> BASE_SAME_DAY_FEE;
            case ECONOMY -> new BigDecimal("15000");
            case PICKUP -> BigDecimal.ZERO;
        };

        // Phụ phí theo khu vực
        BigDecimal areaSurcharge = calculateAreaSurcharge(province);
        
        // Free ship cho đơn >= 500k (chỉ với Standard)
        BigDecimal freeShipThreshold = new BigDecimal("500000");
        if (type == ShippingType.STANDARD && orderTotal.compareTo(freeShipThreshold) >= 0) {
            return BigDecimal.ZERO;
        }

        return baseFee.add(areaSurcharge);
    }

    /**
     * Lấy danh sách giao trong ngày
     */
    @Transactional(readOnly = true)
    public List<ShippingInfo> getTodayDeliveries() {
        return shippingInfoRepository.findByEstimatedDeliveryDate(LocalDate.now());
    }

    /**
     * Lấy danh sách chưa lấy hàng
     */
    @Transactional(readOnly = true)
    public List<ShippingInfo> getPendingPickups() {
        return shippingInfoRepository.findByStatus(ShippingStatus.PENDING);
    }

    /**
     * Lấy danh sách đang giao
     */
    @Transactional(readOnly = true)
    public List<ShippingInfo> getInTransitShipments() {
        return shippingInfoRepository.findByStatusIn(
                List.of(ShippingStatus.PICKED_UP, ShippingStatus.IN_TRANSIT, ShippingStatus.OUT_FOR_DELIVERY));
    }

    /**
     * Thống kê vận chuyển
     */
    @Transactional(readOnly = true)
    public long countByStatus(ShippingStatus status) {
        return shippingInfoRepository.countByStatus(status);
    }

    // ==================== Private Helper Methods ====================

    private String generateTrackingNumber() {
        String carrier = "MV"; // MiniVerse
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return carrier + timestamp + random;
    }

    private LocalDate calculateEstimatedDelivery(ShippingType type) {
        LocalDate today = LocalDate.now();
        return switch (type) {
            case SAME_DAY -> today;
            case EXPRESS -> today.plusDays(1);
            case STANDARD -> today.plusDays(3);
            case ECONOMY -> today.plusDays(7);
            case PICKUP -> today.plusDays(1);
        };
    }

    private BigDecimal calculateAreaSurcharge(String province) {
        if (province == null) return BigDecimal.ZERO;
        
        // Miền Nam - Không phụ phí
        List<String> southProvinces = List.of("TP.HCM", "Hồ Chí Minh", "Bình Dương", "Đồng Nai", "Long An");
        if (southProvinces.stream().anyMatch(province::contains)) {
            return BigDecimal.ZERO;
        }
        
        // Miền Trung - Phụ phí 15k
        List<String> centralProvinces = List.of("Đà Nẵng", "Huế", "Quảng Nam", "Bình Định", "Nha Trang");
        if (centralProvinces.stream().anyMatch(province::contains)) {
            return new BigDecimal("15000");
        }
        
        // Miền Bắc và các tỉnh khác - Phụ phí 30k
        return new BigDecimal("30000");
    }

    // ==================== Admin Controller Support Methods ====================

    /**
     * Lấy danh sách các nhà vận chuyển khả dụng
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableProviders() {
        return List.of("GHTK", "GHN", "ViettelPost", "J&T Express", "Ninja Van", "Kerry Express");
    }

    /**
     * Cập nhật nhà vận chuyển cho đơn hàng
     */
    public void updateProvider(Long orderId, String provider) {
        shippingInfoRepository.findByOrderId(orderId).ifPresent(shipping -> {
            shipping.setCarrier(provider);
            shippingInfoRepository.save(shipping);
            log.info("Updated shipping provider for order {} to {}", orderId, provider);
        });
    }
}
