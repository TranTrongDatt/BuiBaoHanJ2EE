package fit.hutech.BuiBaoHan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket với STOMP protocol
 * - Endpoint: /ws để client kết nối WebSocket
 * - Message broker: /topic (broadcast), /queue (private)
 * - Application prefix: /app
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker cho các destination bắt đầu với /topic và /queue
        // /topic: broadcast messages (public chat rooms)
        // /queue: private messages (user-to-user)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix cho các message gửi từ client đến server (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho private messages gửi đến user cụ thể
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint WebSocket
        // Client sẽ kết nối đến ws://localhost:9090/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Cho phép cross-origin
                .withSockJS();  // Fallback cho các browser không hỗ trợ WebSocket
    }
}
