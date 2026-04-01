package fit.hutech.BuiBaoHan.controllers.shipper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fit.hutech.BuiBaoHan.constants.ContractType;
import fit.hutech.BuiBaoHan.constants.Gender;
import fit.hutech.BuiBaoHan.constants.ShipperStatus;
import fit.hutech.BuiBaoHan.constants.VehicleType;
import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.entities.ShipperConfig;
import fit.hutech.BuiBaoHan.entities.ShipperProfile;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IShipperConfigRepository;
import fit.hutech.BuiBaoHan.repositories.IShipperProfileRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Shipper Account Controller
 * Quản lý tài khoản và hồ sơ shipper
 */
@Controller
@RequestMapping("/shipper/account")
@PreAuthorize("hasAnyAuthority('ROLE_SHIPPER', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ShipperAccountController {

    private final IUserRepository userRepository;
    private final IShipperProfileRepository shipperProfileRepository;
    private final IShipperConfigRepository configRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Trang hồ sơ cá nhân
     */
    @GetMapping({"", "/"})
    public String accountPage(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();
        ShipperConfig config = configRepository.getConfig();

        model.addAttribute("user", currentUser);
        model.addAttribute("profile", profile);
        model.addAttribute("config", config);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("contractTypes", ContractType.values());
        model.addAttribute("statuses", ShipperStatus.values());

        return "shipper/account";
    }

    /**
     * Cập nhật thông tin cơ bản
     */
    @PostMapping("/update-basic")
    @Transactional
    public String updateBasicInfo(
            @RequestParam String fullName,
            @RequestParam Gender gender,
            @RequestParam String phone,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String address,
            @RequestParam(required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return "redirect:/shipper/profile/setup";
            }

            ShipperProfile profile = profileOpt.get();

            // Cập nhật thông tin
            profile.setFullName(fullName);
            profile.setGender(gender);
            profile.setPhone(phone);
            profile.setDateOfBirth(dateOfBirth);
            profile.setAddress(address);

            // Upload avatar nếu có
            if (avatar != null && !avatar.isEmpty()) {
                String avatarUrl = cloudinaryService.uploadImage(avatar, "shipper-avatars");
                profile.setAvatarUrl(avatarUrl);
            }

            shipperProfileRepository.save(profile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
            return "redirect:/shipper/account";

        } catch (IOException | RuntimeException e) {
            log.error("Lỗi cập nhật thông tin: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/shipper/account";
        }
    }

    /**
     * Cập nhật thông tin phương tiện
     */
    @PostMapping("/update-vehicle")
    @Transactional
    public String updateVehicleInfo(
            @RequestParam VehicleType vehicleType,
            @RequestParam String licensePlate,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return "redirect:/shipper/profile/setup";
            }

            ShipperProfile profile = profileOpt.get();

            profile.setVehicleType(vehicleType);
            profile.setLicensePlate(licensePlate);

            shipperProfileRepository.save(profile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin phương tiện thành công!");
            return "redirect:/shipper/account";

        } catch (Exception e) {
            log.error("Lỗi cập nhật phương tiện: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/shipper/account";
        }
    }

    /**
     * Cập nhật thông tin ngân hàng
     */
    @PostMapping("/update-bank")
    @Transactional
    public String updateBankInfo(
            @RequestParam String bankName,
            @RequestParam String bankAccountNumber,
            @RequestParam String bankAccountHolder,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return "redirect:/shipper/profile/setup";
            }

            ShipperProfile profile = profileOpt.get();

            profile.setBankName(bankName);
            profile.setBankAccountNumber(bankAccountNumber);
            profile.setBankAccountHolder(bankAccountHolder);

            shipperProfileRepository.save(profile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin ngân hàng thành công!");
            return "redirect:/shipper/account";

        } catch (Exception e) {
            log.error("Lỗi cập nhật ngân hàng: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/shipper/account";
        }
    }

    /**
     * Cập nhật thông tin bảo hiểm
     */
    @PostMapping("/update-insurance")
    @Transactional
    public String updateInsuranceInfo(
            @RequestParam(required = false) String socialInsuranceNumber,
            @RequestParam(required = false) String healthInsuranceNumber,
            @RequestParam(required = false) String unemploymentInsuranceNumber,
            @RequestParam(required = false) String taxCode,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isEmpty()) {
                return "redirect:/shipper/profile/setup";
            }

            ShipperProfile profile = profileOpt.get();

            profile.setSocialInsuranceNumber(socialInsuranceNumber);
            profile.setHealthInsuranceNumber(healthInsuranceNumber);
            profile.setUnemploymentInsuranceNumber(unemploymentInsuranceNumber);
            profile.setTaxCode(taxCode);

            shipperProfileRepository.save(profile);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin bảo hiểm thành công!");
            return "redirect:/shipper/account";

        } catch (Exception e) {
            log.error("Lỗi cập nhật bảo hiểm: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/shipper/account";
        }
    }

    /**
     * Đăng ký profile mới
     */
    @PostMapping("/register")
    @Transactional
    public String registerProfile(
            @RequestParam String fullName,
            @RequestParam Gender gender,
            @RequestParam String phone,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            @RequestParam String address,
            @RequestParam String idCardNumber,
            @RequestParam VehicleType vehicleType,
            @RequestParam String licensePlate,
            @RequestParam(defaultValue = "PART_TIME") ContractType contractType,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            if (currentUser == null) {
                return "redirect:/login";
            }

            // Kiểm tra đã có profile chưa
            if (shipperProfileRepository.existsByUserId(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã có hồ sơ shipper rồi!");
                return "redirect:/shipper/account";
            }

            // Tạo profile mới
            ShipperProfile profile = new ShipperProfile();
            profile.setUser(currentUser);
            profile.setFullName(fullName);
            profile.setGender(gender);
            profile.setPhone(phone);
            profile.setDateOfBirth(dateOfBirth);
            profile.setAddress(address);
            profile.setIdCardNumber(idCardNumber);
            profile.setVehicleType(vehicleType);
            profile.setLicensePlate(licensePlate);
            profile.setContractType(contractType);
            profile.setStatus(ShipperStatus.OFFLINE);
            profile.setJoinDate(LocalDate.now());
            profile.setRating(new BigDecimal("5.00"));
            profile.setTotalDeliveries(0);
            profile.setSuccessfulDeliveries(0);
            profile.setFailedDeliveries(0);
            profile.setIsActive(true);

            shipperProfileRepository.save(profile);

            log.info("Shipper {} đăng ký profile thành công", fullName);

            redirectAttributes.addFlashAttribute("success", "Đăng ký hồ sơ shipper thành công!");
            return "redirect:/shipper/dashboard";

        } catch (Exception e) {
            log.error("Lỗi đăng ký shipper: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/shipper/profile/setup";
        }
    }

    /**
     * API cập nhật trạng thái
     */
    @PostMapping("/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<ShipperProfile>> updateStatus(
            @RequestParam ShipperStatus status,
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
            profile.setStatus(status);
            shipperProfileRepository.save(profile);

            return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!", profile));

        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái: ", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Có lỗi xảy ra"));
        }
    }

    /**
     * Thống kê cá nhân
     */
    @GetMapping("/stats")
    public String personalStats(Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(currentUser.getId());
        if (profileOpt.isEmpty()) {
            return "redirect:/shipper/profile/setup";
        }

        ShipperProfile profile = profileOpt.get();

        model.addAttribute("profile", profile);
        model.addAttribute("successRate", profile.getSuccessRate());
        model.addAttribute("totalDeliveries", profile.getTotalDeliveries());
        model.addAttribute("successfulDeliveries", profile.getSuccessfulDeliveries());
        model.addAttribute("failedDeliveries", profile.getFailedDeliveries());
        model.addAttribute("rating", profile.getRating());

        return "shipper/personal-stats";
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
