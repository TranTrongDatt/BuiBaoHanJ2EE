package fit.hutech.BuiBaoHan.validators;

import fit.hutech.BuiBaoHan.validators.annotations.ValidISBN;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for ISBN-10 and ISBN-13 formats
 */
public class ISBNValidator implements ConstraintValidator<ValidISBN, String> {
    
    private boolean allowISBN10;
    private boolean allowISBN13;
    private boolean allowHyphens;

    @Override
    public void initialize(ValidISBN constraintAnnotation) {
        this.allowISBN10 = constraintAnnotation.allowISBN10();
        this.allowISBN13 = constraintAnnotation.allowISBN13();
        this.allowHyphens = constraintAnnotation.allowHyphens();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Use @NotBlank for null check
        }
        
        String isbn = allowHyphens ? value.replace("-", "").replace(" ", "") : value;
        
        if (allowISBN10 && isbn.length() == 10) {
            return isValidISBN10(isbn);
        }
        
        if (allowISBN13 && isbn.length() == 13) {
            return isValidISBN13(isbn);
        }
        
        return false;
    }

    /**
     * Validate ISBN-10 checksum
     */
    private boolean isValidISBN10(String isbn) {
        if (!isbn.matches("^\\d{9}[\\dX]$")) {
            return false;
        }
        
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (10 - i) * Character.getNumericValue(isbn.charAt(i));
        }
        
        char lastChar = isbn.charAt(9);
        int lastDigit = (lastChar == 'X' || lastChar == 'x') ? 10 : Character.getNumericValue(lastChar);
        sum += lastDigit;
        
        return sum % 11 == 0;
    }

    /**
     * Validate ISBN-13 checksum
     */
    private boolean isValidISBN13(String isbn) {
        if (!isbn.matches("^\\d{13}$")) {
            return false;
        }
        
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == Character.getNumericValue(isbn.charAt(12));
    }
}
