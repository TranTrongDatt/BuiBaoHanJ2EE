package fit.hutech.BuiBaoHan.controllers.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.DashboardStatsResponse;
import fit.hutech.BuiBaoHan.services.LibraryReportService;
import fit.hutech.BuiBaoHan.services.OrderService;
import lombok.RequiredArgsConstructor;

/**
 * Admin Reports Controller
 */
@Controller
@RequestMapping("/admin/reports")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {

    private final OrderService orderService;
    private final LibraryReportService libraryReportService;

    /**
     * Reports dashboard
     */
    @GetMapping
    public String reportsDashboard(Model model) {
        model.addAttribute("reportTypes", List.of(
                new ReportType("sales", "Sales Report", "Revenue and order statistics"),
                new ReportType("inventory", "Inventory Report", "Stock levels and movements"),
                new ReportType("library", "Library Report", "Borrow records and fines"),
                new ReportType("users", "User Report", "User activity and growth"),
                new ReportType("books", "Book Performance", "Sales and ratings by book")
        ));
        return "admin/reports";
    }

    /**
     * Sales report page
     */
    @GetMapping("/sales")
    public String salesReport(
            Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("report", orderService.getSalesReport(startDate, endDate));
        model.addAttribute("revenueChart", orderService.getRevenueByDays(30));
        model.addAttribute("topProducts", orderService.getTopSellingBooks(10));
        
        return "admin/reports/sales";
    }

    /**
     * Sales report data (JSON)
     */
    @GetMapping("/sales/data")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getSalesReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> data = orderService.getSalesReport(startDate, endDate);
        return ApiResponse.success(data);
    }

    /**
     * Library report page
     */
    @GetMapping("/library")
    public String libraryReport(
            Model model,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("report", libraryReportService.getPeriodReport(startDate, endDate));
        model.addAttribute("popularBooks", libraryReportService.getPopularBooks(10));
        model.addAttribute("activeMembers", libraryReportService.getActiveMembers(10));
        
        return "admin/reports/library";
    }

    /**
     * Library report data (JSON)
     */
    @GetMapping("/library/data")
    @ResponseBody
    public ApiResponse<Map<String, Object>> getLibraryReportData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> data = libraryReportService.getPeriodReport(startDate, endDate);
        return ApiResponse.success(data);
    }

    /**
     * Revenue chart data
     */
    @GetMapping("/charts/revenue")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getRevenueChart(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "daily") String interval) {
        
        List<DashboardStatsResponse.ChartData> data = switch (interval) {
            case "weekly" -> orderService.getRevenueByWeeks(days / 7);
            case "monthly" -> orderService.getRevenueByMonths(days / 30);
            default -> orderService.getRevenueByDays(days);
        };
        
        return ApiResponse.success(data);
    }

    /**
     * Orders chart data
     */
    @GetMapping("/charts/orders")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getOrdersChart(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "daily") String interval) {
        
        List<DashboardStatsResponse.ChartData> data = switch (interval) {
            case "weekly" -> orderService.getOrderCountByWeeks(days / 7);
            case "monthly" -> orderService.getOrderCountByMonths(days / 30);
            default -> orderService.getOrderCountByDays(days);
        };
        
        return ApiResponse.success(data);
    }

    /**
     * Borrow chart data
     */
    @GetMapping("/charts/borrows")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.ChartData>> getBorrowsChart(
            @RequestParam(defaultValue = "30") int days) {
        
        List<DashboardStatsResponse.ChartData> data = libraryReportService.getBorrowsByDays(days);
        return ApiResponse.success(data);
    }

    /**
     * Top selling books
     */
    @GetMapping("/top-selling-books")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.TopItem>> getTopSellingBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DashboardStatsResponse.TopItem> data = orderService.getTopSellingBooks(limit, startDate, endDate);
        return ApiResponse.success(data);
    }

    /**
     * Top borrowed books
     */
    @GetMapping("/top-borrowed-books")
    @ResponseBody
    public ApiResponse<List<DashboardStatsResponse.TopItem>> getTopBorrowedBooks(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<DashboardStatsResponse.TopItem> data = libraryReportService.getTopBorrowedBooks(limit, startDate, endDate);
        return ApiResponse.success(data);
    }

    /**
     * Export report
     */
    @GetMapping("/export")
    @ResponseBody
    public Map<String, Object> exportReport(
            @RequestParam String type,
            @RequestParam String format,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return switch (type) {
            case "sales" -> orderService.exportReport(format, startDate, endDate);
            case "library" -> libraryReportService.exportReport(format, startDate, endDate);
            default -> throw new IllegalArgumentException("Unknown report type: " + type);
        };
    }

    public record ReportType(String id, String name, String description) {}
}
