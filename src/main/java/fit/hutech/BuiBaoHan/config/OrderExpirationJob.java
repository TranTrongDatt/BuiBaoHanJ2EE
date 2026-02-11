package fit.hutech.BuiBaoHan.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.services.NotificationService;
import fit.hutech.BuiBaoHan.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to auto-cancel expired BANK_TRANSFER orders
 * that haven't been paid within the configured time limit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationJob {

    private final IOrderRepository orderRepository;
    private final OrderService orderService;
    private final NotificationService notificationService;

    @Value("${payment.order-expiration-minutes:30}")
    private int expirationMinutes;

    /**
     * Run every 5 minutes to check for expired bank transfer orders
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cancelExpiredBankTransferOrders() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(expirationMinutes);
        List<Order> expiredOrders = orderRepository.findExpiredBankTransferOrders(expirationTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired bank transfer orders to cancel", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                String reason = "Tự động hủy: Quá hạn thanh toán chuyển khoản (" + expirationMinutes + " phút)";
                orderService.cancelOrder(order.getId(), reason);

                // Notify user
                notificationService.sendOrderExpiredNotification(order.getUser().getId(), order);

                log.info("Auto-cancelled expired order {} (created at {})", 
                        order.getOrderCode(), order.getCreatedAt());
            } catch (Exception e) {
                log.error("Failed to auto-cancel order {}: {}", order.getOrderCode(), e.getMessage());
            }
        }
    }
}
