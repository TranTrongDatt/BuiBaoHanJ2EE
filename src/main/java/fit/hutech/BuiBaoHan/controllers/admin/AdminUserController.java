package fit.hutech.BuiBaoHan.controllers.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import fit.hutech.BuiBaoHan.entities.Role;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IRoleRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.services.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Admin User Management Controller
 */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final IRoleRepository roleRepository;
    private final IUserRepository userRepository;

    /**
     * List all users
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String listUsers(
            Model model,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        
        Page<User> users = userService.getAllUsers(search, role, status, pageable);
        
        // Pass roles as simple maps to avoid entity serialization issues in JS
        List<Map<String, Object>> roleList = roleRepository.findAll().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "name", r.getName(),
                        "description", r.getDescription() != null ? r.getDescription() : ""
                ))
                .collect(Collectors.toList());

        model.addAttribute("users", users);
        model.addAttribute("roles", roleList);
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("status", status);
        
        return "admin/users";
    }

    /**
     * View user details
     */
    @GetMapping("/{id}")
    public String viewUser(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return userService.findById(id)
                .map(user -> {
                    model.addAttribute("user", user);
                    model.addAttribute("roles", roleRepository.findAll());
                    model.addAttribute("orders", userService.getUserOrders(id));
                    model.addAttribute("libraryCard", userService.getUserLibraryCard(id));
                    return "admin/users/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found");
                    return "redirect:/admin/users";
                });
    }

    /**
     * Toggle user status (active/inactive)
     */
    @PostMapping("/{id}/toggle-status")
    @ResponseBody
    public ApiResponse<UserStatusResponse> toggleStatus(@PathVariable Long id) {
        try {
            User user = userService.toggleUserStatus(id);
            return ApiResponse.success(
                    user.getEnabled() ? "User activated" : "User deactivated",
                    new UserStatusResponse(user.getId(), user.getEnabled(), user.getLocked())
            );
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Lock user account
     */
    @PostMapping("/{id}/lock")
    @ResponseBody
    public ApiResponse<UserStatusResponse> lockUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            User user = userService.lockUser(id, reason);
            return ApiResponse.success("User locked", 
                    new UserStatusResponse(user.getId(), user.getEnabled(), user.getLocked()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Unlock user account
     */
    @PostMapping("/{id}/unlock")
    @ResponseBody
    public ApiResponse<UserStatusResponse> unlockUser(@PathVariable Long id) {
        try {
            User user = userService.unlockUser(id);
            return ApiResponse.success("User unlocked",
                    new UserStatusResponse(user.getId(), user.getEnabled(), user.getLocked()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Cấm tài khoản (BANNED)
     */
    @PostMapping("/{id}/ban")
    @ResponseBody
    public ApiResponse<UserStatusResponse> banUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            User user = userService.banUser(id, reason);
            return ApiResponse.success("Đã cấm tài khoản",
                    new UserStatusResponse(user.getId(), user.getEnabled(), user.getLocked()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Mở cấm tài khoản
     */
    @PostMapping("/{id}/unban")
    @ResponseBody
    public ApiResponse<UserStatusResponse> unbanUser(@PathVariable Long id) {
        try {
            User user = userService.unbanUser(id);
            return ApiResponse.success("Đã mở cấm tài khoản",
                    new UserStatusResponse(user.getId(), user.getEnabled(), user.getLocked()));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Set single role for user (replace all existing roles)
     */
    @PostMapping("/{id}/roles/set")
    @ResponseBody
    public ApiResponse<Set<String>> setRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        try {
            User user = userService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
            
            Role targetRole = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            
            // Xóa tất cả roles cũ, gán đúng 1 role mới
            // Dùng userRepository.save() trực tiếp để KHÔNG encode lại password và KHÔNG reset provider
            user.getRoles().clear();
            user.getRoles().add(targetRole);
            userRepository.save(user);
            
            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
            return ApiResponse.success("Role updated", roles);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{id}/roles/add")
    @ResponseBody
    public ApiResponse<Set<String>> assignRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        try {
            User user = userService.assignRole(id, roleName);
            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(java.util.stream.Collectors.toSet());
            return ApiResponse.success("Role assigned", roles);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Remove role from user
     */
    @PostMapping("/{id}/roles/remove")
    @ResponseBody
    public ApiResponse<Set<String>> removeRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        try {
            User user = userService.removeRole(id, roleName);
            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(java.util.stream.Collectors.toSet());
            return ApiResponse.success("Role removed", roles);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Reset user password
     */
    @PostMapping("/{id}/reset-password")
    @ResponseBody
    public ApiResponse<String> resetPassword(@PathVariable Long id) {
        try {
            String tempPassword = userService.resetPassword(id);
            return ApiResponse.success("Password reset", tempPassword);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Delete user
     */
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Bulk actions
     */
    @PostMapping("/bulk-action")
    @ResponseBody
    public ApiResponse<Integer> bulkAction(
            @RequestParam List<Long> ids,
            @RequestParam String action) {
        try {
            int count = switch (action) {
                case "activate" -> userService.bulkActivate(ids);
                case "deactivate" -> userService.bulkDeactivate(ids);
                case "lock" -> userService.bulkLock(ids);
                case "unlock" -> userService.bulkUnlock(ids);
                case "delete" -> userService.bulkDelete(ids);
                default -> throw new IllegalArgumentException("Unknown action: " + action);
            };
            return ApiResponse.success("Action completed for " + count + " users", count);
        } catch (RuntimeException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * Export users to CSV
     */
    @GetMapping("/export")
    @ResponseBody
    public String exportUsers(@RequestParam(required = false) String role) {
        return userService.exportToCsv(role);
    }

    public record UserStatusResponse(Long id, Boolean enabled, Boolean locked) {}
}
