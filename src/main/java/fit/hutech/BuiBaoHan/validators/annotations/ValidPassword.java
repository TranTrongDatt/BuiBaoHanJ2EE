package fit.hutech.BuiBaoHan.validators.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fit.hutech.BuiBaoHan.validators.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates password against security policy
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    
    String message() default "Password does not meet security requirements";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum password length
     */
    int minLength() default 8;
    
    /**
     * Maximum password length
     */
    int maxLength() default 128;
    
    /**
     * Require at least one uppercase letter
     */
    boolean requireUppercase() default true;
    
    /**
     * Require at least one lowercase letter
     */
    boolean requireLowercase() default true;
    
    /**
     * Require at least one digit
     */
    boolean requireDigit() default true;
    
    /**
     * Require at least one special character
     */
    boolean requireSpecial() default true;
    
    /**
     * Allowed special characters
     */
    String specialChars() default "!@#$%^&*()_+-=[]{}|;:,.<>?";
}
