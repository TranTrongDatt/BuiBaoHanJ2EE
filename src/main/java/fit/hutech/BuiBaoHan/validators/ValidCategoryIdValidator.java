package fit.hutech.BuiBaoHan.validators;

import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.validators.annotations.ValidCategoryId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class ValidCategoryIdValidator implements
ConstraintValidator<ValidCategoryId, Category> {
 @Override
 public boolean isValid(Category category,
ConstraintValidatorContext context) {
 return category != null && category.getId() != null;
 }
}
