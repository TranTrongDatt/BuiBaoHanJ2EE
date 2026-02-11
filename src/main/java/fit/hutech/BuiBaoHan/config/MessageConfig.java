package fit.hutech.BuiBaoHan.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Internationalization (i18n) configuration
 */
@Configuration
public class MessageConfig implements WebMvcConfigurer {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        
        // Message file locations
        messageSource.setBasenames(
                "classpath:messages/messages",
                "classpath:messages/validation",
                "classpath:messages/error"
        );
        
        // Default encoding
        messageSource.setDefaultEncoding("UTF-8");
        
        // Cache refresh interval (5 minutes in development)
        messageSource.setCacheSeconds(300);
        
        // Use message format
        messageSource.setAlwaysUseMessageFormat(true);
        
        // Fallback to system locale
        messageSource.setFallbackToSystemLocale(true);
        
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver("MV_LOCALE");
        
        // Default locale
        localeResolver.setDefaultLocale(Locale.of("vi", "VN"));
        
        // Cookie max age (30 days)
        localeResolver.setCookieMaxAge(Duration.ofSeconds(2592000));
        
        // Cookie path
        localeResolver.setCookiePath("/");
        
        return localeResolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        
        // Parameter name for locale change
        interceptor.setParamName("lang");
        
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
