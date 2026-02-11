package fit.hutech.BuiBaoHan.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SerializerMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ cho messaging
 * - Exchange: chat.exchange
 * - Queue: chat.queue
 * - Routing key: chat.messages
 * 
 * Chỉ kích hoạt khi rabbitmq.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
public class RabbitMQConfig {

    public static final String CHAT_QUEUE = "chat.queue";
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_ROUTING_KEY = "chat.messages";

    /**
     * Tạo Queue để lưu trữ messages
     */
    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true); // durable = true
    }

    /**
     * Tạo Topic Exchange cho routing messages
     */
    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE);
    }

    /**
     * Bind Queue với Exchange qua routing key
     */
    @Bean
    public Binding chatBinding(Queue chatQueue, TopicExchange chatExchange) {
        return BindingBuilder
                .bind(chatQueue)
                .to(chatExchange)
                .with(CHAT_ROUTING_KEY);
    }

    /**
     * Message Converter để serialize/deserialize messages
     */
    @Bean
    public MessageConverter messageConverter() {
        return new SerializerMessageConverter();
    }

    /**
     * RabbitTemplate với message converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
