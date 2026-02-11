package fit.hutech.BuiBaoHan.validators;

import java.util.ArrayList;
import java.util.List;

import fit.hutech.BuiBaoHan.validators.annotations.ValidPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for password security policy
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    private int minLength;
    private int maxLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;
    private String specialChars;

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.maxLength = constraintAnnotation.maxLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecial = constraintAnnotation.requireSpecial();
        this.specialChars = constraintAnnotation.specialChars();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true; // Use @NotBlank for null check
        }
        
        List<String> violations = new ArrayList<>();
        
        // Length check
        if (password.length() < minLength) {
            violations.add(String.format("at least %d characters", minLength));
        }
        if (password.length() > maxLength) {
            violations.add(String.format("at most %d characters", maxLength));
        }
        
        // Uppercase check
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            violations.add("one uppercase letter");
        }
        
        // Lowercase check
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            violations.add("one lowercase letter");
        }
        
        // Digit check
        if (requireDigit && !password.matches(".*\\d.*")) {
            violations.add("one digit");
        }
        
        // Special character check
        if (requireSpecial && !containsSpecialChar(password)) {
            violations.add("one special character (" + specialChars + ")");
        }
        
        if (!violations.isEmpty()) {
            // Build custom message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Password must contain: " + String.join(", ", violations)
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }

    private boolean containsSpecialChar(String password) {
        for (char c : password.toCharArray()) {
            if (specialChars.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
}
