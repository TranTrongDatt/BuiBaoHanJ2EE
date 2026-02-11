package fit.hutech.BuiBaoHan.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import fit.hutech.BuiBaoHan.validators.annotations.ValidUsername;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class ValidUsernameValidator implements
        ConstraintValidator<ValidUsername, String> {
    
    @Autowired
    private IUserRepository userRepository;
    
    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (userRepository == null) {
            return true; // Skip validation if repository not available
        }
        if (username == null || username.isEmpty()) {
            return true; // Let @NotBlank handle empty validation
        }
        // findByUsername returns Optional<User>, which is NEVER null.
        // Must use .isEmpty() instead of == null check.
        return userRepository.findByUsername(username).isEmpty();
    }
}
