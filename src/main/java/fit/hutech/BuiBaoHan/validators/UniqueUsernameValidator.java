package fit.hutech.BuiBaoHan.validators;

import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.validators.annotations.UniqueUsername;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * Validator for unique username constraint
 */
@Component
@RequiredArgsConstructor
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {
    
    private final IUserRepository userRepository;
    private boolean ignoreCase;

    @Override
    public void initialize(UniqueUsername constraintAnnotation) {
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) {
            return true; // Use @NotBlank for null check
        }
        
        if (ignoreCase) {
            return !userRepository.existsByUsernameIgnoreCase(username);
        }
        
        return !userRepository.existsByUsername(username);
    }
}
