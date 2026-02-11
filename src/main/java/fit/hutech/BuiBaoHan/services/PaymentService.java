package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.constants.PaymentStatus;
import fit.hutech.BuiBaoHan.dto.PaymentRequest;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.Payment;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý Thanh toán
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final IPaymentRepository paymentRepository;
    private final IOrderRepository orderRepository;

    /**
     * Inner record cho kết quả thanh toán
     */
    public record PaymentResult(
        boolean success,
        String message,
        Payment payment,
        String redirectUrl,
        Long orderId,
        String orderNumber,
        String transactionId
    ) {
        public static PaymentResult success(Payment payment, String redirectUrl) {
            return new PaymentResult(
                true, "Thanh toán thành công", payment, redirectUrl,
                payment != null && payment.getOrder() != null ? payment.getOrder().getId() : null,
                payment != null && payment.getOrder() != null ? payment.getOrder().getOrderCode() : null,
                payment != null ? payment.getTransactionId() : null
            );
        }
        
        public static PaymentResult failure(String message) {
            return new PaymentResult(false, message, null, null, null, null, null);
        }
    }

    /**
     * Tìm payment theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * Tìm payment theo transaction ID
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    /**
     * Lấy payment của order
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream().findFirst();
    }

    /**
     * Khởi tạo thanh toán
     */
    public Payment initiatePayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + request.orderId()));

        // Kiểm tra đã thanh toán chưa
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Đơn hàng đã được thanh toán");
        }

        // Kiểm tra có payment pending không
        Optional<Payment> existingPayment = paymentRepository.findByOrderIdAndStatus(
                request.orderId(), PaymentStatus.PENDING);
        if (existingPayment.isPresent()) {
            // Hủy payment cũ
            existingPayment.get().setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(existingPayment.get());
        }

        // Tạo payment mới
        String transactionId = generateTransactionId();
        
        Payment payment = Payment.builder()
                .order(order)
                .transactionId(transactionId)
                .amount(request.amount() != null ? request.amount() : order.getTotalAmount())
                .paymentMethod(request.paymentMethod().name())
                .status(PaymentStatus.PENDING)
                .returnUrl(request.returnUrl())
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // 30 phút hết hạn
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Initiated payment {} for order {}", transactionId, order.getOrderCode());
        return saved;
    }

    /**
     * Xử lý callback thanh toán thành công
     */
    public Payment processSuccessCallback(String transactionId, Map<String, String> callbackData) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment: " + transactionId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment không ở trạng thái chờ xử lý");
        }

        // Kiểm tra hết hạn
        if (LocalDateTime.now().isAfter(payment.getExpiresAt())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw new IllegalStateException("Payment đã hết hạn");
        }

        // Cập nhật payment
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayTransactionId(callbackData.get("gatewayTransactionId"));
        payment.setGatewayResponse(callbackData.toString());

        Payment updated = paymentRepository.save(payment);

        // Cập nhật order
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaidDate(LocalDateTime.now());
        order.setTransactionId(transactionId);
        orderRepository.save(order);

        log.info("Payment {} completed for order {}", transactionId, order.getOrderCode());
        return updated;
    }

    /**
     * Xử lý callback thanh toán thất bại
     */
    public Payment processFailCallback(String transactionId, String reason) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment: " + transactionId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedReason(reason);

        Payment updated = paymentRepository.save(payment);
        log.info("Payment {} failed: {}", transactionId, reason);
        return updated;
    }

    /**
     * Hủy payment
     */
    public Payment cancelPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment: " + transactionId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể hủy payment đang chờ xử lý");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment cancelled = paymentRepository.save(payment);
        log.info("Cancelled payment {}", transactionId);
        return cancelled;
    }

    /**
     * Hoàn tiền
     */
    public Payment refundPayment(Long paymentId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy payment ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Chỉ có thể hoàn tiền payment đã thanh toán");
        }

        // Validate số tiền hoàn
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được lớn hơn số tiền đã thanh toán");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(refundAmount);
        payment.setRefundedAt(LocalDateTime.now());
        payment.setRefundReason(reason);

        Payment refunded = paymentRepository.save(payment);

        // Cập nhật order
        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);

        log.info("Refunded {} for payment {}", refundAmount, paymentId);
        return refunded;
    }

    /**
     * Tạo URL thanh toán (Mock - cần tích hợp payment gateway thực tế)
     */
    public String createPaymentUrl(Payment payment) {
        PaymentMethod method = PaymentMethod.valueOf(payment.getPaymentMethod());
        return switch (method) {
            case VNPAY -> createVnPayUrl(payment);
            case MOMO -> createMomoUrl(payment);
            case ZALOPAY -> createZaloPayUrl(payment);
            case BANK_TRANSFER -> createBankTransferInfo(payment);
            default -> null;
        };
    }

    /**
     * Kiểm tra trạng thái thanh toán
     */
    @Transactional(readOnly = true)
    public PaymentStatus checkPaymentStatus(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .map(Payment::getStatus)
                .orElse(null);
    }

    /**
     * Xử lý payment hết hạn
     */
    public void processExpiredPayments() {
        paymentRepository.findExpiredPendingPayments()
                .forEach(payment -> {
                    payment.setStatus(PaymentStatus.EXPIRED);
                    paymentRepository.save(payment);
                    log.info("Expired payment {}", payment.getTransactionId());
                });
    }

    // ==================== Private Helper Methods ====================

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String createVnPayUrl(Payment payment) {
        // TODO: Tích hợp VNPay API thực tế
        // Mock URL
        return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" +
                "?vnp_TxnRef=" + payment.getTransactionId() +
                "&vnp_Amount=" + payment.getAmount().multiply(new BigDecimal("100")).intValue() +
                "&vnp_ReturnUrl=" + payment.getReturnUrl();
    }

    private String createMomoUrl(Payment payment) {
        // TODO: Tích hợp MoMo API thực tế
        return "https://test-payment.momo.vn/v2/gateway/pay" +
                "?orderId=" + payment.getTransactionId() +
                "&amount=" + payment.getAmount().intValue();
    }

    private String createZaloPayUrl(Payment payment) {
        // TODO: Tích hợp ZaloPay API thực tế
        return "https://sandbox.zalopay.com.vn/v001/tpe/createorder" +
                "?apptransid=" + payment.getTransactionId() +
                "&amount=" + payment.getAmount().intValue();
    }

    private String createBankTransferInfo(Payment payment) {
        // Trả về thông tin chuyển khoản
        return """
                Ngân hàng: Vietcombank
                Số TK: 1234567890
                Chủ TK: CÔNG TY ABC
                Nội dung: %s""".formatted(payment.getTransactionId());
    }

    // ==================== ApiPaymentController Support Methods ====================

    /**
     * Xử lý VNPay callback
     */
    public PaymentResult processVNPayCallback(Map<String, String> params) {
        String transactionId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        if (transactionId == null) {
            return PaymentResult.failure("Thiếu tham số transaction reference");
        }
        
        try {
            if ("00".equals(responseCode)) {
                // Thành công
                Payment payment = processSuccessCallback(transactionId, params);
                return PaymentResult.success(payment, null);
            } else {
                // Thất bại
                String reason = "VNPay error code: " + responseCode;
                processFailCallback(transactionId, reason);
                return PaymentResult.failure(reason);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý VNPay callback", e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    /**
     * Xử lý thanh toán COD
     */
    public PaymentResult processCODPayment(Order order) {
        try {
            String transactionId = generateTransactionId();
            
            Payment payment = Payment.builder()
                    .order(order)
                    .transactionId(transactionId)
                    .amount(order.getTotalAmount())
                    .paymentMethod(PaymentMethod.COD.name())
                    .status(PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusDays(7)) // COD có 7 ngày để thanh toán
                    .build();
            
            Payment saved = paymentRepository.save(payment);
            
            // Cập nhật order
            order.setPaymentMethod(PaymentMethod.COD);
            order.setTransactionId(transactionId);
            orderRepository.save(order);
            
            log.info("Tạo COD payment {} cho order {}", transactionId, order.getOrderCode());
            return PaymentResult.success(saved, null);
        } catch (Exception e) {
            log.error("Lỗi tạo COD payment", e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    /**
     * Xử lý VNPay IPN (Instant Payment Notification)
     */
    public void processVNPayIPN(Map<String, String> params) {
        String transactionId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        
        if ("00".equals(responseCode)) {
            processSuccessCallback(transactionId, params);
        } else {
            processFailCallback(transactionId, "VNPay IPN error: " + responseCode);
        }
    }

    /**
     * Xử lý MoMo callback
     */
    public PaymentResult processMoMoCallback(Map<String, Object> payload) {
        String transactionId = (String) payload.get("orderId");
        Integer resultCode = (Integer) payload.get("resultCode");
        
        try {
            if (resultCode != null && resultCode == 0) {
                Map<String, String> params = Map.of(
                        "gatewayTransactionId", String.valueOf(payload.get("transId"))
                );
                Payment payment = processSuccessCallback(transactionId, params);
                return PaymentResult.success(payment, null);
            } else {
                String message = (String) payload.getOrDefault("message", "MoMo payment failed");
                processFailCallback(transactionId, message);
                return PaymentResult.failure(message);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý MoMo callback", e);
            return PaymentResult.failure(e.getMessage());
        }
    }

    /**
     * Xử lý ZaloPay callback
     */
    public void processZaloPayCallback(Map<String, Object> payload) {
        String transactionId = (String) payload.get("app_trans_id");
        Integer returnCode = (Integer) payload.get("return_code");
        
        if (returnCode != null && returnCode == 1) {
            Map<String, String> params = Map.of(
                    "gatewayTransactionId", String.valueOf(payload.get("zp_trans_id"))
            );
            processSuccessCallback(transactionId, params);
        } else {
            processFailCallback(transactionId, "ZaloPay callback failed");
        }
    }

    /**
     * Tạo VNPay payment URL
     */
    public String createVNPayPaymentUrl(Order order, String clientIP) {
        // TODO: Tích hợp VNPay API thực tế
        return createVnPayUrl(Payment.builder()
                .order(order)
                .transactionId(generateTransactionId())
                .amount(order.getTotalAmount())
                .build());
    }

    /**
     * Tạo MoMo payment URL
     */
    public String createMoMoPaymentUrl(Order order) {
        // TODO: Tích hợp MoMo API thực tế
        return createMomoUrl(Payment.builder()
                .order(order)
                .transactionId(generateTransactionId())
                .amount(order.getTotalAmount())
                .build());
    }

    /**
     * Tạo ZaloPay payment URL
     */
    public String createZaloPayPaymentUrl(Order order) {
        // TODO: Tích hợp ZaloPay API thực tế
        return createZaloPayUrl(Payment.builder()
                .order(order)
                .transactionId(generateTransactionId())
                .amount(order.getTotalAmount())
                .build());
    }

    /**
     * Yêu cầu hoàn tiền
     */
    public RefundResult requestRefund(Order order, String reason) {
        // Find payment for this order
        var paymentOpt = paymentRepository.findByOrderId(order.getId()).stream().findFirst();
        if (paymentOpt.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy payment cho order");
        }
        
        Payment payment = paymentOpt.get();
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Chỉ có thể hoàn tiền payment đã thanh toán");
        }
        
        // Process refund
        Payment refunded = refundPayment(payment.getId(), payment.getAmount(), reason);
        
        return new RefundResult(
                order.getId(),
                "REFUND_REQUESTED",
                "REF-" + refunded.getTransactionId(),
                "Yêu cầu hoàn tiền đã được tạo"
        );
    }

    /**
     * Inner record cho kết quả hoàn tiền
     */
    public record RefundResult(
            Long orderId,
            String status,
            String refundId,
            String message
    ) {}
}
