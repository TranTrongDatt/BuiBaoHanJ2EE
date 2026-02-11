package fit.hutech.BuiBaoHan.controllers.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.OrderSummaryDto;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.services.NotificationService;
import fit.hutech.BuiBaoHan.services.OrderService;
import fit.hutech.BuiBaoHan.services.PdfService;
import fit.hutech.BuiBaoHan.services.ShippingService;
import lombok.RequiredArgsConstructor;

/**
 * Admin Order Management Controller
 */
@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final ShippingService shippingService;
    private final PdfService pdfService;
    private final NotificationService notificationService;

    /**
     * List all orders
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String listOrders(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String dateRange) {
        
        Page<Order> orders = orderService.findAll(search, status, paymentStatus, dateRange, pageable);
        
        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("dateRange", dateRange);
        
        // Stats
        model.addAttribute("pendingCount", orderService.countByStatus("PENDING"));
        model.addAttribute("confirmedCount", orderService.countByStatus("CONFIRMED"));
        model.addAttribute("shippingCount", orderService.countByStatus("SHIPPING"));
        model.addAttribute("deliveredCount", orderService.countByStatus("DELIVERED"));
        model.addAttribute("cancelledCount", orderService.countByStatus("CANCELLED"));
        
        return "admin/orders";
    }

    /**
     * View order details
     */
    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return orderService.findById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("statusHistory", orderService.getStatusHistory(id));
                    model.addAttribute("shippingProviders", shippingService.getAvailableProviders());
                    return "admin/orders/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Order not found");
                    return "redirect:/admin/orders";
                });
    }

    /**
     * Confirm order
     */
    @PostMapping("/{id}/confirm")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> confirmOrder(@PathVariable Long id) {
        try {
            Order order = orderService.confirmOrder(id);
            return ApiResponse.success("Order confirmed", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Ship order
     */
    @PostMapping("/{id}/ship")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> shipOrder(
            @PathVariable Long id,
            @RequestParam String trackingNumber,
            @RequestParam(required = false) String shippingProvider) {
        try {
            Order order = orderService.shipOrder(id, trackingNumber);
            if (shippingProvider != null) {
                shippingService.updateProvider(id, shippingProvider);
            }
            return ApiResponse.success("Order shipped", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Mark order as delivered
     */
    @PostMapping("/{id}/deliver")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> deliverOrder(@PathVariable Long id) {
        try {
            Order order = orderService.deliverOrder(id);
            return ApiResponse.success("Order marked as delivered", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Cancel order
     */
    @PostMapping("/{id}/cancel")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            Order order = orderService.adminCancelOrder(id, reason);
            return ApiResponse.success("Order cancelled", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Update order status
     */
    @PostMapping("/{id}/status")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String note) {
        try {
            Order order = orderService.updateStatus(id, status, note);
            return ApiResponse.success("Status updated", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Update payment status
     */
    @PostMapping("/{id}/payment-status")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String paymentStatus,
            @RequestParam(required = false) String transactionId) {
        try {
            Order order = orderService.updatePaymentStatus(id, paymentStatus, transactionId);
            // Notify user when payment is confirmed as PAID
            if ("PAID".equalsIgnoreCase(paymentStatus)) {
                try {
                    notificationService.sendPaymentConfirmedNotification(order.getUser().getId(), order);
                } catch (Exception e) {
                    // Don't fail the payment status update if notification fails
                }
            }
            return ApiResponse.success("Payment status updated", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Add order note
     */
    @PostMapping("/{id}/note")
    @ResponseBody
    public ApiResponse<Void> addNote(
            @PathVariable Long id,
            @RequestParam String note) {
        try {
            orderService.addNote(id, note);
            return ApiResponse.success("Note added");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Process refund
     */
    @PostMapping("/{id}/refund")
    @ResponseBody
    public ApiResponse<OrderSummaryDto> processRefund(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            Order order = orderService.processRefund(id, reason);
            return ApiResponse.success("Refund processed", OrderSummaryDto.from(order));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Bulk update status
     */
    @PostMapping("/bulk-status")
    @ResponseBody
    public ApiResponse<Integer> bulkUpdateStatus(
            @RequestParam List<Long> ids,
            @RequestParam String status) {
        try {
            int count = orderService.bulkUpdateStatus(ids, status);
            return ApiResponse.success("Updated " + count + " orders", count);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Print order
     */
    @GetMapping("/{id}/print")
    public String printOrder(@PathVariable Long id, Model model) {
        return orderService.findById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    return "admin/orders/print";
                })
                .orElse("redirect:/admin/orders");
    }

    /**
     * View invoice as HTML page (để xem hóa đơn trực tiếp)
     */
    @GetMapping("/{id}/invoice")
    public String viewInvoice(@PathVariable Long id, Model model) {
        return orderService.findById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("storeInfo", new PdfService.StoreInfo());
                    return "pdf/invoice";
                })
                .orElse("redirect:/admin/orders");
    }

    /**
     * Download PDF invoice
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(order -> {
                    byte[] pdfBytes = pdfService.generateInvoicePdf(order);
                    
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", 
                            "HoaDon_" + order.getOrderCode() + ".pdf");
                    headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                    
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(pdfBytes);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Export orders to CSV
     */
    @GetMapping("/export")
    @ResponseBody
    public String exportOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateRange) {
        return orderService.exportToCsv(status, dateRange);
    }
}
