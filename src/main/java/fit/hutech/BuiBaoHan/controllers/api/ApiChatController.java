package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.ChatMessage;
import fit.hutech.BuiBaoHan.entities.ChatRoom;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.AIChatService;
import fit.hutech.BuiBaoHan.services.BlogChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for Chat (AI & User-to-User)
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ApiChatController {

    private final AIChatService aiChatService;
    private final BlogChatService blogChatService;
    private final AuthResolver authResolver;

    // ==================== AI Chat Endpoints ====================

    /**
     * Resolve User entity from SecurityContext principal.
     */
    private User resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return authResolver.resolveUserOrNull(auth.getPrincipal());
    }

    /**
     * Send message to AI chatbot
     */
    @PostMapping("/ai")
    public ResponseEntity<ApiResponse<AIChatResponse>> sendToAI(
            @Valid @RequestBody AIChatRequest request) {
        try {
            User user = resolveUser();
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Vui lòng đăng nhập để sử dụng AI Chat"));
            }
            String response = aiChatService.chat(user, request.message(), request.context());
            var history = aiChatService.getChatHistory(user, 5);
            
            // Lấy sách gợi ý từ DB dựa trên message
            List<BookSuggestion> suggestedBooks = aiChatService.getSuggestedBooks(request.message())
                    .stream().map(BookSuggestion::from).toList();
            
            return ResponseEntity.ok(ApiResponse.success(new AIChatResponse(
                    response,
                    request.message(),
                    history.stream().map(ChatHistoryItem::from).toList(),
                    suggestedBooks
            )));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("AI chat error: " + e.getMessage()));
        }
    }

    /**
     * Get AI chat history
     */
    @GetMapping("/ai/history")
    public ResponseEntity<ApiResponse<List<ChatHistoryItem>>> getAIChatHistory(
            @RequestParam(defaultValue = "20") int limit) {
        User user = resolveUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập"));
        }
        var history = aiChatService.getChatHistory(user, limit).stream()
                .map(ChatHistoryItem::from)
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * Clear AI chat history
     */
    @DeleteMapping("/ai/history")
    public ResponseEntity<ApiResponse<Void>> clearAIChatHistory() {
        User user = resolveUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập"));
        }
        aiChatService.clearHistory(user);
        return ResponseEntity.ok(ApiResponse.success("Chat history cleared"));
    }

    /**
     * Get AI suggestions for book
     */
    @GetMapping("/ai/book-suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getAIBookSuggestions(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String preferences) {
        User user = resolveUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Vui lòng đăng nhập"));
        }
        List<String> suggestions = aiChatService.getBookSuggestions(user, genre, preferences);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    // ==================== User Chat Endpoints ====================

    /**
     * Get my chat rooms
     */
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomSummary>>> getMyChatRooms(
            @AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        List<ChatRoomSummary> rooms = blogChatService.getUserRooms(user).stream()
                .map(room -> ChatRoomSummary.from(room, user))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * Get or create chat room with user
     */
    @PostMapping("/rooms/with/{userId}")
    public ResponseEntity<ApiResponse<ChatRoomDetail>> getOrCreateRoom(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User user = authResolver.resolveUser(principal);
        try {
            ChatRoom room = blogChatService.getOrCreateRoom(user, userId);
            return ResponseEntity.ok(ApiResponse.success(ChatRoomDetail.from(room, user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get chat room messages
     */
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageDto>>> getRoomMessages(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long roomId,
            @PageableDefault(size = 50) Pageable pageable) {
        User user = authResolver.resolveUser(principal);
        try {
            Page<ChatMessage> messages = blogChatService.getRoomMessages(roomId, user, pageable);
            List<ChatMessageDto> dtos = messages.getContent().stream()
                    .map(ChatMessageDto::from)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(PageResponse.from(messages, dtos)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Send message to chat room
     */
    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request) {
        User user = authResolver.resolveUser(principal);
        try {
            ChatMessage message = blogChatService.sendMessage(roomId, user, request.content());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(ChatMessageDto.from(message)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark room messages as read
     */
    @PostMapping("/rooms/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long roomId) {
        User user = authResolver.resolveUser(principal);
        try {
            blogChatService.markAsRead(roomId, user);
            return ResponseEntity.ok(ApiResponse.success("Messages marked as read"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get unread message count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int count = (int) blogChatService.getUnreadCount(user);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Delete chat room
     */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long roomId) {
        User user = authResolver.resolveUser(principal);
        try {
            blogChatService.deleteRoom(roomId, user);
            return ResponseEntity.ok(ApiResponse.deleted());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Block user in chat
     */
    @PostMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User user = authResolver.resolveUser(principal);
        try {
            blogChatService.blockUser(user, userId);
            return ResponseEntity.ok(ApiResponse.success("User blocked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Unblock user
     */
    @DeleteMapping("/block/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User user = authResolver.resolveUser(principal);
        try {
            blogChatService.unblockUser(user, userId);
            return ResponseEntity.ok(ApiResponse.success("User unblocked"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== Inner Records ====================

    public record AIChatRequest(
            @NotBlank String message,
            String context
    ) {}

    public record AIChatResponse(
            String response,
            String userMessage,
            List<ChatHistoryItem> recentHistory,
            List<BookSuggestion> suggestedBooks
    ) {}

    public record ChatHistoryItem(
            String role,
            String content,
            String timestamp
    ) {
        public static ChatHistoryItem from(ChatMessage message) {
            return new ChatHistoryItem(
                    message.getSender() != null ? "user" : "assistant",
                    message.getContent(),
                    message.getCreatedAt().toString()
            );
        }

        public static ChatHistoryItem from(fit.hutech.BuiBaoHan.entities.AIChatSession session) {
            return new ChatHistoryItem(
                    "session",
                    session.getTitle(),
                    session.getCreatedAt() != null ? session.getCreatedAt().toString() : null
            );
        }
    }

    public record ChatRoomSummary(
            Long id,
            UserInfo otherUser,
            String lastMessage,
            String lastMessageTime,
            int unreadCount,
            boolean online
    ) {
        public static ChatRoomSummary from(ChatRoom room, User currentUser) {
            User other = room.getUser1().getId().equals(currentUser.getId()) 
                    ? room.getUser2() 
                    : room.getUser1();
            
            ChatMessage lastMsg = room.getMessages().isEmpty() 
                    ? null 
                    : room.getMessages().get(room.getMessages().size() - 1);
            
            return new ChatRoomSummary(
                    room.getId(),
                    UserInfo.from(other),
                    lastMsg != null ? lastMsg.getContent() : null,
                    lastMsg != null ? lastMsg.getCreatedAt().toString() : null,
                    0, // Will be calculated by service
                    false // Will be determined by WebSocket
            );
        }
    }

    public record ChatRoomDetail(
            Long id,
            UserInfo otherUser,
            String createdAt
    ) {
        public static ChatRoomDetail from(ChatRoom room, User currentUser) {
            User other = room.getUser1().getId().equals(currentUser.getId()) 
                    ? room.getUser2() 
                    : room.getUser1();
            
            return new ChatRoomDetail(
                    room.getId(),
                    UserInfo.from(other),
                    room.getCreatedAt().toString()
            );
        }
    }

    public record ChatMessageDto(
            Long id,
            Long senderId,
            String senderName,
            String senderAvatar,
            String content,
            boolean read,
            String createdAt
    ) {
        public static ChatMessageDto from(ChatMessage message) {
            return new ChatMessageDto(
                    message.getId(),
                    message.getSender() != null ? message.getSender().getId() : null,
                    message.getSender() != null ? message.getSender().getFullName() : "AI Assistant",
                    message.getSender() != null ? message.getSender().getAvatar() : null,
                    message.getContent(),
                    message.getRead(),
                    message.getCreatedAt().toString()
            );
        }
    }

    public record UserInfo(Long id, String username, String fullName, String avatar) {
        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getFullName(), user.getAvatar());
        }
    }

    public record SendMessageRequest(@NotBlank String content) {}

    public record BookSuggestion(
            Long id,
            String title,
            String author,
            String price,
            String coverImage,
            String slug,
            String link,
            boolean inStock,
            boolean canBorrow
    ) {
        public static BookSuggestion from(Book book) {
            java.text.NumberFormat vndFormat = java.text.NumberFormat.getInstance(java.util.Locale.forLanguageTag("vi-VN"));
            String priceStr = book.getPrice() != null ? vndFormat.format(book.getPrice()) + "đ" : "Liên hệ";
            return new BookSuggestion(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor() != null ? book.getAuthor().getName() : "Không rõ",
                    priceStr,
                    book.getCoverImage(),
                    book.getSlug(),
                    book.getSlug() != null ? "/books/" + book.getSlug() : "/books",
                    book.getStockQuantity() != null && book.getStockQuantity() > 0,
                    book.getLibraryStock() != null && book.getLibraryStock() > 0
            );
        }
    }
}
