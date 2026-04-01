package fit.hutech.BuiBaoHan.controllers.shipper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fit.hutech.BuiBaoHan.constants.AttendanceStatus;
import fit.hutech.BuiBaoHan.constants.PunchType;
import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.entities.Attendance;
import fit.hutech.BuiBaoHan.entities.AttendancePunch;
import fit.hutech.BuiBaoHan.entities.ShipperConfig;
import fit.hutech.BuiBaoHan.entities.ShipperPenalty;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IAttendancePunchRepository;
import fit.hutech.BuiBaoHan.repositories.IAttendanceRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperConfigRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperPenaltyRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperProfileRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipper Attendance Controller
 * Quản lý chấm công cho nhân viên giao hàng
 */
@Controller
@RequestMapping("/shipper/attendance")
@PreAuthorize("hasAnyAuthority('ROLE_SHIPPER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ShipperAttendanceController {

    private final IUserRepository userRepository;
    private final IShipperProfileRepository shipperProfileRepository;
    private final IAttendanceRepository attendanceRepository;
    private final IAttendancePunchRepository punchRepository;
    private final IShipperConfigRepository configRepository;
    private final IShipperPenaltyRepository penaltyRepository;

    // Giờ chấm công tiêu chuẩn
    private static final LocalTime STANDARD_CHECK_IN = LocalTime.of(8, 0);
    @SuppressWarnings("unused")
    private static final LocalTime STANDARD_CHECK_OUT = LocalTime.of(17, 0);

    /**
     * Trang chấm công
     */
    @GetMapping({"", "/"})
    public String attendancePage(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        LocalDate today = LocalDate.now();

        // Attendance hôm nay
        Optional<Attendance> todayAttendance = attendanceRepository.findTodayAttendanceWithPunches(
            profile.getId(), today
        );

        model.addAttribute("profile", profile);
        model.addAttribute("today", today);
        model.addAttribute("attendance", todayAttendance.orElse(null));
        model.addAttribute("currentTime", LocalTime.now());

        // Xác định punch tiếp theo cần làm
        if (todayAttendance.isEmpty()) {
            model.addAttribute("nextPunchType", PunchType.CHECK_IN);
        } else {
            Attendance att = todayAttendance.get();
            model.addAttribute("nextPunchType", att.getNextPunchType());
        }

        // Config để hiển thị quy định
        ShipperConfig config = configRepository.getConfig();
        model.addAttribute("config", config);

        return "shipper/attendance";
    }

    /**
     * Lịch sử chấm công
     */
    @GetMapping("/history")
    public String attendanceHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
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

        // Default: 30 ngày gần nhất
        if (from == null) {
            from = LocalDate.now().minusDays(30);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        List<Attendance> attendances = attendanceRepository.findByShipperIdAndDateRange(
            profile.getId(), from, to
        );

        // Thống kê tháng hiện tại
        YearMonth currentMonth = YearMonth.now();
        int year = currentMonth.getYear();
        int month = currentMonth.getMonthValue();

        model.addAttribute("profile", profile);
        model.addAttribute("attendances", attendances);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("workDays", attendanceRepository.countWorkDaysInMonth(profile.getId(), year, month));
        model.addAttribute("lateDays", attendanceRepository.countLateDaysInMonth(profile.getId(), year, month));
        model.addAttribute("totalWorkHours", attendanceRepository.getTotalWorkHoursInMonth(profile.getId(), year, month));

        return "shipper/attendance-history";
    }

    /**
     * API chấm công (CHECK_IN, BREAK_START, BREAK_END, CHECK_OUT)
     */
    @PostMapping("/punch")
    @Transactional
    @ResponseBody
    public ResponseEntity<ApiResponse<Object>> punch(
            @RequestParam PunchType punchType,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
            }

            ShipperProfile profile = profileOpt.get();
            LocalDate today = LocalDate.now();
            LocalDateTime now = LocalDateTime.now();
            ShipperConfig config = configRepository.getConfig();

            // Tìm hoặc tạo attendance hôm nay
            Attendance attendance = attendanceRepository.findByShipperIdAndWorkDate(profile.getId(), today)
                .orElseGet(() -> {
                    Attendance newAtt = new Attendance();
                    newAtt.setShipper(profile);
                    newAtt.setWorkDate(today);
                    newAtt.setStatus(AttendanceStatus.INCOMPLETE);
                    return attendanceRepository.save(newAtt);
                });

            // Kiểm tra đã punch loại này chưa
            if (punchRepository.existsByAttendanceIdAndPunchType(attendance.getId(), punchType)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Bạn đã chấm công " + punchType.getDisplayName() + " hôm nay")
                );
            }

            // Kiểm tra thứ tự punch (CHECK_IN phải trước BREAK_START, etc.)
            PunchType nextExpected = attendance.getNextPunchType();
            if (nextExpected != null && punchType != nextExpected) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Vui lòng chấm công " + nextExpected.getDisplayName() + " trước")
                );
            }

            // Tạo punch mới
            AttendancePunch punch = new AttendancePunch();
            punch.setAttendance(attendance);
            punch.setPunchType(punchType);
            punch.setPunchTime(now);
            if (latitude != null) {
                punch.setLatitude(BigDecimal.valueOf(latitude));
            }
            if (longitude != null) {
                punch.setLongitude(BigDecimal.valueOf(longitude));
            }

            // Kiểm tra đi trễ (chỉ áp dụng cho CHECK_IN)
            if (punchType == PunchType.CHECK_IN) {
                LocalTime checkInTime = now.toLocalTime();
                if (checkInTime.isAfter(STANDARD_CHECK_IN)) {
                    int lateMinutes = (int) java.time.Duration.between(STANDARD_CHECK_IN, checkInTime).toMinutes();
                    punch.setIsLate(true);
                    punch.setLateMinutes(lateMinutes);
                    attendance.setLateMinutes(lateMinutes);

                    // Tạo penalty cho đi trễ
                    ShipperPenalty penalty = ShipperPenalty.createLatePenalty(
                        profile.getUser(), attendance, lateMinutes, config.getLatePenaltyPerMinute()
                    );
                    penaltyRepository.save(penalty);
                    
                    log.info("Shipper {} check-in trễ {} phút", profile.getFullName(), lateMinutes);
                }
            }

            // Lưu punch
            punch.setVerified(true);
            punchRepository.save(punch);

            // Thêm punch vào attendance
            attendance.addPunch(punch);

            // Nếu là CHECK_OUT, tính toán giờ làm việc
            if (punchType == PunchType.CHECK_OUT) {
                attendance.calculateWorkHours(config);
                attendance.calculateNetEarning(config);
                attendance.setStatus(AttendanceStatus.COMPLETE);
            }

            attendanceRepository.save(attendance);

            String message = "Chấm công " + punchType.getDisplayName() + " thành công!";
            if (punch.getIsLate()) {
                message += " (Trễ " + punch.getLateMinutes() + " phút)";
            }

            return ResponseEntity.ok(ApiResponse.success(message, attendance));

        } catch (Exception e) {
            log.error("Lỗi chấm công: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra: " + e.getMessage()));
        }
    }

    /**
     * API lấy trạng thái chấm công hôm nay
     */
    @GetMapping("/today")
    @ResponseBody
    public ResponseEntity<ApiResponse<Attendance>> getTodayAttendance(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chưa có hồ sơ shipper"));
        }

        Optional<Attendance> attendance = attendanceRepository.findTodayAttendanceWithPunches(
            profileOpt.get().getId(), LocalDate.now()
        );

        return ResponseEntity.ok(ApiResponse.success("OK", attendance.orElse(null)));
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
