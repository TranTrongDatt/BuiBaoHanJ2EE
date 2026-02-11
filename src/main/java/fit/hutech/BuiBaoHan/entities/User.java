package fit.hutech.BuiBaoHan.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import fit.hutech.BuiBaoHan.constants.Gender;
import fit.hutech.BuiBaoHan.constants.UserStatus;
import fit.hutech.BuiBaoHan.validators.annotations.ValidUsername;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", length = 50, unique = true)
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
    @ValidUsername
    private String username;

    @Column(name = "password", length = 250)
    @NotBlank(message = "Password is required")
    private String password;

    @Column(name = "email", length = 100, unique = true)
    @NotBlank(message = "Email is required")
    @Size(min = 1, max = 100, message = "Email must be between 1 and 100 characters")
    @Email
    private String email;

    @Column(name = "phone", length = 15, unique = true)
    @Length(min = 10, max = 15, message = "Phone must be 10-15 characters")
    @Pattern(regexp = "^[0-9]*$", message = "Phone must be number")
    private String phone;

    @Column(name = "full_name", length = 100)
    @Size(max = 100, message = "Full name max 100 characters")
    private String fullName;

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "provider", length = 50)
    @Builder.Default
    private String provider = "LOCAL";

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Quan hệ với LibraryCard (một user có một thẻ thư viện)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private LibraryCard libraryCard;

    // Quan hệ với Cart (một user có một giỏ hàng)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    @Builder.Default
    private Set<Invoice> invoices = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Tài khoản bị khóa hoặc bị cấm → không thể đăng nhập
        if (Boolean.TRUE.equals(isLocked)) return false;
        if (status == UserStatus.LOCKED || status == UserStatus.BANNED) return false;
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Tài khoản inactive hoặc bị cấm → không thể đăng nhập
        if (Boolean.FALSE.equals(isActive)) return false;
        if (status == UserStatus.BANNED) return false;
        return true;
    }

    /**
     * Kiểm tra user có quyền Admin không
     */
    public boolean isAdmin() {
        return roles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()) 
                        || "ADMIN".equals(role.getName()));
    }

    /**
     * Kiểm tra user có quyền Staff không
     */
    public boolean isStaff() {
        return roles.stream()
                .anyMatch(role -> "ROLE_STAFF".equals(role.getName()) 
                        || "STAFF".equals(role.getName()));
    }

    /**
     * Kiểm tra user có quyền Librarian (Thủ thư) không
     */
    public boolean isLibrarian() {
        return roles.stream()
                .anyMatch(role -> "ROLE_LIBRARIAN".equals(role.getName()) 
                        || "LIBRARIAN".equals(role.getName()));
    }

    // ==================== Status helper methods ==
    
    /**
     * Get enabled status (alias for isActive)
     */
    public Boolean getEnabled() {
        return isActive;
    }
    
    /**
     * Get locked status (alias for isLocked)
     */
    public Boolean getLocked() {
        return isLocked;
    }
    
    /**
     * Set enabled status (alias for setIsActive)
     */
    public void setEnabled(Boolean enabled) {
        this.isActive = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        return getId() != null && Objects.equals(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
