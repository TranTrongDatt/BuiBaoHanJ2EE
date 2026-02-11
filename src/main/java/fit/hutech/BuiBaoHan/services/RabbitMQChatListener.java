package fit.hutech.BuiBaoHan.services;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import fit.hutech.BuiBaoHan.config.RabbitMQConfig;
import fit.hutech.BuiBaoHan.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ Listener cho Chat messages
 * Chỉ kích hoạt khi rabbitmq.enabled=true
 */
@Component
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class RabbitMQChatListener {

    private final ChatService chatService;

    /**
     * Nhận message từ queue và broadcast qua WebSocket
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleChatMessage(ChatMessageDto messageDto) {
        log.info("Received message from RabbitMQ: {}", messageDto.getContent());
        chatService.broadcastToRoom(messageDto);
    }
}
