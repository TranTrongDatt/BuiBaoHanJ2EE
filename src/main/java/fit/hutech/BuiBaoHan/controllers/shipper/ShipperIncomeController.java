package fit.hutech.BuiBaoHan.controllers.shipper;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import fit.hutech.BuiBaoHan.constants.PenaltyType;
import fit.hutech.BuiBaoHan.entities.ShipperConfig;
import fit.hutech.BuiBaoHan.entities.ShipperPenalty;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.ShipperSalary;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IAttendanceRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperConfigRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperPenaltyRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperProfileRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperSalaryRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipper Income Controller
 * Quản lý thu nhập và bảng lương cho nhân viên giao hàng
 */
@Controller
@RequestMapping("/shipper/income")
@PreAuthorize("hasAnyAuthority('ROLE_SHIPPER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ShipperIncomeController {

    private final IUserRepository userRepository;
    private final IShipperProfileRepository shipperProfileRepository;
    private final IShipperSalaryRepository salaryRepository;
    private final IShipperPenaltyRepository penaltyRepository;
    private final IAttendanceRepository attendanceRepository;
    private final IShipperConfigRepository configRepository;

    /**
     * Tổng quan thu nhập
     */
    @GetMapping({"", "/"})
    public String incomeOverview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model, 
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        YearMonth currentYearMonth = YearMonth.now();

        int finalYear = (year == null) ? currentYearMonth.getYear() : year;
        int finalMonth = (month == null) ? currentYearMonth.getMonthValue() : month;

        // Bảng lương tháng hiện tại
        Optional<ShipperSalary> currentSalary = salaryRepository.findByShipperIdAndYearAndMonth(
            profile.getId(), finalYear, finalMonth
        );

        // Lịch sử lương (12 tháng gần nhất)
        List<ShipperSalary> salaryHistory = salaryRepository.findByShipperIdOrderByYearDescMonthDesc(
            profile.getId()
        ).stream().limit(12).toList();

        // Config
        ShipperConfig config = configRepository.getConfig();

        // Thống kê tháng này
        Double totalWorkHours = attendanceRepository.getTotalWorkHoursInMonth(profile.getId(), finalYear, finalMonth);
        Double totalOvertime = attendanceRepository.getTotalOvertimeInMonth(profile.getId(), finalYear, finalMonth);
        long workDaysLong = attendanceRepository.countWorkDaysInMonth(profile.getId(), finalYear, finalMonth);
        int workDays = (int) workDaysLong;
        BigDecimal totalPenalties = penaltyRepository.getTotalPenaltyInMonth(profile.getId(), finalYear, finalMonth);

        // List phạt tháng này
        List<ShipperPenalty> penalties = penaltyRepository.findByShipperIdAndMonth(profile.getId(), finalYear, finalMonth);

        model.addAttribute("profile", profile);
        model.addAttribute("year", finalYear);
        model.addAttribute("month", finalMonth);
        model.addAttribute("monthName", YearMonth.of(finalYear, finalMonth).format(DateTimeFormatter.ofPattern("MM/yyyy")));
        model.addAttribute("currentSalary", currentSalary.orElse(null));
        model.addAttribute("salaryHistory", salaryHistory);
        model.addAttribute("config", config);
        model.addAttribute("totalWorkHours", totalWorkHours != null ? totalWorkHours : 0.0);
        model.addAttribute("totalOvertime", totalOvertime != null ? totalOvertime : 0.0);
        model.addAttribute("workDays", workDays);
        model.addAttribute("totalPenalties", totalPenalties != null ? totalPenalties.doubleValue() : 0.0);
        model.addAttribute("penalties", penalties);

        // Tính lương tạm nếu chưa có bảng lương chính thức
        if (currentSalary.isEmpty() && totalWorkHours != null && totalWorkHours > 0) {
            double estimatedBase = totalWorkHours * config.getHourlyRate().doubleValue();
            double estimatedOvertime = (totalOvertime != null ? totalOvertime : 0) * config.getOvertimeHourlyRate().doubleValue();
            double estimatedMeal = workDays * config.getMealAllowanceDaily().doubleValue();
            double estimatedGas = config.getGasAllowanceMonthly().doubleValue();
            double estimatedPenalty = totalPenalties != null ? totalPenalties.doubleValue() : 0;
            double estimatedTotal = estimatedBase + estimatedOvertime + estimatedMeal + estimatedGas - estimatedPenalty;

            model.addAttribute("estimatedBase", estimatedBase);
            model.addAttribute("estimatedOvertime", estimatedOvertime);
            model.addAttribute("estimatedMeal", estimatedMeal);
            model.addAttribute("estimatedGas", estimatedGas);
            model.addAttribute("estimatedPenalty", estimatedPenalty);
            model.addAttribute("estimatedTotal", estimatedTotal);
        }

        return "shipper/income";
    }

    /**
     * Chi tiết bảng lương
     */
    @GetMapping("/{id}")
    public String salaryDetail(@PathVariable Long id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        Optional<ShipperSalary> salaryOpt = salaryRepository.findById(id);

        if (salaryOpt.isEmpty()) {
            return "redirect:/shipper/income?error=notfound";
        }

        ShipperSalary salary = salaryOpt.get();

        // Kiểm tra quyền
        if (!salary.getShipper().getId().equals(profile.getId())) {
            return "redirect:/shipper/income?error=unauthorized";
        }

        // List penalties của tháng đó
        List<ShipperPenalty> penalties = penaltyRepository.findByShipperIdAndMonth(
            profile.getId(), salary.getSalaryYear(), salary.getSalaryMonth()
        );

        model.addAttribute("profile", profile);
        model.addAttribute("salary", salary);
        model.addAttribute("penalties", penalties);
        model.addAttribute("config", configRepository.getConfig());

        return "shipper/salary-detail";
    }

    /**
     * Danh sách khoản phạt
     */
    @GetMapping("/penalties")
    public String penalties(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model, 
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        YearMonth currentYearMonth = YearMonth.now();

        if (year == null) year = currentYearMonth.getYear();
        if (month == null) month = currentYearMonth.getMonthValue();

        List<ShipperPenalty> penalties = penaltyRepository.findByShipperIdAndMonth(profile.getId(), year, month);

        // Thống kê phạt
        BigDecimal totalLate = penaltyRepository.getTotalLatePenaltyInMonth(
            profile.getId(), year, month
        );
        BigDecimal totalMissing = penaltyRepository.getTotalMissingPunchPenaltyInMonth(
            profile.getId(), year, month
        );
        long latePenaltyCount = penaltyRepository.countPenaltiesByTypeInMonth(
            profile.getId(), PenaltyType.LATE, year, month
        );
        long missingPenaltyCount = penaltyRepository.countPenaltiesByTypeInMonth(
            profile.getId(), PenaltyType.MISSING_PUNCH, year, month
        );

        model.addAttribute("profile", profile);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("penalties", penalties);
        model.addAttribute("totalLate", totalLate != null ? totalLate.doubleValue() : 0.0);
        model.addAttribute("totalMissing", totalMissing != null ? totalMissing.doubleValue() : 0.0);
        model.addAttribute("latePenaltyCount", (int) latePenaltyCount);
        model.addAttribute("missingPenaltyCount", (int) missingPenaltyCount);

        return "shipper/penalties";
    }

    /**
     * Thống kê tổng quan
     */
    @GetMapping("/stats")
    public String incomeStats(
            @RequestParam(required = false) Integer year,
            Model model, 
            Authentication authentication) {
        
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();

        final int finalYear = (year == null) ? YearMonth.now().getYear() : year;

        // Tổng thu nhập năm
        BigDecimal yearlyIncome = salaryRepository.getTotalEarningInYear(profile.getId(), finalYear);
        
        // Số tháng đã được thanh toán (đếm theo list)
        List<ShipperSalary> yearlySalaries = salaryRepository.findByShipperIdOrderByYearDescMonthDesc(profile.getId())
            .stream()
            .filter(s -> s.getSalaryYear() != null && s.getSalaryYear().equals(finalYear))
            .toList();
        long paidMonths = yearlySalaries.stream()
            .filter(s -> s.getStatus() == fit.hutech.BuiBaoHan.constants.SalaryStatus.PAID)
            .count();

        // Tổng giờ làm việc năm (tính tổng từng tháng)
        Double yearlyHours = 0.0;
        for (int m = 1; m <= 12; m++) {
            Double monthHours = attendanceRepository.getTotalWorkHoursInMonth(profile.getId(), finalYear, m);
            if (monthHours != null) yearlyHours += monthHours;
        }

        model.addAttribute("profile", profile);
        model.addAttribute("year", finalYear);
        model.addAttribute("yearlyIncome", yearlyIncome != null ? yearlyIncome.doubleValue() : 0.0);
        model.addAttribute("paidMonths", paidMonths);
        model.addAttribute("yearlyHours", yearlyHours);
        model.addAttribute("yearlySalaries", yearlySalaries);

        return "shipper/income-stats";
    }

    /**
     * Helper method lấy current user
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
