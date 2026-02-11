package fit.hutech.BuiBaoHan.validators.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fit.hutech.BuiBaoHan.validators.ISBNValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates ISBN-10 or ISBN-13 format
 */
@Documented
@Constraint(validatedBy = ISBNValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidISBN {
    
    String message() default "Invalid ISBN format";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow ISBN-10 format
     */
    boolean allowISBN10() default true;
    
    /**
     * Whether to allow ISBN-13 format
     */
    boolean allowISBN13() default true;
    
    /**
     * Whether to allow hyphens in the ISBN
     */
    boolean allowHyphens() default true;
}
