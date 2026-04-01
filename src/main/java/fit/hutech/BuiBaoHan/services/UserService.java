package fit.hutech.BuiBaoHan.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import fit.hutech.BuiBaoHan.constants.Provider;
import fit.hutech.BuiBaoHan.constants.UserStatus;
import fit.hutech.BuiBaoHan.dto.RegisterRequest;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IRoleRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Validated
public class UserService implements UserDetailsService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void save(@NotNull User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setProvider(Provider.LOCAL.value);
        userRepository.save(user);
        log.info("User saved: {}", user.getUsername());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void setDefaultRole(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            roleRepository.findByName("ROLE_USER").ifPresent(role -> {
                user.getRoles().add(role);
                userRepository.save(user);
                log.info("Default role USER assigned to: {}", username);
            });
        });
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    /**
     * Tìm user theo ID
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    /**
     * Kiểm tra username đã tồn tại chưa.
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Kiểm tra username đã tồn tại chưa (case-insensitive).
     */
    public boolean existsByUsernameIgnoreCase(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }
    
    /**
     * Kiểm tra email đã tồn tại chưa.
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Kiểm tra email đã tồn tại chưa (case-insensitive).
     */
    public boolean existsByEmailIgnoreCase(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }
    
    /**
     * Đăng ký user mới từ RegisterRequest.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User registerUser(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(Provider.LOCAL.value)
                .build();
        
        userRepository.save(user);
        log.info("User registered: {}", user.getUsername());
        
        // Gán role USER mặc định
        setDefaultRole(user.getUsername());
        
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void saveOauthUser(String email, @NotNull String username) {
        // Kiểm tra xem user đã tồn tại bằng email chưa
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("OAuth user already exists with email: {}", email);
            return;
        }

        // Kiểm tra username đã tồn tại chưa, nếu có thì thêm suffix
        String finalUsername = username;
        int counter = 1;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + "_" + counter++;
        }

        // Tạo user mới
        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider(Provider.GOOGLE.value)
                .build();

        userRepository.save(user);
        log.info("OAuth user created: {} with email: {}", finalUsername, email);

        // Gán role USER mặc định
        setDefaultRole(finalUsername);
    }

    /**
     * Cập nhật trạng thái online của user (stub - không làm gì)
     */
    public void updateOnlineStatus(Long userId, boolean online) {
        // Stub method - có thể implement sau nếu cần
        log.debug("User {} online status: {}", userId, online);
    }

    // ==================== Profile Management ====================

    /**
     * Cập nhật profile người dùng
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User updateProfile(Long userId, String fullName, String phone, String address, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        if (fullName != null) user.setFullName(fullName);
        if (phone != null) user.setPhone(phone);
        if (address != null) user.setAddress(address);
        if (avatar != null) user.setAvatar(avatar);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("Updated profile for user: {}", userId);
        return updated;
    }

    /**
     * Cập nhật profile người dùng (bao gồm bio)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User updateProfile(Long userId, String fullName, String phone, String address, String bio, String avatar) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        if (fullName != null) user.setFullName(fullName);
        if (phone != null) user.setPhone(phone);
        if (address != null) user.setAddress(address);
        if (bio != null) user.setBio(bio);
        if (avatar != null) user.setAvatar(avatar);
        user.setUpdatedAt(LocalDateTime.now());

        User updated = userRepository.save(user);
        log.info("Updated profile for user: {}", userId);
        return updated;
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password changed for user: {}", userId);
    }

    /**
     * Reset mật khẩu (Admin hoặc forgot password)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public String resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        // Tạo mật khẩu ngẫu nhiên
        String newPassword = UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        log.info("Password reset for user: {}", userId);
        return newPassword;
    }

    /**
     * Cập nhật avatar
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        user.setAvatar(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updated = userRepository.save(user);
        log.info("Updated avatar for user: {}", userId);
        return updated;
    }

    // ==================== Admin User Management ====================

    /**
     * Lấy tất cả users (Admin)
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Tìm kiếm users
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchByKeyword(keyword, pageable);
    }

    /**
     * Kích hoạt/Vô hiệu hóa user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User toggleUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        user.setIsActive(isActive);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updated = userRepository.save(user);
        log.info("{} user: {}", isActive ? "Activated" : "Deactivated", userId);
        return updated;
    }

    /**
     * Khóa user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User lockUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        user.setIsLocked(true);
        user.setLockReason(reason);
        user.setLockedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User locked = userRepository.save(user);
        log.info("Locked user: {} - Reason: {}", userId, reason);
        return locked;
    }

    /**
     * Mở khóa user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        user.setIsLocked(false);
        user.setLockReason(null);
        user.setLockedAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        User unlocked = userRepository.save(user);
        log.info("Unlocked user: {}", userId);
        return unlocked;
    }

    /**
     * Gán role cho user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User assignRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        
        var role = roleRepository.findRoleById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Không tìm thấy role ID: " + roleId);
        }

        user.getRoles().add(role);
        User updated = userRepository.save(user);
        log.info("Assigned role {} to user {}", roleId, userId);
        return updated;
    }

    /**
     * Xóa role khỏi user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public User removeRole(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        User updated = userRepository.save(user);
        log.info("Removed role {} from user {}", roleId, userId);
        return updated;
    }

    /**
     * Xóa user
     */
    @Transactional(isolation = Isolation.SERIALIZABLE,
            rollbackFor = {Exception.class, Throwable.class})
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        userRepository.delete(user);
        log.info("Deleted user: {}", userId);
    }

    // ==================== Statistics ====================

    /**
     * Đếm tổng số users
     */
    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    /**
     * Đếm users active
     */
    @Transactional(readOnly = true)
    public long countActiveUsers() {
        return userRepository.countByIsActiveTrue();
    }

    /**
     * Users đăng ký trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public long countNewUsers(LocalDateTime startDate, LocalDateTime endDate) {
        return userRepository.countByCreatedAtBetween(startDate, endDate);
    }

    /**
     * Lấy users mới nhất
     */
    @Transactional(readOnly = true)
    public List<User> getRecentUsers(int limit) {
        return userRepository.findRecentUsers(PageRequest.of(0, limit));
    }

    // ==================== AdminUserController Support Methods ====================

    /**
     * Get all users with filters
     */
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(String search, String role, String status, Pageable pageable) {
        // Simplified implementation - just returns all users with pagination
        // In real implementation, would apply filters
        if (search != null && !search.isBlank()) {
            return userRepository.searchByKeyword(search, pageable);
        }
        return userRepository.findAll(pageable);
    }

    /**
     * Get user orders
     */
    @Transactional(readOnly = true)
    public List<Object> getUserOrders(Long userId) {
        // Returns empty list - actual implementation would query orders
        return java.util.Collections.emptyList();
    }

    /**
     * Get user library card
     */
    @Transactional(readOnly = true)
    public Object getUserLibraryCard(Long userId) {
        return userRepository.findById(userId)
                .map(User::getLibraryCard)
                .orElse(null);
    }

    /**
     * Toggle user status (active/inactive)
     */
    @Transactional
    public User toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        return userRepository.save(user);
    }

    /**
     * Assign role by name
     */
    @Transactional
    public User assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
        
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Remove role by name
     */
    @Transactional
    public User removeRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.getRoles().removeIf(r -> r.getName().equals(roleName));
        return userRepository.save(user);
    }

    /**
     * Bulk activate users
     */
    @Transactional
    public int bulkActivate(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(user -> {
                user.setIsActive(true);
                userRepository.save(user);
            });
            count++;
        }
        return count;
    }

    /**
     * Bulk deactivate users
     */
    @Transactional
    public int bulkDeactivate(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(user -> {
                user.setIsActive(false);
                userRepository.save(user);
            });
            count++;
        }
        return count;
    }

    /**
     * Bulk lock users
     */
    @Transactional
    public int bulkLock(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(user -> {
                user.setIsLocked(true);
                userRepository.save(user);
            });
            count++;
        }
        return count;
    }

    /**
     * Bulk unlock users
     */
    @Transactional
    public int bulkUnlock(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            userRepository.findById(id).ifPresent(user -> {
                user.setIsLocked(false);
                userRepository.save(user);
            });
            count++;
        }
        return count;
    }

    /**
     * Bulk delete users
     */
    @Transactional
    public int bulkDelete(List<Long> ids) {
        int count = 0;
        for (Long id : ids) {
            if (userRepository.existsById(id)) {
                userRepository.deleteById(id);
                count++;
            }
        }
        return count;
    }

    /**
     * Cấm tài khoản — user không thể đăng nhập
     */
    @Transactional
    public User banUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        
        user.setStatus(UserStatus.BANNED);
        user.setIsActive(false);
        user.setIsLocked(true);
        user.setLockReason(reason != null && !reason.isBlank() ? reason : "Bị cấm bởi admin");
        user.setLockedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User banned = userRepository.save(user);
        log.info("Banned user: {} - Reason: {}", userId, reason);
        return banned;
    }

    /**
     * Mở cấm tài khoản
     */
    @Transactional
    public User unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));
        
        user.setStatus(UserStatus.ACTIVE);
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setLockReason(null);
        user.setLockedAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        
        User unbanned = userRepository.save(user);
        log.info("Unbanned user: {}", userId);
        return unbanned;
    }

    /**
     * Export users to CSV
     */
    @Transactional(readOnly = true)
    public String exportToCsv(String roleFilter) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Username,Email,Full Name,Active,Created At\n");
        
        List<User> users = userRepository.findAll();
        for (User user : users) {
            csv.append(user.getId()).append(",")
                    .append(user.getUsername()).append(",")
                    .append(user.getEmail()).append(",")
                    .append(user.getFullName() != null ? user.getFullName() : "").append(",")
                    .append(user.getIsActive()).append(",")
                    .append(user.getCreatedAt()).append("\n");
        }
        
        return csv.toString();
    }

    // ==================== Profile Controller Support Methods ====================

    /**
     * Lấy danh sách wishlist của user
     */
    @Transactional(readOnly = true)
    public List<Object> getWishlist(User user) {
        // TODO: Implement wishlist repository
        return java.util.Collections.emptyList();
    }

    /**
     * Lấy danh sách địa chỉ của user
     */
    @Transactional(readOnly = true)
    public List<Object> getAddresses(User user) {
        // TODO: Implement address repository
        return java.util.Collections.emptyList();
    }

    /**
     * Lấy cài đặt thông báo của user
     */
    @Transactional(readOnly = true)
    public Object getNotificationSettings(User user) {
        // TODO: Implement notification settings
        return Map.of(
            "emailEnabled", true,
            "pushEnabled", true,
            "smsEnabled", false
        );
    }
}
