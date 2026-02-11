package fit.hutech.BuiBaoHan.validators.annotations;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import fit.hutech.BuiBaoHan.validators.ValidCategoryIdValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({TYPE, FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCategoryIdValidator.class)
@Documented
public @interface ValidCategoryId {

    String message() default "Invalid Category Id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
