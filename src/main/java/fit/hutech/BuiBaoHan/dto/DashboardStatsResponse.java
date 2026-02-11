package fit.hutech.BuiBaoHan.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin Dashboard statistics response
 */
public record DashboardStatsResponse(
        // Overview Stats
        OverviewStats overview,
        
        // Revenue Stats
        RevenueStats revenue,
        
        // Order Stats
        OrderStats orders,
        
        // Library Stats
        LibraryStats library,
        
        // Charts Data
        List<ChartData> revenueChart,
        List<ChartData> ordersChart,
        List<ChartData> userRegistrationChart,
        
        // Top Items
        List<TopItem> topSellingBooks,
        List<TopItem> topBorrowedBooks,
        List<TopItem> topUsers,
        
        // Recent Activity
        List<ActivityItem> recentOrders,
        List<ActivityItem> recentUsers,
        List<ActivityItem> recentBorrows
) {
    // ==================== Nested Records ====================

    public record OverviewStats(
            long totalUsers,
            long activeUsers,
            long newUsersToday,
            long totalBooks,
            long lowStockBooks,
            long totalOrders,
            long pendingOrders,
            long totalBlogPosts
    ) {}

    public record RevenueStats(
            BigDecimal todayRevenue,
            BigDecimal weekRevenue,
            BigDecimal monthRevenue,
            BigDecimal yearRevenue,
            BigDecimal totalRevenue,
            Double growthPercent
    ) {}

    public record OrderStats(
            long pending,
            long confirmed,
            long shipping,
            long delivered,
            long cancelled,
            long todayOrders,
            long weekOrders,
            long monthOrders
    ) {}

    public record LibraryStats(
            long totalCards,
            long activeCards,
            long expiringCards,
            long currentlyBorrowed,
            long overdueRecords,
            long pendingFines,
            BigDecimal totalUnpaidFines
    ) {}

    public record ChartData(
            String label,
            Number value
    ) {}

    public record TopItem(
            Long id,
            String name,
            String image,
            long count,
            BigDecimal amount
    ) {}

    public record ActivityItem(
            Long id,
            String title,
            String subtitle,
            String status,
            String time,
            String link
    ) {}

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private OverviewStats overview;
        private RevenueStats revenue;
        private OrderStats orders;
        private LibraryStats library;
        private List<ChartData> revenueChart;
        private List<ChartData> ordersChart;
        private List<ChartData> userRegistrationChart;
        private List<TopItem> topSellingBooks;
        private List<TopItem> topBorrowedBooks;
        private List<TopItem> topUsers;
        private List<ActivityItem> recentOrders;
        private List<ActivityItem> recentUsers;
        private List<ActivityItem> recentBorrows;

        public Builder overview(OverviewStats overview) {
            this.overview = overview;
            return this;
        }

        public Builder revenue(RevenueStats revenue) {
            this.revenue = revenue;
            return this;
        }

        public Builder orders(OrderStats orders) {
            this.orders = orders;
            return this;
        }

        public Builder library(LibraryStats library) {
            this.library = library;
            return this;
        }

        public Builder revenueChart(List<ChartData> revenueChart) {
            this.revenueChart = revenueChart;
            return this;
        }

        public Builder ordersChart(List<ChartData> ordersChart) {
            this.ordersChart = ordersChart;
            return this;
        }

        public Builder userRegistrationChart(List<ChartData> userRegistrationChart) {
            this.userRegistrationChart = userRegistrationChart;
            return this;
        }

        public Builder topSellingBooks(List<TopItem> topSellingBooks) {
            this.topSellingBooks = topSellingBooks;
            return this;
        }

        public Builder topBorrowedBooks(List<TopItem> topBorrowedBooks) {
            this.topBorrowedBooks = topBorrowedBooks;
            return this;
        }

        public Builder topUsers(List<TopItem> topUsers) {
            this.topUsers = topUsers;
            return this;
        }

        public Builder recentOrders(List<ActivityItem> recentOrders) {
            this.recentOrders = recentOrders;
            return this;
        }

        public Builder recentUsers(List<ActivityItem> recentUsers) {
            this.recentUsers = recentUsers;
            return this;
        }

        public Builder recentBorrows(List<ActivityItem> recentBorrows) {
            this.recentBorrows = recentBorrows;
            return this;
        }

        public DashboardStatsResponse build() {
            return new DashboardStatsResponse(
                    overview, revenue, orders, library,
                    revenueChart, ordersChart, userRegistrationChart,
                    topSellingBooks, topBorrowedBooks, topUsers,
                    recentOrders, recentUsers, recentBorrows
            );
        }
    }
}
