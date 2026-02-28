package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.constants.BorrowStatus;
import fit.hutech.BuiBaoHan.constants.CardStatus;
import fit.hutech.BuiBaoHan.constants.FineStatus;
import fit.hutech.BuiBaoHan.dto.DashboardStatsResponse;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IBorrowRecordRepository;
import fit.hutech.BuiBaoHan.repositories.IFineRepository;
import fit.hutech.BuiBaoHan.repositories.ILibraryCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service báo cáo thư viện
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LibraryReportService {

    private final IBookRepository bookRepository;
    private final ILibraryCardRepository libraryCardRepository;
    private final IBorrowRecordRepository borrowRecordRepository;
    private final IFineRepository fineRepository;

    /**
     * Báo cáo tổng quan thư viện
     */
    public Map<String, Object> getLibraryOverview() {
        Map<String, Object> report = new HashMap<>();

        // Sách
        report.put("totalBooks", bookRepository.count());
        report.put("totalLibraryStock", bookRepository.sumLibraryStock());
        
        // Thẻ thư viện
        report.put("totalCards", libraryCardRepository.count());
        report.put("activeCards", libraryCardRepository.countByStatus(CardStatus.ACTIVE));
        
        // Mượn/Trả
        report.put("totalBorrowRecords", borrowRecordRepository.count());
        report.put("currentlyBorrowed", borrowRecordRepository.countByStatus(BorrowStatus.BORROWING));
        report.put("overdueRecords", borrowRecordRepository.countOverdue(LocalDate.now()));
        
        // Phạt
        report.put("totalUnpaidFines", fineRepository.sumByStatus(FineStatus.PENDING));
        report.put("pendingFineCount", fineRepository.countByStatus(FineStatus.PENDING));

        return report;
    }

    /**
     * Thống kê mượn sách theo tháng
     */
    public Map<String, Long> getBorrowStatsByMonth(int year) {
        Map<String, Long> stats = new HashMap<>();
        
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
            LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            
            long count = borrowRecordRepository.countByBorrowDateBetween(start, end);
            stats.put(String.format("%d-%02d", year, month), count);
        }
        
        return stats;
    }

    /**
     * Top sách được mượn nhiều nhất
     */
    public List<Map<String, Object>> getTopBorrowedBooks(int limit) {
        return borrowRecordRepository.findTopBorrowedBooks(PageRequest.of(0, limit)).stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookId", row[0]);
                    map.put("title", row[1]);
                    map.put("borrowCount", row[2]);
                    return map;
                })
                .toList();
    }

    /**
     * Top người mượn nhiều nhất
     */
    public List<Map<String, Object>> getTopBorrowers(int limit) {
        return borrowRecordRepository.findTopBorrowers(PageRequest.of(0, limit)).stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("userId", row[0]);
                    map.put("username", row[1]);
                    map.put("borrowCount", row[2]);
                    return map;
                })
                .toList();
    }

    /**
     * Thống kê theo danh mục
     */
    public List<Map<String, Object>> getBorrowStatsByCategory() {
        return borrowRecordRepository.countByCategory().stream()
                .map(row -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", row[0]);
                    map.put("count", row[1]);
                    return map;
                })
                .toList();
    }

    /**
     * Báo cáo quá hạn
     */
    public Map<String, Object> getOverdueReport() {
        Map<String, Object> report = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        
        report.put("overdueCount", borrowRecordRepository.countOverdue(today));
        report.put("overdueRecords", borrowRecordRepository.findOverdueRecords(today));
        
        // Phân loại theo số ngày quá hạn
        long overdue1to7 = borrowRecordRepository.countOverdueBetweenDays(today.minusDays(7), today);
        long overdue8to30 = borrowRecordRepository.countOverdueBetweenDays(today.minusDays(30), today.minusDays(7));
        long overdueOver30 = borrowRecordRepository.countOverdueBeforeDate(today.minusDays(30));
        
        report.put("overdue1to7days", overdue1to7);
        report.put("overdue8to30days", overdue8to30);
        report.put("overdueOver30days", overdueOver30);
        
        return report;
    }

    /**
     * Báo cáo thu phạt
     */
    public Map<String, Object> getFineReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        // Tổng phạt trong kỳ
        BigDecimal totalFines = fineRepository.sumAmountBetween(start, end);
        BigDecimal totalCollected = fineRepository.sumPaidBetween(start, end);
        BigDecimal totalWaived = fineRepository.sumWaivedBetween(start, end);
        
        report.put("totalFines", totalFines != null ? totalFines : BigDecimal.ZERO);
        report.put("totalCollected", totalCollected != null ? totalCollected : BigDecimal.ZERO);
        report.put("totalWaived", totalWaived != null ? totalWaived : BigDecimal.ZERO);
        
        // Số lượng
        report.put("fineCount", fineRepository.countBetween(start, end));
        report.put("paidCount", fineRepository.countPaidBetween(start, end));
        report.put("waivedCount", fineRepository.countWaivedBetween(start, end));
        
        return report;
    }

    /**
     * Báo cáo thẻ thư viện
     */
    public Map<String, Object> getCardReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("totalCards", libraryCardRepository.count());
        report.put("activeCards", libraryCardRepository.countByStatus(CardStatus.ACTIVE));
        report.put("suspendedCards", libraryCardRepository.countByStatus(CardStatus.SUSPENDED));
        report.put("expiredCards", libraryCardRepository.countByStatus(CardStatus.EXPIRED));
        
        // Thẻ sắp hết hạn (30 ngày tới)
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        report.put("expiringCards", libraryCardRepository.countExpiringCards(today, thirtyDaysLater));
        
        // Thống kê theo loại thẻ
        report.put("cardsByType", libraryCardRepository.countByCardType());
        
        return report;
    }

    /**
     * Báo cáo tồn kho thư viện
     */
    public Map<String, Object> getInventoryReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("totalBooks", bookRepository.count());
        report.put("totalLibraryStock", bookRepository.sumLibraryStock());
        report.put("availableBooks", bookRepository.countAvailableBooks());
        
        // Sách có tồn thấp
        report.put("lowStockBooks", bookRepository.findLowLibraryStock(5));
        
        // Thống kê theo field
        report.put("booksByField", bookRepository.countByField());
        
        return report;
    }

    /**
     * Tạo báo cáo tổng hợp PDF (trả về data)
     */
    public Map<String, Object> generateMonthlyReport(int year, int month) {
        Map<String, Object> report = new HashMap<>();
        
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        report.put("period", yearMonth.toString());
        report.put("generatedAt", LocalDateTime.now());
        
        // Mượn trong tháng
        report.put("borrowCount", borrowRecordRepository.countByBorrowDateBetween(start, end));
        report.put("returnCount", borrowRecordRepository.countByReturnDateBetween(start, end));
        
        // Thẻ mới trong tháng
        report.put("newCards", libraryCardRepository.countByIssueDateBetween(
                yearMonth.atDay(1), yearMonth.atEndOfMonth()));
        
        // Phạt trong tháng
        report.put("fineReport", getFineReport(yearMonth.atDay(1), yearMonth.atEndOfMonth()));
        
        // Top sách
        report.put("topBooks", borrowRecordRepository.findTopBorrowedBooksInPeriod(start, end, PageRequest.of(0, 10)));
        
        log.info("Generated monthly report for {}", yearMonth);
        return report;
    }

    // ==================== Dashboard Methods ====================

    /**
     * Get overview stats for dashboard
     */
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        
        stats.put("totalCards", libraryCardRepository.count());
        stats.put("activeCards", libraryCardRepository.countByStatus(CardStatus.ACTIVE));
        stats.put("expiringCards", libraryCardRepository.countExpiringCards(today, thirtyDaysLater));
        stats.put("currentlyBorrowed", borrowRecordRepository.countByStatus(BorrowStatus.BORROWING));
        stats.put("overdueRecords", borrowRecordRepository.countOverdue(today));
        stats.put("pendingFines", fineRepository.countByStatus(FineStatus.PENDING));
        
        BigDecimal unpaidFines = fineRepository.sumByStatus(FineStatus.PENDING);
        stats.put("totalUnpaidFines", unpaidFines != null ? unpaidFines : BigDecimal.ZERO);
        
        return stats;
    }

    /**
     * Get period report for dashboard
     */
    public Map<String, Object> getPeriodReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);
        
        report.put("startDate", startDate);
        report.put("endDate", endDate);
        
        // Borrow stats
        report.put("borrowCount", borrowRecordRepository.countByBorrowDateBetween(start, end));
        report.put("returnCount", borrowRecordRepository.countByReturnDateBetween(start, end));
        
        // Fine stats
        report.put("fineReport", getFineReport(startDate, endDate));
        
        // Top books in period
        report.put("topBooks", borrowRecordRepository.findTopBorrowedBooksInPeriod(start, end, PageRequest.of(0, 10)));
        
        return report;
    }

    /**
     * Get top borrowed books for dashboard (returns TopItem list)
     */
    public List<DashboardStatsResponse.TopItem> getTopBorrowedBooksAsTopItems(int limit) {
        return borrowRecordRepository.findTopBorrowedBooks(PageRequest.of(0, limit)).stream()
                .map(row -> new DashboardStatsResponse.TopItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        null, // image
                        ((Number) row[2]).longValue(),
                        BigDecimal.ZERO // amount
                ))
                .toList();
    }

    // ==================== AdminReportController Methods ====================

    /**
     * Get popular books (by borrow count)
     */
    public List<Map<String, Object>> getPopularBooks(int limit) {
        return getTopBorrowedBooks(limit);
    }

    /**
     * Get active members (top borrowers)
     */
    public List<Map<String, Object>> getActiveMembers(int limit) {
        return getTopBorrowers(limit);
    }

    /**
     * Get overdue statistics for dashboard
     */
    public Map<String, Object> getOverdueStats() {
        Map<String, Object> stats = new HashMap<>();
        
        LocalDate today = LocalDate.now();
        
        stats.put("overdueCount", borrowRecordRepository.countOverdue(today));
        
        // Phân loại theo số ngày quá hạn
        try {
            stats.put("overdue1to7days", borrowRecordRepository.countOverdueBetweenDays(today.minusDays(7), today));
            stats.put("overdue8to30days", borrowRecordRepository.countOverdueBetweenDays(today.minusDays(30), today.minusDays(7)));
            stats.put("overdueOver30days", borrowRecordRepository.countOverdueBeforeDate(today.minusDays(30)));
        } catch (Exception e) {
            log.warn("Could not get detailed overdue stats: {}", e.getMessage());
            stats.put("overdue1to7days", 0L);
            stats.put("overdue8to30days", 0L);
            stats.put("overdueOver30days", 0L);
        }
        
        return stats;
    }

    /**
     * Get borrows by days chart data
     */
    public List<DashboardStatsResponse.ChartData> getBorrowsByDays(int days) {
        List<DashboardStatsResponse.ChartData> result = new java.util.ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = borrowRecordRepository.countByBorrowDateBetween(
                    date.atStartOfDay(),
                    date.atTime(23, 59, 59)
            );
            result.add(new DashboardStatsResponse.ChartData(date.toString(), count));
        }
        
        return result;
    }

    /**
     * Get top borrowed books in date range (overload)
     */
    public List<DashboardStatsResponse.TopItem> getTopBorrowedBooks(int limit, LocalDate startDate, LocalDate endDate) {
        return borrowRecordRepository.findTopBorrowedBooksInPeriod(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59),
                PageRequest.of(0, limit)
        ).stream()
                .map(row -> new DashboardStatsResponse.TopItem(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        null,
                        ((Number) row[2]).longValue(),
                        BigDecimal.ZERO
                ))
                .toList();
    }

    /**
     * Export library report
     */
    public Map<String, Object> exportReport(String format, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = getPeriodReport(startDate, endDate);
        report.put("format", format);
        report.put("exportedAt", LocalDateTime.now());
        return report;
    }
}
