package fit.hutech.BuiBaoHan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import fit.hutech.BuiBaoHan.entities.Author;
import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.entities.Publisher;
import fit.hutech.BuiBaoHan.repositories.IAuthorRepository;
import fit.hutech.BuiBaoHan.repositories.ICategoryRepository;
import fit.hutech.BuiBaoHan.repositories.IPublisherRepository;
import lombok.RequiredArgsConstructor;

/**
 * Configuration class để đăng ký các Converter cho Thymeleaf form binding
 * Cho phép convert String ID thành Entity objects
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ICategoryRepository categoryRepository;
    private final IAuthorRepository authorRepository;
    private final IPublisherRepository publisherRepository;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToCategoryConverter());
        registry.addConverter(new StringToAuthorConverter());
        registry.addConverter(new StringToPublisherConverter());
    }

    /**
     * Cấu hình static resource handler cho thư mục uploads
     * Cho phép browser truy cập file upload qua /uploads/**
     */
    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    /**
     * Converter: String (ID) -> Category
     */
    private class StringToCategoryConverter implements Converter<String, Category> {
        @Override
        public Category convert(String source) {
            if (source == null || source.isEmpty() || "null".equals(source)) {
                return null;
            }
            try {
                long id = Long.parseLong(source);
                return categoryRepository.findById(id).orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Converter: String (ID) -> Author
     */
    private class StringToAuthorConverter implements Converter<String, Author> {
        @Override
        public Author convert(String source) {
            if (source == null || source.isEmpty() || "null".equals(source)) {
                return null;
            }
            try {
                long id = Long.parseLong(source);
                return authorRepository.findById(id).orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * Converter: String (ID) -> Publisher
     */
    private class StringToPublisherConverter implements Converter<String, Publisher> {
        @Override
        public Publisher convert(String source) {
            if (source == null || source.isEmpty() || "null".equals(source)) {
                return null;
            }
            try {
                long id = Long.parseLong(source);
                return publisherRepository.findById(id).orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
