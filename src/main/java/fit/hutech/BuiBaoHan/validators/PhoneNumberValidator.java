package fit.hutech.BuiBaoHan.validators;

import java.util.regex.Pattern;

import fit.hutech.BuiBaoHan.validators.annotations.ValidPhoneNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for Vietnamese phone numbers
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    // Vietnamese phone patterns
    private static final Pattern VN_MOBILE_PATTERN = Pattern.compile(
            "^(0|\\+84)(3[2-9]|5[2689]|7[0-9]|8[1-9]|9[0-9])[0-9]{7}$"
    );
    
    private static final Pattern VN_LANDLINE_PATTERN = Pattern.compile(
            "^(0|\\+84)(2[0-9]{1,2})[0-9]{7,8}$"
    );
    
    private boolean allowInternational;

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowInternational = constraintAnnotation.allowInternational();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotBlank for null check
        }
        
        // Normalize: remove spaces, dashes, dots
        String phone = value.replaceAll("[\\s\\-.]", "");
        
        // Convert international format if allowed
        if (allowInternational && phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        
        // Validate against Vietnamese patterns
        return VN_MOBILE_PATTERN.matcher(phone).matches() 
                || VN_LANDLINE_PATTERN.matcher(phone).matches();
    }
}
