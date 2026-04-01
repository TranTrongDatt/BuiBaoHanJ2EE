package fit.hutech.BuiBaoHan.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * RabbitMQ Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMQProperties {
    
    /**
     * Whether RabbitMQ is enabled
     */
    private boolean enabled = false;
    
    /**
     * RabbitMQ host
     */
    private String host = "localhost";
    
    /**
     * RabbitMQ port
     */
    private int port = 5672;
    
    /**
     * RabbitMQ username
     */
    private String username = "guest";
    
    /**
     * RabbitMQ password
     */
    private String password = "guest";
    
    /**
     * Virtual host
     */
    private String virtualHost = "/";
}
