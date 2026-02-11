package fit.hutech.BuiBaoHan.services;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Rate Limiting.
 * 
 * Sử dụng Bucket4j với Token Bucket algorithm.
 * 
 * Rate limits:
 * - Login: 5 requests/minute/IP
 * - Register: 3 requests/minute/IP
 * - API: 100 requests/minute/user
 * - Password reset: 3 requests/hour/email
 */
@Service
@Slf4j
public class RateLimitService {

    /**
     * Cache buckets theo key (IP, user ID, email)
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Enum định nghĩa các loại rate limit
     */
    public enum RateLimitType {
        LOGIN(5, Duration.ofMinutes(1)),           // 5 requests/minute
        REGISTER(3, Duration.ofMinutes(1)),        // 3 requests/minute
        API(100, Duration.ofMinutes(1)),           // 100 requests/minute
        PASSWORD_RESET(3, Duration.ofHours(1)),    // 3 requests/hour
        CAPTCHA(10, Duration.ofMinutes(1)),        // 10 requests/minute
        GENERAL(30, Duration.ofMinutes(1));        // 30 requests/minute

        private final int capacity;
        private final Duration period;

        RateLimitType(int capacity, Duration period) {
            this.capacity = capacity;
            this.period = period;
        }

        public int getCapacity() {
            return capacity;
        }

        public Duration getPeriod() {
            return period;
        }
    }

    /**
     * Kiểm tra xem request có được phép không
     * 
     * @param key Unique key (IP, user ID, email)
     * @param type Loại rate limit
     * @return true nếu được phép, false nếu bị rate limited
     */
    public boolean tryConsume(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(type));
        
        boolean consumed = bucket.tryConsume(1);
        
        if (!consumed) {
            log.warn("Rate limit exceeded for key: {} type: {}", key, type);
        }
        
        return consumed;
    }

    /**
     * Lấy số tokens còn lại
     */
    public long getAvailableTokens(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        Bucket bucket = buckets.get(bucketKey);
        return bucket != null ? bucket.getAvailableTokens() : type.getCapacity();
    }

    /**
     * Reset bucket cho key (dùng sau khi login thành công)
     */
    public void resetBucket(String key, RateLimitType type) {
        String bucketKey = type.name() + ":" + key;
        buckets.remove(bucketKey);
    }

    /**
     * Tạo bucket với cấu hình từ RateLimitType
     * Sử dụng Bucket4j 8.x fluent API
     */
    private Bucket createBucket(RateLimitType type) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(type.getCapacity())
                        .refillIntervally(type.getCapacity(), type.getPeriod()))
                .build();
    }

    /**
     * Kiểm tra login rate limit
     */
    public boolean checkLoginRateLimit(String ipAddress) {
        return tryConsume(ipAddress, RateLimitType.LOGIN);
    }

    /**
     * Kiểm tra register rate limit
     */
    public boolean checkRegisterRateLimit(String ipAddress) {
        return tryConsume(ipAddress, RateLimitType.REGISTER);
    }

    /**
     * Kiểm tra API rate limit
     */
    public boolean checkApiRateLimit(String userId) {
        return tryConsume(userId, RateLimitType.API);
    }

    /**
     * Kiểm tra password reset rate limit
     */
    public boolean checkPasswordResetRateLimit(String email) {
        return tryConsume(email, RateLimitType.PASSWORD_RESET);
    }

    /**
     * Cleanup old buckets (call periodically)
     */
    public void cleanup() {
        // Remove buckets that haven't been used for a while
        // This is a simple implementation; in production, use Caffeine cache with expiry
        log.debug("Rate limit buckets count: {}", buckets.size());
    }
}
