package fit.hutech.BuiBaoHan.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Cache configuration using Caffeine
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Default cache manager
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return cacheManager;
    }

    /**
     * Short-lived cache for frequently changing data
     */
    @Bean("shortLivedCacheManager")
    public CacheManager shortLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "bookCounts",
                "cartItems",
                "unreadNotifications"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats());
        return cacheManager;
    }

    /**
     * Long-lived cache for rarely changing data
     */
    @Bean("longLivedCacheManager")
    public CacheManager longLivedCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "categories",
                "fields",
                "publishers",
                "authors",
                "roles"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(20)
                .maximumSize(100)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats());
        return cacheManager;
    }

    /**
     * Book details cache
     */
    @Bean("bookCacheManager")
    public CacheManager bookCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "books",
                "bookDetails",
                "relatedBooks",
                "featuredBooks"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats());
        return cacheManager;
    }

    /**
     * User session cache
     */
    @Bean("userCacheManager")
    public CacheManager userCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "userDetails",
                "userProfile",
                "userSettings"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(15))
                .expireAfterAccess(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }

    /**
     * Search results cache
     */
    @Bean("searchCacheManager")
    public CacheManager searchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "searchResults",
                "suggestions",
                "popularSearches"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(2000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats());
        return cacheManager;
    }
}
