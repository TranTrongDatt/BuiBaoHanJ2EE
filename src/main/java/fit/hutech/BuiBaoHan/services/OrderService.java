package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.OrderStatus;
import fit.hutech.BuiBaoHan.constants.PaymentMethod;
import fit.hutech.BuiBaoHan.constants.PaymentStatus;
import fit.hutech.BuiBaoHan.constants.ShippingType;
import fit.hutech.BuiBaoHan.dto.DashboardStatsResponse;
import fit.hutech.BuiBaoHan.dto.OrderCreateRequest;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.CartItem;
import fit.hutech.BuiBaoHan.entities.Order;
import fit.hutech.BuiBaoHan.entities.OrderItem;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.ICartItemRepository;
import fit.hutech.BuiBaoHan.repositories.IOrderItemRepository;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Đơn hàng
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final IOrderRepository orderRepository;
    private final IOrderItemRepository orderItemRepository;
    private final ICartItemRepository cartItemRepository;
    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final NotificationService notificationService;

    // Phí vận chuyển
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("30000");
    private static final BigDecimal EXPRESS_SHIPPING_FEE = new BigDecimal("50000");
    private static final BigDecimal SAME_DAY_SHIPPING_FEE = new BigDecimal("80000");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500000");

    /**
     * Lấy tất cả đơn hàng (Admin)
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * Lấy đơn hàng theo trạng thái
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    /**
     * Tìm đơn hàng theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findByIdWithItems(id);
    }

    /**
     * Tìm đơn hàng theo mã (eager fetch items + user)
     */
    @Transactional(readOnly = true)
    public Optional<Order> getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCodeWithItems(orderCode);
    }

    /**
     * Lấy đơn hàng của user
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Lấy đơn hàng của user theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Tạo đơn hàng từ giỏ hàng
     */
    public Order createOrderFromCart(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        List<CartItem> cartItems = cartItemRepository.findByCartUserIdOrderByCreatedAtDesc(userId);
        
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        // Validate tồn kho
        for (CartItem item : cartItems) {
            if (item.getBook().getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Sách '" + item.getBook().getTitle() + "' không đủ số lượng");
            }
        }

        // Tính tổng tiền
        BigDecimal subtotal = cartItems.stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính phí ship
        ShippingType shippingType = request.shippingType() != null ? 
                request.shippingType() : ShippingType.STANDARD;
        BigDecimal shippingFee = calculateShippingFee(subtotal, shippingType);

        BigDecimal totalAmount = subtotal.add(shippingFee);

        // Tạo order
        Order order = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .receiverName(request.receiverName() != null ? request.receiverName() : user.getFullName())
                .receiverPhone(request.receiverPhone() != null ? request.receiverPhone() : user.getPhone())
                .shippingAddress(request.shippingAddress())
                .shippingType(shippingType)
                .shippingFee(shippingFee)
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.COD)
                .subtotal(subtotal)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .notes(request.notes())
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // Tạo order items
        for (CartItem cartItem : cartItems) {
            Book book = cartItem.getBook();
            
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .originalPrice(book.getOriginalPrice())
                    .bookTitle(book.getTitle())
                    .bookIsbn(book.getIsbn())
                    .bookImage(book.getCoverImage())
                    .build();

            order.getItems().add(item);

            // Trừ tồn kho
            book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
            book.setSoldCount(book.getSoldCount() + cartItem.getQuantity());
            bookRepository.save(book);
        }

        Order saved = orderRepository.save(order);

        // Xóa giỏ hàng
        cartItemRepository.deleteByCartUserId(userId);

        // Gửi thông báo
        notificationService.sendOrderNotification(userId, saved);

        log.info("Created order {} for user {}", saved.getOrderCode(), userId);
        return saved;
    }

    /**
     * Cập nhật trạng thái đơn hàng
     */
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        // Validate chuyển trạng thái
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Cập nhật thời gian
        switch (newStatus) {
            case CONFIRMED -> order.setConfirmedDate(LocalDateTime.now());
            case SHIPPING -> order.setShippedDate(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredDate(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledDate(LocalDateTime.now());
                // Hoàn kho
                restoreStock(order);
            }
            default -> {}
        }

        Order updated = orderRepository.save(order);

        // Gửi thông báo
        notificationService.sendOrderStatusNotification(order.getUser().getId(), order, oldStatus);

        log.info("Updated order {} status from {} to {}", orderId, oldStatus, newStatus);
        return updated;
    }

    /**
     * Hủy đơn hàng
     */
    public Order cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        if (!canCancel(order)) {
            throw new IllegalStateException("Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledDate(LocalDateTime.now());
        order.setCancelReason(reason);

        // Hoàn kho
        restoreStock(order);

        Order cancelled = orderRepository.save(order);

        // Gửi thông báo
        notificationService.sendOrderCancelledNotification(order.getUser().getId(), order);

        log.info("Cancelled order {} - Reason: {}", orderId, reason);
        return cancelled;
    }

    /**
     * Kiểm tra có thể hủy đơn không
     */
    @Transactional(readOnly = true)
    public boolean canCancel(Order order) {
        return order.getStatus() == OrderStatus.PENDING 
                || order.getStatus() == OrderStatus.CONFIRMED;
    }

    /**
     * Cập nhật thanh toán
     */
    public Order updatePaymentStatus(Long orderId, PaymentStatus paymentStatus, String transactionId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));

        order.setPaymentStatus(paymentStatus);
        if (transactionId != null) {
            order.setTransactionId(transactionId);
        }
        if (paymentStatus == PaymentStatus.PAID) {
            order.setPaidDate(LocalDateTime.now());
        }

        Order updated = orderRepository.save(order);
        log.info("Updated order {} payment status to {}", orderId, paymentStatus);
        return updated;
    }

    /**
     * Thống kê đơn hàng
     */
    @Transactional(readOnly = true)
    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    /**
     * Tổng doanh thu
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal result = orderRepository.getRevenueByStatusBetween(startDate, endDate, OrderStatus.DELIVERED);
        return result != null ? result : BigDecimal.ZERO;
    }

    // ==================== Private Helper Methods ====================

    private String generateOrderCode() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + random;
    }

    private BigDecimal calculateShippingFee(BigDecimal subtotal, ShippingType shippingType) {
        // Free shipping nếu đơn >= 500k
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 && shippingType == ShippingType.STANDARD) {
            return BigDecimal.ZERO;
        }

        return switch (shippingType) {
            case STANDARD -> STANDARD_SHIPPING_FEE;
            case EXPRESS -> EXPRESS_SHIPPING_FEE;
            case SAME_DAY -> SAME_DAY_SHIPPING_FEE;
            case ECONOMY -> new BigDecimal("15000");
            case PICKUP -> BigDecimal.ZERO;
        };
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.PROCESSING || next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case PROCESSING -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case DELIVERED -> next == OrderStatus.COMPLETED || next == OrderStatus.RETURNED;
            case COMPLETED, CANCELLED, RETURNED -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Không thể chuyển từ " + current + " sang " + next);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Book book = item.getBook();
            book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
            book.setSoldCount(book.getSoldCount() - item.getQuantity());
            bookRepository.save(book);
        }
        log.info("Restored stock for order {}", order.getId());
    }

    // ==================== ApiPaymentController Support Methods ====================

    /**
     * Tìm đơn hàng theo ID và user
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByIdAndUser(Long orderId, User user) {
        return orderRepository.findByIdWithItems(orderId)
                .filter(o -> o.getUser().getId().equals(user.getId()));
    }

    // ==================== API Controller Support Methods ====================

    /**
     * Tạo đơn hàng từ giỏ hàng (wrapper với User object)
     */
    public Order createFromCart(User user, OrderCreateRequest request) {
        return createOrderFromCart(user.getId(), request);
    }

    /**
     * Hủy đơn hàng với kiểm tra quyền sở hữu
     */
    public Order cancelOrder(Long orderId, User user, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn hàng này");
        }
        
        return cancelOrder(orderId, reason);
    }

    /**
     * Tìm đơn hàng theo trạng thái (String version)
     */
    @Transactional(readOnly = true)
    public Page<Order> findByStatus(String status, Pageable pageable) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        return getOrdersByStatus(orderStatus, pageable);
    }

    /**
     * Lấy tất cả đơn hàng (alias)
     */
    @Transactional(readOnly = true)
    public Page<Order> findAll(Pageable pageable) {
        return getAllOrders(pageable);
    }

    /**
     * Lấy đơn hàng của user (wrapper với User object)
     */
    @Transactional(readOnly = true)
    public Page<Order> findByUser(User user, Pageable pageable) {
        return getOrdersByUserId(user.getId(), pageable);
    }

    // ==================== Dashboard Methods ====================

    /**
     * Get today's revenue
     */
    @Transactional(readOnly = true)
    public BigDecimal getTodayRevenue() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal result = orderRepository.getRevenueBetween(startOfDay, endOfDay);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get this week's revenue
     */
    @Transactional(readOnly = true)
    public BigDecimal getWeekRevenue() {
        LocalDateTime startOfWeek = LocalDate.now().minusDays(7).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal result = orderRepository.getRevenueBetween(startOfWeek, now);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get this month's revenue
     */
    @Transactional(readOnly = true)
    public BigDecimal getMonthRevenue() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal result = orderRepository.getRevenueBetween(startOfMonth, now);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get this year's revenue
     */
    @Transactional(readOnly = true)
    public BigDecimal getYearRevenue() {
        LocalDateTime startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal result = orderRepository.getRevenueBetween(startOfYear, now);
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get total revenue (all time)
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        BigDecimal result = orderRepository.getTotalRevenue();
        return result != null ? result : BigDecimal.ZERO;
    }

    /**
     * Get revenue growth percentage (this month vs last month)
     */
    @Transactional(readOnly = true)
    public Double getRevenueGrowthPercent() {
        LocalDate now = LocalDate.now();
        LocalDate lastMonthStart = now.minusMonths(1).withDayOfMonth(1);
        LocalDate lastMonthEnd = now.withDayOfMonth(1).minusDays(1);
        
        BigDecimal thisMonth = getMonthRevenue();
        BigDecimal lastMonth = orderRepository.getRevenueBetween(
                lastMonthStart.atStartOfDay(),
                lastMonthEnd.atTime(LocalTime.MAX)
        );
        
        if (lastMonth == null || lastMonth.compareTo(BigDecimal.ZERO) == 0) {
            return thisMonth.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        
        return thisMonth.subtract(lastMonth)
                .divide(lastMonth, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    /**
     * Get payment method statistics for dashboard pie chart
     * Returns Map with payment method names as keys and counts as values
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getPaymentMethodStats() {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        // Initialize with default values
        stats.put("COD", 0L);
        stats.put("VIETQR", 0L);
        stats.put("BANK_TRANSFER", 0L);
        stats.put("OTHER", 0L);
        
        List<Object[]> results = orderRepository.countByPaymentMethod();
        for (Object[] row : results) {
            if (row[0] != null) {
                String method = row[0].toString();
                Long count = ((Number) row[1]).longValue();
                
                // Map to simplified categories
                if ("COD".equals(method)) {
                    stats.put("COD", stats.get("COD") + count);
                } else if ("VIETQR".equals(method) || "BANK_TRANSFER".equals(method)) {
                    stats.put("VIETQR", stats.get("VIETQR") + count);
                } else if ("MOMO".equals(method) || "ZALOPAY".equals(method) || "VNPAY".equals(method)) {
                    stats.put("OTHER", stats.get("OTHER") + count);
                } else {
                    stats.put("OTHER", stats.get("OTHER") + count);
                }
            }
        }
        return stats;
    }

    /**
     * Get today's order count
     */
    @Transactional(readOnly = true)
    public long getTodayOrderCount() {
        return orderRepository.getTodayOrderCount();
    }

    /**
     * Get best selling categories in current month for dashboard chart
     * Returns map with categoryNames and categorySoldCounts lists
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getBestSellingCategoriesInMonth(int limit) {
        List<Object[]> results = orderItemRepository.findBestSellingCategoriesInMonth(limit);
        
        List<String> categoryNames = new ArrayList<>();
        List<Long> categorySoldCounts = new ArrayList<>();
        
        for (Object[] row : results) {
            categoryNames.add((String) row[0]);
            categorySoldCounts.add(((Number) row[1]).longValue());
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("categoryNames", categoryNames);
        data.put("categorySoldCounts", categorySoldCounts);
        return data;
    }

    /**
     * Get top customers by completed orders for dashboard table
     * Returns list of maps with user info and rank
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopCustomers(int limit) {
        List<Object[]> results = orderRepository.findTopCustomersByCompletedOrders(limit);
        List<Map<String, Object>> customers = new ArrayList<>();
        
        for (int i = 0; i < results.size(); i++) {
            Object[] row = results.get(i);
            Map<String, Object> customer = new HashMap<>();
            customer.put("userId", row[0]);
            customer.put("username", row[1]);
            customer.put("fullName", row[2]);
            customer.put("avatar", row[3]);
            long orderCount = ((Number) row[4]).longValue();
            customer.put("orderCount", orderCount);
            
            // Determine rank based on completed orders
            int rank;
            if (orderCount >= 5) {
                rank = 1; // Gold
            } else if (orderCount >= 3) {
                rank = 2; // Silver
            } else {
                rank = 3; // Bronze
            }
            customer.put("rank", rank);
            customers.add(customer);
        }
        return customers;
    }

    /**
     * Find recent orders
     */
    @Transactional(readOnly = true)
    public List<Order> findRecent(int limit) {
        return orderRepository.findAllWithUser(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    /**
     * Get revenue by days for chart
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getRevenueByDays(int days) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            BigDecimal revenue = orderRepository.getRevenueBetween(
                    date.atStartOfDay(),
                    date.atTime(LocalTime.MAX)
            );
            result.add(new DashboardStatsResponse.ChartData(
                    date.toString(),
                    revenue != null ? revenue.doubleValue() : 0.0
            ));
        }
        
        return result;
    }

    /**
     * Get order count by days for chart
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getOrderCountByDays(int days) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = orderRepository.findByCreatedAtBetween(
                    date.atStartOfDay(),
                    date.atTime(LocalTime.MAX)
            ).size();
            result.add(new DashboardStatsResponse.ChartData(
                    date.toString(),
                    (double) count
            ));
        }
        
        return result;
    }

    /**
     * Get top selling books
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.TopItem> getTopSellingBooks(int limit) {
        // Aggregate order items by book
        Map<Book, Long> bookSales = new HashMap<>();
        
        orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    Book book = item.getBook();
                    bookSales.merge(book, (long) item.getQuantity(), (a, b) -> a + b);
                });
        
        return bookSales.entrySet().stream()
                .sorted(Map.Entry.<Book, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new DashboardStatsResponse.TopItem(
                        entry.getKey().getId(),
                        entry.getKey().getTitle(),
                        entry.getKey().getCoverImage(),
                        entry.getValue(),
                        entry.getKey().getPrice()
                ))
                .toList();
    }

    /**
     * Get sales report for period
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesReport(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        long totalOrders = orders.size();
        long completedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        
        return Map.of(
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "completedOrders", completedOrders,
                "cancelledOrders", cancelledOrders,
                "startDate", startDate,
                "endDate", endDate
        );
    }

    // ==================== AdminReportController Methods ====================

    /**
     * Get revenue by weeks
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getRevenueByWeeks(int weeks) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            BigDecimal revenue = orderRepository.getRevenueBetween(
                    weekStart.atStartOfDay(),
                    weekEnd.atTime(LocalTime.MAX)
            );
            
            result.add(new DashboardStatsResponse.ChartData(
                    "Week " + (weeks - i),
                    revenue != null ? revenue.doubleValue() : 0.0
            ));
        }
        
        return result;
    }

    /**
     * Get revenue by months
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getRevenueByMonths(int months) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            BigDecimal revenue = orderRepository.getRevenueBetween(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(LocalTime.MAX)
            );
            
            result.add(new DashboardStatsResponse.ChartData(
                    monthStart.getMonth().name(),
                    revenue != null ? revenue.doubleValue() : 0.0
            ));
        }
        
        return result;
    }

    /**
     * Get order count by weeks
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getOrderCountByWeeks(int weeks) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = weeks - 1; i >= 0; i--) {
            LocalDate weekStart = LocalDate.now().minusWeeks(i).with(java.time.DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            
            long count = orderRepository.findByCreatedAtBetween(
                    weekStart.atStartOfDay(),
                    weekEnd.atTime(LocalTime.MAX)
            ).size();
            
            result.add(new DashboardStatsResponse.ChartData("Week " + (weeks - i), (double) count));
        }
        
        return result;
    }

    /**
     * Get order count by months
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.ChartData> getOrderCountByMonths(int months) {
        List<DashboardStatsResponse.ChartData> result = new ArrayList<>();
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            long count = orderRepository.findByCreatedAtBetween(
                    monthStart.atStartOfDay(),
                    monthEnd.atTime(LocalTime.MAX)
            ).size();
            
            result.add(new DashboardStatsResponse.ChartData(monthStart.getMonth().name(), (double) count));
        }
        
        return result;
    }

    /**
     * Get top selling books with date range (overload)
     */
    @Transactional(readOnly = true)
    public List<DashboardStatsResponse.TopItem> getTopSellingBooks(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        Map<Book, Long> bookSales = new HashMap<>();
        
        orderRepository.findByCreatedAtBetween(start, end).stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    Book book = item.getBook();
                    bookSales.merge(book, (long) item.getQuantity(), (a, b) -> a + b);
                });
        
        return bookSales.entrySet().stream()
                .sorted(Map.Entry.<Book, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> new DashboardStatsResponse.TopItem(
                        entry.getKey().getId(),
                        entry.getKey().getTitle(),
                        entry.getKey().getCoverImage(),
                        entry.getValue(),
                        entry.getKey().getPrice()
                ))
                .toList();
    }

    /**
     * Export sales report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportReport(String format, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>(getSalesReport(startDate, endDate));
        report.put("format", format);
        report.put("exportedAt", java.time.LocalDateTime.now());
        return report;
    }

    // ==================== Profile Controller Support Methods ====================

    /**
     * Lấy đơn hàng gần đây của user (by userId - hỗ trợ cả Form + OAuth2 login)
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserRecentById(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).getContent();
    }

    /**
     * Lấy đơn hàng gần đây của user
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserRecent(User user, int limit) {
        return findByUserRecentById(user.getId(), limit);
    }

    /**
     * Lấy tất cả đơn hàng của user (by userId)
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserAllById(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged()).getContent();
    }

    /**
     * Lấy tất cả đơn hàng của user
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserAll(User user) {
        return findByUserAllById(user.getId());
    }

    /**
     * Lấy đơn hàng của user theo trạng thái (by userId)
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserAndStatusById(Long userId, String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByUserIdAndStatus(userId, orderStatus);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    /**
     * Lấy đơn hàng của user theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<Order> findByUserAndStatus(User user, String status) {
        return findByUserAndStatusById(user.getId(), status);
    }

    /**
     * Tìm đơn hàng theo ID và userId (hỗ trợ cả Form + OAuth2 login)
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdWithItems(orderId)
                .filter(o -> o.getUser().getId().equals(userId));
    }

    // ==================== Admin Controller Support Methods ====================

    /**
     * Tìm đơn hàng theo ID
     */
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return getOrderById(id);
    }

    /**
     * Tìm tất cả đơn hàng với bộ lọc
     */
    @Transactional(readOnly = true)
    public Page<Order> findAll(String search, String status, String paymentStatus, String dateRange, Pageable pageable) {
        // TODO: Implement full search with filters
        if (status != null && !status.isEmpty()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                return orderRepository.findByStatusWithUser(orderStatus, pageable);
            } catch (IllegalArgumentException e) {
                // Invalid status, return all
            }
        }
        return orderRepository.findAllWithUser(pageable);
    }

    /**
     * Đếm đơn hàng theo trạng thái (String version)
     */
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return countByStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * Lấy lịch sử thay đổi trạng thái
     */
    @Transactional(readOnly = true)
    public List<Object> getStatusHistory(Long orderId) {
        // TODO: Implement order status history tracking
        return List.of();
    }

    /**
     * Xác nhận đơn hàng
     */
    public Order confirmOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.CONFIRMED);
    }

    /**
     * Đánh dấu đơn hàng đang vận chuyển
     */
    public Order shipOrder(Long orderId, String trackingNumber) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setTrackingNumber(trackingNumber);
        orderRepository.save(order);
        return updateOrderStatus(orderId, OrderStatus.SHIPPING);
    }

    /**
     * Đánh dấu đơn hàng đã giao
     */
    public Order deliverOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.DELIVERED);
    }

    /**
     * Hủy đơn hàng (Admin)
     */
    public Order adminCancelOrder(Long orderId, String reason) {
        return cancelOrder(orderId, reason);
    }

    /**
     * Cập nhật trạng thái đơn hàng (String version)
     */
    public Order updateStatus(Long orderId, String status, String note) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        Order order = updateOrderStatus(orderId, orderStatus);
        if (note != null && !note.isEmpty()) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + note : note);
            orderRepository.save(order);
        }
        return order;
    }

    /**
     * Cập nhật trạng thái thanh toán (String version)
     */
    public Order updatePaymentStatus(Long orderId, String paymentStatus, String transactionId) {
        fit.hutech.BuiBaoHan.constants.PaymentStatus status = 
                fit.hutech.BuiBaoHan.constants.PaymentStatus.valueOf(paymentStatus.toUpperCase());
        return updatePaymentStatus(orderId, status, transactionId);
    }

    /**
     * Thêm ghi chú cho đơn hàng
     */
    public void addNote(Long orderId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + note : note);
        orderRepository.save(order);
        log.info("Added note to order {}", orderId);
    }

    /**
     * Xử lý hoàn tiền
     */
    public Order processRefund(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + orderId));
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundReason(reason);
        order.setRefundedAt(LocalDateTime.now());
        log.info("Processed refund for order {} - Reason: {}", orderId, reason);
        return orderRepository.save(order);
    }

    /**
     * Cập nhật trạng thái hàng loạt
     */
    public int bulkUpdateStatus(List<Long> ids, String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        int count = 0;
        for (Long id : ids) {
            try {
                updateOrderStatus(id, orderStatus);
                count++;
            } catch (Exception e) {
                log.warn("Failed to update order {} status: {}", id, e.getMessage());
            }
        }
        return count;
    }

    /**
     * Xuất đơn hàng ra CSV
     */
    @Transactional(readOnly = true)
    public String exportToCsv(String status, String dateRange) {
        StringBuilder csv = new StringBuilder();
        csv.append("Order ID,Order Code,Customer,Total,Status,Payment Status,Created At\n");
        
        Page<Order> orders = findAll(null, status, null, dateRange, Pageable.unpaged());
        for (Order order : orders.getContent()) {
            csv.append(order.getId()).append(",")
                    .append(order.getOrderCode()).append(",")
                    .append(order.getUser().getFullName()).append(",")
                    .append(order.getTotalAmount()).append(",")
                    .append(order.getStatus()).append(",")
                    .append(order.getPaymentStatus()).append(",")
                    .append(order.getCreatedAt()).append("\n");
        }
        
        return csv.toString();
    }

    // ==================== Session Cart Support ====================

    /**
     * Tạo đơn hàng từ session cart (DAO)
     */
    public Order createOrderFromSessionCart(
            User user,
            fit.hutech.BuiBaoHan.daos.Cart sessionCart,
            String receiverName,
            String receiverPhone,
            String receiverEmail,
            String shippingAddress,
            String province,
            BigDecimal shippingFee,
            PaymentMethod paymentMethod,
            String notes) {

        if (sessionCart.getCartItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        // Tính subtotal
        BigDecimal subtotal = sessionCart.getCartItems().stream()
                .map(item -> BigDecimal.valueOf(item.getPrice()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = subtotal.add(shippingFee);

        // Tạo order
        Order order = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .shippingAddress(shippingAddress)
                .shippingType(ShippingType.STANDARD)
                .shippingFee(shippingFee)
                .paymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD)
                .subtotal(subtotal)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // Tạo order items
        for (fit.hutech.BuiBaoHan.daos.Item cartItem : sessionCart.getCartItems()) {
            Book book = bookRepository.findById(cartItem.getBookId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ID: " + cartItem.getBookId()));

            // Validate tồn kho
            if (book.getStockQuantity() != null && book.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Sách '" + book.getTitle() + "' không đủ số lượng tồn kho");
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .originalPrice(book.getOriginalPrice())
                    .bookTitle(book.getTitle())
                    .bookIsbn(book.getIsbn())
                    .bookImage(book.getCoverImage())
                    .build();

            order.getItems().add(item);

            // Trừ tồn kho
            if (book.getStockQuantity() != null) {
                book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
            }
            long sold = book.getSoldCount() != null ? book.getSoldCount() : 0L;
            book.setSoldCount(sold + cartItem.getQuantity());
            bookRepository.save(book);
        }

        Order saved = orderRepository.save(order);

        // Gửi thông báo
        try {
            notificationService.sendOrderNotification(user.getId(), saved);
        } catch (Exception e) {
            log.warn("Failed to send order notification: {}", e.getMessage());
        }

        log.info("Created order {} from session cart for user {}", saved.getOrderCode(), user.getId());
        return saved;
    }

    // ==================== DB Cart Support ====================

    /**
     * Tạo đơn hàng từ database cart (CartItem entities)
     */
    public Order createOrderFromDbCart(
            User user,
            List<CartItem> dbCartItems,
            String receiverName,
            String receiverPhone,
            String receiverEmail,
            String shippingAddress,
            String province,
            BigDecimal shippingFee,
            PaymentMethod paymentMethod,
            String notes) {

        if (dbCartItems == null || dbCartItems.isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        // Tính subtotal
        BigDecimal subtotal = dbCartItems.stream()
                .map(item -> item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmount = subtotal.add(shippingFee);

        // Generate transactionId for BANK_TRANSFER orders
        String transactionId = null;
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            transactionId = "VQR" + System.currentTimeMillis();
        }

        // Tạo order
        Order order = Order.builder()
                .user(user)
                .orderCode(generateOrderCode())
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .shippingAddress(shippingAddress)
                .shippingType(ShippingType.STANDARD)
                .shippingFee(shippingFee)
                .paymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.COD)
                .subtotal(subtotal)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(transactionId)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // Tạo order items
        for (CartItem cartItem : dbCartItems) {
            Book book = cartItem.getBook();

            // Validate tồn kho
            if (book.getStockQuantity() != null && book.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Sách '" + book.getTitle() + "' không đủ số lượng tồn kho");
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .book(book)
                    .quantity(cartItem.getQuantity())
                    .price(book.getPrice())
                    .originalPrice(book.getOriginalPrice())
                    .bookTitle(book.getTitle())
                    .bookIsbn(book.getIsbn())
                    .bookImage(book.getCoverImage())
                    .build();

            order.getItems().add(item);

            // Trừ tồn kho
            if (book.getStockQuantity() != null) {
                book.setStockQuantity(book.getStockQuantity() - cartItem.getQuantity());
            }
            long sold = book.getSoldCount() != null ? book.getSoldCount() : 0L;
            book.setSoldCount(sold + cartItem.getQuantity());
            bookRepository.save(book);
        }

        Order saved = orderRepository.save(order);

        // Gửi thông báo cho user
        try {
            notificationService.sendOrderNotification(user.getId(), saved);
        } catch (Exception e) {
            log.warn("Failed to send order notification: {}", e.getMessage());
        }

        // Gửi thông báo cho admin nếu thanh toán chuyển khoản
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            try {
                notificationService.notifyAdminsNewBankTransferOrder(saved);
            } catch (Exception e) {
                log.warn("Failed to notify admins about bank transfer order: {}", e.getMessage());
            }
        }

        log.info("Created order {} from DB cart for user {}", saved.getOrderCode(), user.getId());
        return saved;
    }
}
