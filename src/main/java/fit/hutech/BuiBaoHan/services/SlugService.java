package fit.hutech.BuiBaoHan.services;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Service tạo slug từ text (dùng cho URL thân thiện)
 */
@Service
public class SlugService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGESDHASHES = Pattern.compile("(^-|-$)");

    // Vietnamese character mapping
    private static final String[][] VIETNAMESE_CHARS = {
            {"à", "á", "ạ", "ả", "ã", "â", "ầ", "ấ", "ậ", "ẩ", "ẫ", "ă", "ằ", "ắ", "ặ", "ẳ", "ẵ"},
            {"è", "é", "ẹ", "ẻ", "ẽ", "ê", "ề", "ế", "ệ", "ể", "ễ"},
            {"ì", "í", "ị", "ỉ", "ĩ"},
            {"ò", "ó", "ọ", "ỏ", "õ", "ô", "ồ", "ố", "ộ", "ổ", "ỗ", "ơ", "ờ", "ớ", "ợ", "ở", "ỡ"},
            {"ù", "ú", "ụ", "ủ", "ũ", "ư", "ừ", "ứ", "ự", "ử", "ữ"},
            {"ỳ", "ý", "ỵ", "ỷ", "ỹ"},
            {"đ"}
    };

    private static final String[] VIETNAMESE_REPLACEMENTS = {"a", "e", "i", "o", "u", "y", "d"};

    /**
     * Tạo slug từ text
     * @param input Text đầu vào
     * @return Slug (lowercase, no special chars, dashes for spaces)
     */
    public String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String result = input.toLowerCase(Locale.ROOT);
        
        // Convert Vietnamese characters
        result = convertVietnamese(result);
        
        // Normalize unicode
        result = Normalizer.normalize(result, Normalizer.Form.NFD);
        
        // Replace whitespace with dash
        result = WHITESPACE.matcher(result).replaceAll("-");
        
        // Remove non-latin characters
        result = NONLATIN.matcher(result).replaceAll("");
        
        // Remove leading/trailing dashes
        result = EDGESDHASHES.matcher(result).replaceAll("");
        
        // Remove consecutive dashes
        result = result.replaceAll("-+", "-");

        return result;
    }

    /**
     * Tạo unique slug bằng cách thêm suffix nếu cần
     * @param input Text đầu vào
     * @param existsChecker Function kiểm tra slug đã tồn tại chưa
     * @return Unique slug
     */
    public String toUniqueSlug(String input, java.util.function.Predicate<String> existsChecker) {
        String baseSlug = toSlug(input);
        
        if (!existsChecker.test(baseSlug)) {
            return baseSlug;
        }

        int counter = 1;
        String uniqueSlug;
        do {
            uniqueSlug = baseSlug + "-" + counter++;
        } while (existsChecker.test(uniqueSlug));

        return uniqueSlug;
    }

    /**
     * Convert Vietnamese characters to ASCII equivalents
     */
    private String convertVietnamese(String input) {
        String result = input;
        for (int i = 0; i < VIETNAMESE_CHARS.length; i++) {
            for (String vChar : VIETNAMESE_CHARS[i]) {
                result = result.replace(vChar, VIETNAMESE_REPLACEMENTS[i]);
                result = result.replace(vChar.toUpperCase(), VIETNAMESE_REPLACEMENTS[i]);
            }
        }
        return result;
    }
}
