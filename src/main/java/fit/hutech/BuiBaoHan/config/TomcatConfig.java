package fit.hutech.BuiBaoHan.config;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * Configuration để fix FileCountLimitExceededException trong Tomcat 11.x (Spring Boot 4.x)
 * 
 * Root cause: Spring Boot 4.x đặt maxPartCount = 10 (default rất thấp).
 * Form /admin/books/save có ~24 parts → vượt giới hạn → FileCountLimitExceededException.
 * 
 * Fix: Gọi trực tiếp connector.setMaxPartCount() thay vì connector.setProperty()
 * vì setProperty() set lên ProtocolHandler, không phải Connector.
 * Request.parseParts() đọc từ connector.getMaxPartCount() (getter trên Connector).
 */
@Configuration
public class TomcatConfig {

    /**
     * Customize Tomcat Connector trực tiếp - gọi setter methods trên Connector class.
     * KHÔNG dùng connector.setProperty() vì nó set lên ProtocolHandler, không ảnh hưởng
     * đến Connector.getMaxPartCount() mà Request.parseParts() sử dụng.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers(connector -> {
            // Gọi TRỰC TIẾP setter trên Connector (không phải setProperty)
            connector.setMaxPartCount(200);           // default: 10 trong Spring Boot 4.x
            connector.setMaxParameterCount(10000);     // default: 1000
            connector.setMaxPostSize(200 * 1024 * 1024); // 200MB
        });
    }

    /**
     * MultipartResolver với strict compliance disabled
     */
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setStrictServletCompliance(false);
        return resolver;
    }
}
