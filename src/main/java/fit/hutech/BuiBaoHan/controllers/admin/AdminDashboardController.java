package fit.hutech.BuiBaoHan.controllers.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.DashboardStatsResponse;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IBorrowRecordRepository;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import fit.hutech.BuiBaoHan.repositories.IFieldRepository;
import fit.hutech.BuiBaoHan.repositories.ILibraryCardRepository;
import fit.hutech.BuiBaoHan.repositories.IOrderRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.services.ExcelExportService;
import fit.hutech.BuiBaoHan.services.LibraryReportService;
import fit.hutech.BuiBaoHan.services.OrderService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Admin Dashboard Controller
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardController {

    private final IUserRepository userRepository;
    private final IBookRepository bookRepository;
    private final IOrderRepository orderRepository;
    private final ILibraryCardRepository libraryCardRepository;
    private final IBorrowRecordRepository borrowRecordRepository;
    private final ICategoryRepository categoryRepository;
    private final IFieldRepository fieldRepository;
    private final OrderService orderService;
    private final LibraryReportService libraryReportService;
    private final ExcelExportService excelExportService;

    /**
     * Dashboard home page
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        // Overview stats
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("activeUsers", userRepository.countActiveUsers());
        model.addAttribute("totalBooks", bookRepository.count());
        model.addAttribute("lowStockBooks", bookRepository.countLowStock(5));
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("pendingOrders", orderRepository.countPendingOrders());
        model.addAttribute("totalLibraryCards", libraryCardRepository.count());
        model.addAttribute("currentlyBorrowed", borrowRecordRepository.countCurrentlyBorrowed());
        
        // Category & Field stats
        model.addAttribute("totalCategories", categoryRepository.count());
        model.addAttribute("totalFields", fieldRepository.count());
        
        // Overdue records count
        model.addAttribute("overdueRecords", borrowRecordRepository.countOverdue());
        
        // Revenue stats
        model.addAttribute("todayRevenue", orderService.getTodayRevenue());
        model.addAttribute("monthRevenue", orderService.getMonthRevenue());
        model.addAttribute("todayOrders", orderService.getTodayOrderCount());
        
        // Payment method statistics for pie chart
        var paymentStats = orderService.getPaymentMethodStats();
        model.addAttribute("codPayments", paymentStats.get("COD"));
        model.addAttribute("vietqrPayments", paymentStats.get("VIETQR"));
        model.addAttribute("otherPayments", paymentStats.get("OTHER"));
        
        // Recent activity
        model.addAttribute("recentOrders", orderService.findRecent(5));
        model.addAttribute("recentUsers", userRepository.findTop5ByOrderByCreatedAtDesc());
        
        // Chart data for Category and Field (optimized single queries)
        var categoryData = categoryRepository.findTop5CategoriesWithBookCount();
        var fieldData = fieldRepository.findTop5FieldsWithBookCount();
        
        List<String> categoryNames = new ArrayList<>();
        List<Long> categoryBookCounts = new ArrayList<>();
        for (Object[] row : categoryData) {
            categoryNames.add((String) row[0]);
            categoryBookCounts.add(((Number) row[1]).longValue());
        }
        
        List<String> fieldNames = new ArrayList<>();
        List<Long> fieldBookCounts = new ArrayList<>();
        for (Object[] row : fieldData) {
            fieldNames.add((String) row[0]);
            fieldBookCounts.add(((Number) row[1]).longValue());
        }
        
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("categoryBookCounts", categoryBookCounts);
        model.addAttribute("fieldNames", fieldNames);
        model.addAttribute("fieldBookCounts", fieldBookCounts);
        
        // Best selling categories in month for horizontal bar chart
        var bestSellingCategoriesData = orderService.getBestSellingCategoriesInMonth(5);
        model.addAttribute("bestSellingCategoryNames", bestSellingCategoriesData.get("categoryNames"));
        model.addAttribute("bestSellingCategoryCounts", bestSellingCategoriesData.get("categorySoldCounts"));
        
        // Top stock products for vertical bar chart
        var stockData = bookRepository.findTopByStockQuantity(PageRequest.of(0, 5));
        List<String> stockBookNames = new ArrayList<>();
        List<Integer> stockBookCounts = new ArrayList<>();
        for (Object[] row : stockData) {
            String title = (String) row[0];
            // Truncate long titles
            stockBookNames.add(title.length() > 15 ? title.substring(0, 15) + "..." : title);
            stockBookCounts.add(((Number) row[1]).intValue());
        }
        model.addAttribute("stockBookNames", stockBookNames);
        model.addAttribute("stockBookCounts", stockBookCounts);
        
        // Top customers for ranking table
        model.addAttribute("topCustomers", orderService.getTopCustomers(3));
        
        return "admin/dashboard";
    }
    
    /**
     * Export dashboard statistics to Excel file
     */
    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=miniverse-report-" + 
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx");
        
        byte[] excelData = excelExportService.exportDashboardToExcel();
        response.getOutputStream().write(excelData);
        response.getOutputStream().flush();
    }

    /**
     * Get dashboard stats as JSON (for AJAX refresh)
     */
    @GetMapping("/dashboard/stats")
    @ResponseBody
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        // Build overview stats
        var overview = new DashboardStatsResponse.OverviewStats(
                userRepository.count(),
                userRepository.countActiveUsers(),
                userRepository.countNewUsersToday(),
                bookRepository.count(),
                bookRepository.countLowStock(5),
                orderRepository.count(),
                orderRepository.countPendingOrders(),
                0L // blog posts
        );

        // Build revenue stats
        var revenue = new DashboardStatsResponse.RevenueStats(
                orderService.getTodayRevenue(),
                orderService.getWeekRevenue(),
                orderService.getMonthRevenue(),
                orderService.getYearRevenue(),
                orderService.getTotalRevenue(),
                orderService.getRevenueGrowthPercent()
        );

        // Build order stats
        var orders = new DashboardStatsResponse.OrderStats(
                orderRepository.countByStatus("PENDING"),
                orderRepository.countByStatus("CONFIRMED"),
                orderRepository.countByStatus("SHIPPING"),
                orderRepository.countByStatus("DELIVERED"),
                orderRepository.countByStatus("CANCELLED"),
                orderRepository.countTodayOrders(),
                orderRepository.countWeekOrders(),
                orderRepository.countMonthOrders()
        );

        // Build library stats
        var libraryStats = libraryReportService.getOverviewStats();
        var library = new DashboardStatsResponse.LibraryStats(
                ((Number) libraryStats.getOrDefault("totalCards", 0L)).longValue(),
                ((Number) libraryStats.getOrDefault("activeCards", 0L)).longValue(),
                ((Number) libraryStats.getOrDefault("expiringCards", 0L)).longValue(),
                ((Number) libraryStats.getOrDefault("currentlyBorrowed", 0L)).longValue(),
                ((Number) libraryStats.getOrDefault("overdueRecords", 0L)).longValue(),
                ((Number) libraryStats.getOrDefault("pendingFines", 0L)).longValue(),
                (BigDecimal) libraryStats.getOrDefault("totalUnpaidFines", BigDecimal.ZERO)
        );

        var response = DashboardStatsResponse.builder()
                .overview(overview)
                .revenue(revenue)
                .orders(orders)
                .library(library)
                .build();

        return ApiResponse.success(response);
    }

    /**
     * Get revenue chart data
     */
    @GetMapping("/dashboard/revenue-chart")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getRevenueChart(
            @RequestParam(defaultValue = "7") int days) {
        
        List<DashboardStatsResponse.ChartData> data = orderService.getRevenueByDays(days);
        return ApiResponse.success(data);
    }

    /**
     * Get orders chart data
     */
    @GetMapping("/dashboard/orders-chart")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getOrdersChart(
            @RequestParam(defaultValue = "7") int days) {
        
        List<DashboardStatsResponse.ChartData> data = orderService.getOrderCountByDays(days);
        return ApiResponse.success(data);
    }

    /**
     * Get user registration chart data
     */
    @GetMapping("/dashboard/users-chart")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getUsersChart(
            @RequestParam(defaultValue = "7") int days) {
        
        List<DashboardStatsResponse.ChartData> data = userRepository.getUserRegistrationByDays(days);
        return ApiResponse.success(data);
    }

    /**
     * Get top selling books
     */
    @GetMapping("/dashboard/top-selling-books")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.TopItem>> getTopSellingBooks(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<DashboardStatsResponse.TopItem> data = orderService.getTopSellingBooks(limit);
        return ApiResponse.success(data);
    }

    /**
     * Get top borrowed books
     */
    @GetMapping("/dashboard/top-borrowed-books")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.TopItem>> getTopBorrowedBooks(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<DashboardStatsResponse.TopItem> data = libraryReportService.getTopBorrowedBooksAsTopItems(limit);
        return ApiResponse.success(data);
    }

    /**
     * Get report data (API for dashboard)
     */
    @GetMapping("/reports/data")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getReportData(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> report = switch (type) {
            case "sales" -> orderService.getSalesReport(startDate, endDate);
            case "library" -> libraryReportService.getPeriodReport(startDate, endDate);
            case "users" -> userRepository.getUserReport(startDate, endDate);
            default -> throw new IllegalArgumentException("Unknown report type: " + type);
        };
        
        return ApiResponse.success(report);
    }
}
