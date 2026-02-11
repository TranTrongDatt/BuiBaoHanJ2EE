package fit.hutech.BuiBaoHan.validators;

import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.validators.annotations.UniqueEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

/**
 * Validator for unique email constraint
 */
@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {
    
    private final IUserRepository userRepository;
    private boolean ignoreCase;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true; // Use @NotBlank for null check
        }
        
        if (ignoreCase) {
            return !userRepository.existsByEmailIgnoreCase(email);
        }
        
        return !userRepository.existsByEmail(email);
    }
}
