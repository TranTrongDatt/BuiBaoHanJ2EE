package fit.hutech.BuiBaoHan.controllers.shipper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import fit.hutech.BuiBaoHan.entities.Attendance;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IAttendanceRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperPenaltyRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperProfileRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipper Dashboard Controller
 * Trang chính của nhân viên giao hàng
 */
@Controller
@RequestMapping("/shipper")
@PreAuthorize("hasAnyAuthority('ROLE_SHIPPER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShipperDashboardController {

    private final IUserRepository userRepository;
    private final IShipperProfileRepository shipperProfileRepository;
    private final IAttendanceRepository attendanceRepository;
    private final IShipperPenaltyRepository penaltyRepository;

    /**
     * Dashboard chính của shipper
     */
    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Lấy profile shipper
        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        
        if (profileOpt.isEmpty()) {
            // Chưa có profile, redirect tới trang tạo profile
            model.addAttribute("message", "Vui lòng hoàn tất hồ sơ nhân viên giao hàng");
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        model.addAttribute("profile", profile);
        model.addAttribute("user", currentUser);

        // Thông tin ngày hôm nay
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        // Attendance hôm nay
        Optional<Attendance> todayAttendance = attendanceRepository.findByShipperIdAndWorkDate(
            profile.getId(), today
        );
        model.addAttribute("todayAttendance", todayAttendance.orElse(null));
        model.addAttribute("hasCheckedIn", todayAttendance.isPresent() && todayAttendance.get().hasCheckedIn());

        // Thống kê đơn hàng hôm nay (TODO: implement shipper-specific queries)
        // model.addAttribute("todayDeliveries", orderRepository.countDeliveriesByShipperAndDate(currentUser.getId(), today));
        // model.addAttribute("pendingDeliveries", orderRepository.countPendingDeliveriesByShipper(currentUser.getId()));
        model.addAttribute("todayDeliveries", 0);
        model.addAttribute("pendingDeliveries", 0);

        // Thống kê tháng này
        model.addAttribute("monthWorkDays", attendanceRepository.countWorkDaysInMonth(profile.getId(), year, month));
        Double totalHours = attendanceRepository.getTotalWorkHoursInMonth(profile.getId(), year, month);
        model.addAttribute("monthWorkHours", totalHours != null ? totalHours : 0.0);
        
        Double overtimeHours = attendanceRepository.getTotalOvertimeHoursInMonth(profile.getId(), year, month);
        model.addAttribute("monthOvertimeHours", overtimeHours != null ? overtimeHours : 0.0);

        BigDecimal monthEarning = attendanceRepository.getTotalNetEarningInMonth(profile.getId(), year, month);
        model.addAttribute("monthEarning", monthEarning != null ? monthEarning : BigDecimal.ZERO);

        // Thống kê penalty tháng này
        BigDecimal totalPenalty = penaltyRepository.getTotalPenaltyInMonth(profile.getId(), year, month);
        model.addAttribute("monthPenalty", totalPenalty != null ? totalPenalty : BigDecimal.ZERO);

        // Rating và số đơn hoàn thành
        model.addAttribute("rating", profile.getRating());
        model.addAttribute("completedDeliveries", profile.getCompletedDeliveries());
        model.addAttribute("totalDeliveries", profile.getTotalDeliveries());
        model.addAttribute("successRate", profile.getSuccessRate());

        // Shipper status
        model.addAttribute("shipperStatus", profile.getStatus());

        return "shipper/dashboard";
    }

    /**
     * Trang setup profile cho shipper mới
     */
    @GetMapping("/profile/setup")
    public String setupProfile(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Kiểm tra đã có profile chưa
        if (shipperProfileRepository.existsByUserId(currentUser.getId())) {
            return "redirect:/shipper/dashboard";
        }

        model.addAttribute("user", currentUser);
        return "shipper/profile-setup";
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
