package fit.hutech.BuiBaoHan.services;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import fit.hutech.BuiBaoHan.config.properties.GeminiProperties;
import fit.hutech.BuiBaoHan.constants.MessageType;
import fit.hutech.BuiBaoHan.constants.SenderType;
import fit.hutech.BuiBaoHan.entities.AIChatMessage;
import fit.hutech.BuiBaoHan.entities.AIChatSession;
import fit.hutech.BuiBaoHan.entities.Book;
import fit.hutech.BuiBaoHan.entities.Category;
import fit.hutech.BuiBaoHan.entities.Field;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IAIChatMessageRepository;
import fit.hutech.BuiBaoHan.repositories.IAIChatSessionRepository;
import fit.hutech.BuiBaoHan.repositories.IBookRepository;
import fit.hutech.BuiBaoHan.repositories.IFieldRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý AI Chatbot sử dụng Google Gemini 2.0 Flash
 * Free tier: 15 RPM, 1,500 requests/day, 1M tokens context
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIChatService {

    private final IAIChatSessionRepository sessionRepository;
    private final IAIChatMessageRepository messageRepository;
    private final IUserRepository userRepository;
    private final IBookRepository bookRepository;
    private final IFieldRepository fieldRepository;
    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();
    private static final NumberFormat VND_FORMAT = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private static final String SYSTEM_PROMPT = """
        Bạn là MiniVerse Assistant - trợ lý AI thân thiện của MiniVerse Bookstore!
        
        == VAI TRÒ ==
        Bạn là trợ lý tư vấn sách thông minh, thân thiện, và hữu ích.
        Bạn CHỈ chuyên tư vấn sách và hỗ trợ mượn sách tại MiniVerse Bookstore.
        
        == PHONG CÁCH ==
        - Thân thiện, vui vẻ, dùng emoji phù hợp
        - Xưng "mình" và gọi người dùng là "bạn"
        - Trả lời ngắn gọn, dễ hiểu bằng tiếng Việt
        
        == CÁCH TƯ VẤN ==
        Khi người dùng hỏi về sách:
        1. Hiểu nhu cầu của họ từ tin nhắn
        2. TÌM KIẾM sách PHÙ HỢP trong lĩnh vực, danh mục từ cơ sở dữ liệu. Lấy đúng sản phẩm từ phần "SẢN PHẨM LIÊN QUAN TÌM ĐƯỢC" bên dưới
        3. Giới thiệu sách với format ngắn gọn:
        
         **Tên sách** - Giá: [giá]đ
        
        (KHÔNG cần ghi link, KHÔNG ghi "Xem chi tiết". Hình ảnh và link sản phẩm sẽ được hệ thống tự động hiển thị bên dưới)
        
        == KHI CÓ SẢN PHẨM TÌM ĐƯỢC ==
        Nếu phần "SẢN PHẨM LIÊN QUAN TÌM ĐƯỢC" có chứa sách:
        → BẮT BUỘC phải giới thiệu CÁC SÁCH ĐÓ cho người dùng (có bao nhiêu giới thiệu bấy nhiêu)
        → KHÔNG ĐƯỢC nói "shop không có sản phẩm" khi đã có sách trong dữ liệu
        → KHÔNG thêm sách không có trong dữ liệu vào danh sách
        
        == KHI KHÔNG TÌM THẤY SÁCH ==
        CHỈ KHI phần "SẢN PHẨM LIÊN QUAN TÌM ĐƯỢC" ghi rõ "Không tìm thấy sách phù hợp":
        → Trả lời: "Ôi, shop nhỏ bé của mình chưa có sản phẩm này, bạn thông cảm nha. Bạn muốn tham khảo sản phẩm khác không nè?"
        → TUYỆT ĐỐI KHÔNG bịa ra sách không có trong dữ liệu
        
        == CÂU HỎI NGOÀI PHẠM VI ==
        Nếu người dùng hỏi về chủ đề KHÔNG liên quan đến sách, mượn sách, đơn hàng, hoặc cửa hàng:
        → Trả lời: "Ây da, câu hỏi này hơi ngoài tầm của mình rồi bạn ơi! Mình chỉ chuyên tư vấn sách và hỗ trợ mượn sách thôi nha~ Bạn có muốn mình giúp tìm sách hay gì không?"
        
        == QUY TẮC BẮT BUỘC ==
        - CHỈ gợi ý sách CÓ TRONG dữ liệu được cung cấp, KHÔNG thêm bớt
        - KHÔNG ghi link, KHÔNG ghi "/books/...", KHÔNG ghi "Xem chi tiết"
        - Chỉ nói về sách và dịch vụ của MiniVerse Bookstore
        
        == MƯỢN SÁCH ==
        Mượn tối đa 5 cuốn, 14 ngày, gia hạn 2 lần.
        """;

    /**
     * Lấy hoặc tạo session chat
     */
    public AIChatSession getOrCreateSession(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + userId));

        // Tìm session active
        Optional<AIChatSession> existingSession = sessionRepository.findActiveSessionByUserId(userId);
        
        if (existingSession.isPresent()) {
            return existingSession.get();
        }

        // Tạo session mới
        AIChatSession session = AIChatSession.builder()
                .user(user)
                .title("Cuộc trò chuyện mới")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return sessionRepository.save(session);
    }

    /**
     * Simple chat method - sends message and returns AI response
     * Tự động inject dữ liệu sách từ DB vào prompt
     */
    public String chat(User user, String message, String context) {
        // Get or create session for user
        AIChatSession session = getOrCreateSession(user.getId());
        
        // Build enriched message with book catalog + search results
        String enrichedMessage = buildEnrichedMessage(message, context);
        
        // Send message and get response
        AIChatMessage response = sendMessage(session.getId(), user.getId(), enrichedMessage);
        return response.getContent();
    }

    /**
     * Lấy danh sách sách gợi ý dựa trên message của user
     * Trả về List<Book> để controller trả về kèm response
     * Sử dụng query với JOIN FETCH để tránh LazyInitializationException
     * 
     * Logic (ưu tiên tìm theo danh mục/lĩnh vực): 
     * 1. Tìm Category/Field khớp với message → lấy TẤT CẢ sách trong danh mục đó
     * 2. Nếu không match danh mục nào → fallback keyword search
     * 3. Nếu không có intent cụ thể → bestseller
     */
    @Transactional(readOnly = true)
    public List<Book> getSuggestedBooks(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return List.of();
        
        String lowerMsg = userMessage.toLowerCase();
        
        // === BƯỚC 1: Tìm theo Category/Field trước (chính xác nhất) ===
        List<Book> categoryResults = findBooksByMatchingCategory(lowerMsg);
        if (!categoryResults.isEmpty()) {
            return categoryResults;
        }
        
        // === BƯỚC 2: Fallback keyword search ===
        List<Book> results = new ArrayList<>();
        boolean hasSpecificIntent = hasSpecificBookIntent(lowerMsg);
        
        List<String> keywords = extractSearchKeywords(lowerMsg);
        for (String keyword : keywords) {
            if (keyword.length() >= 2) {
                List<Book> found = bookRepository.searchBookWithDetails(keyword);
                for (Book b : found) {
                    if (results.stream().noneMatch(r -> r.getId().equals(b.getId()))) {
                        results.add(b);
                    }
                }
            }
            if (results.size() >= 8) break;
        }
        
        // === BƯỚC 3: Chỉ fallback bestseller nếu không có intent cụ thể ===
        if (results.isEmpty() && !hasSpecificIntent) {
            results.addAll(bookRepository.findTopSellingWithDetails(PageRequest.of(0, 4)));
        }
        
        return results;
    }
    
    /**
     * Tìm sách bằng cách match Category/Field trong DB
     * Query tất cả Fields → Categories, so khớp tên với keywords trong message
     * Nếu match → lấy TẤT CẢ sách trong category/field đó (không giới hạn 4)
     */
    @Transactional(readOnly = true)
    private List<Book> findBooksByMatchingCategory(String lowerMsg) {
        try {
            List<Field> fields = fieldRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            
            for (Field field : fields) {
                Optional<Field> fieldWithCats = fieldRepository.findByIdWithCategories(field.getId());
                if (fieldWithCats.isEmpty()) continue;
                
                Field f = fieldWithCats.get();
                
                // Check từng Category trong Field
                if (f.getCategories() != null) {
                    for (Category cat : f.getCategories()) {
                        if (cat.getIsActive() != null && cat.getIsActive() && matchesCategoryName(lowerMsg, cat.getName())) {
                            List<Book> books = bookRepository.findByCategoryIdWithDetails(cat.getId());
                            if (!books.isEmpty()) {
                                log.info("AI Chat: Matched category '{}' with {} books", cat.getName(), books.size());
                                return books;
                            }
                        }
                    }
                }
                
                // Check Field name
                if (matchesFieldName(lowerMsg, f.getName())) {
                    List<Book> books = bookRepository.findByFieldIdWithDetails(f.getId());
                    if (!books.isEmpty()) {
                        log.info("AI Chat: Matched field '{}' with {} books", f.getName(), books.size());
                        return books;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding books by category: {}", e.getMessage());
        }
        return List.of();
    }
    
    /**
     * Kiểm tra message có khớp với tên Category không
     * Hỗ trợ so khớp linh hoạt: "IT", "Ẩm Thực", "Nấu ăn", "Truyện tranh", "Giáo khoa"...
     */
    private boolean matchesCategoryName(String lowerMsg, String categoryName) {
        String catLower = categoryName.toLowerCase();
        
        // Exact/partial match với tên category
        if (lowerMsg.contains(catLower)) return true;
        
        // Mapping từ ngững phổ biến → tên category thật trong DB
        Map<String, List<String>> categoryAliases = Map.ofEntries(
            Map.entry("it - công nghệ thông tin", List.of("it", "công nghệ", "công nghệ thông tin", "lập trình", "programming", "cntt")),
            Map.entry("ẩm thực việt nam", List.of("ẩm thực", "nấu ăn", "món ăn", "cooking", "đầu bếp")),
            Map.entry("truyện tranh có hình", List.of("truyện tranh", "manga", "comic", "doraemon", "conan")),
            Map.entry("văn học việt nam", List.of("văn học", "tiểu thuyết", "truyện ngắn")),
            Map.entry("cấp 1", List.of("cấp 1", "tiểu học", "lớp 1", "lớp 2", "lớp 3", "lớp 4", "lớp 5")),
            Map.entry("cấp 2", List.of("cấp 2", "trung học cơ sở", "thcs", "lớp 6", "lớp 7", "lớp 8", "lớp 9")),
            Map.entry("cấp 3", List.of("cấp 3", "trung học phổ thông", "thpt", "lớp 10", "lớp 11", "lớp 12"))
        );
        
        for (Map.Entry<String, List<String>> entry : categoryAliases.entrySet()) {
            if (catLower.contains(entry.getKey())) {
                for (String alias : entry.getValue()) {
                    if (lowerMsg.contains(alias)) return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra message có khớp với tên Field (lĩnh vực) không
     */
    private boolean matchesFieldName(String lowerMsg, String fieldName) {
        String fieldLower = fieldName.toLowerCase();
        
        if (lowerMsg.contains(fieldLower)) return true;
        
        Map<String, List<String>> fieldAliases = Map.of(
            "sách giáo khoa", List.of("giáo khoa", "sách giáo khoa", "sgk"),
            "truyện tranh", List.of("truyện tranh", "manga", "comic"),
            "tiểu thuyết", List.of("tiểu thuyết", "văn học", "novel"),
            "sách dạy nấu ăn", List.of("nấu ăn", "ẩm thực", "cooking", "món ăn")
        );
        
        for (Map.Entry<String, List<String>> entry : fieldAliases.entrySet()) {
            if (fieldLower.contains(entry.getKey())) {
                for (String alias : entry.getValue()) {
                    if (lowerMsg.contains(alias)) return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Kiểm tra user có intent tìm sách cụ thể không
     * Dùng để quyết định có fallback về bestseller hay không
     */
    private boolean hasSpecificBookIntent(String lowerMsg) {
        // Các từ khóa chỉ category/field cụ thể
        String[] specificKeywords = {
            // IT/Công nghệ Thông tin
            "it", "lập trình", "programming", "code", "java", "python", "web", "phần mềm", "công nghệ",
            "công nghệ thông tin", "software", "algorithm",
            "an toàn", "bảo mật", "security", "database", "ai", "machine learning",
            // Sách giáo khoa
            "giáo khoa", "sách giáo khoa", "cấp 1", "cấp 2", "cấp 3", "tiểu học", "trung học", "đại học", "toán", "lý", "hóa", "văn", "tiếng anh", "tiếng việt",
            // Truyện tranh
            "truyện tranh", "manga", "comic", "doraemon", "conan", "one piece", "naruto", "dragon ball",
            // Tiểu thuyết/Văn học
            "tiểu thuyết", "văn học", "trinh thám", "kinh dị", "ngôn tình", "light novel",
            // Ẩm thực Việt Nam
            "nấu ăn", "ẩm thực", "ẩm thực việt nam", "món ăn", "cooking", "recipe", "đầu bếp",
            "phở", "bánh mì", "bún", "chay", "món việt",
            // Khác
            "kinh tế", "kinh doanh", "self-help", "tâm lý", "phát triển bản thân", "lịch sử", "khoa học",
            "thiếu nhi", "trẻ em", "cổ tích"
        };
        
        for (String kw : specificKeywords) {
            if (lowerMsg.contains(kw)) return true;
        }
        return false;
    }

    /**
     * Lấy session theo ID
     */
    @Transactional(readOnly = true)
    public Optional<AIChatSession> getSessionById(Long sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * Lấy lịch sử sessions của user
     */
    @Transactional(readOnly = true)
    public List<AIChatSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * Lấy lịch sử chat của user (wrapper cho User object)
     */
    @Transactional(readOnly = true)
    public List<AIChatSession> getChatHistory(User user, int limit) {
        List<AIChatSession> sessions = getUserSessions(user.getId());
        return sessions.stream().limit(limit).toList();
    }

    /**
     * Xóa lịch sử chat của user
     */
    public void clearHistory(User user) {
        List<AIChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
        for (AIChatSession session : sessions) {
            messageRepository.deleteBySessionId(session.getId());
            sessionRepository.delete(session);
        }
        log.info("Cleared chat history for user {}", user.getId());
    }

    /**
     * Lấy gợi ý sách từ AI (dựa trên dữ liệu DB thực)
     * Sử dụng query với JOIN FETCH để tránh LazyInitializationException
     */
    public List<String> getBookSuggestions(User user, String category, String preference) {
        getOrCreateSession(user.getId());
        try {
            List<Book> books;
            if (category != null && !category.isEmpty()) {
                // Use searchBookWithDetails with JOIN FETCH
                books = bookRepository.searchBookWithDetails(category).stream().limit(5).toList();
            } else {
                // Use findTopSellingWithDetails with JOIN FETCH
                books = bookRepository.findTopSellingWithDetails(PageRequest.of(0, 5));
            }
            return books.stream()
                    .map(b -> b.getTitle() + (b.getAuthor() != null ? " - " + b.getAuthor().getName() : ""))
                    .toList();
        } catch (Exception e) {
            log.error("Error getting book suggestions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Lấy gợi ý câu hỏi bắt đầu cuộc trò chuyện
     */
    @Transactional(readOnly = true)
    public List<String> getConversationStarters() {
        return List.of(
            "Gợi ý sách IT - Công Nghệ Thông Tin ",
            "Gợi ý sách Ẩm Thực Việt Nam ",
            "Gợi ý sách giáo khoa ",
            "Gợi ý sách Truyện tranh "
        );
    }

    /**
     * Lấy messages của session
     */
    @Transactional(readOnly = true)
    public List<AIChatMessage> getSessionMessages(Long sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * Gửi message và nhận phản hồi AI
     */
    public AIChatMessage sendMessage(Long sessionId, Long userId, String userMessage) {
        AIChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session ID: " + sessionId));

        // Kiểm tra quyền
        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền truy cập session này");
        }

        // Lưu message của user
        AIChatMessage userMsg = AIChatMessage.builder()
                .session(session)
                .role("user")
                .senderType(SenderType.USER)
                .content(userMessage)
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();
        messageRepository.save(userMsg);

        // Lấy context (10 messages gần nhất)
        List<AIChatMessage> history = messageRepository.findRecentBySessionId(sessionId, 10);

        // Gọi AI API với dynamic system prompt chứa dữ liệu sách
        String dynamicPrompt = buildDynamicSystemPrompt(userMessage);
        String aiResponse = callAIApi(history, userMessage, dynamicPrompt);

        // Lưu response của AI
        AIChatMessage aiMsg = AIChatMessage.builder()
                .session(session)
                .role("assistant")
                .senderType(SenderType.AI)
                .content(aiResponse)
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.now())
                .build();
        AIChatMessage saved = messageRepository.save(aiMsg);

        // Cập nhật session
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getTitle().equals("Cuộc trò chuyện mới")) {
            session.setTitle(generateSessionTitle(userMessage));
        }
        sessionRepository.save(session);

        log.info("AI chat: session {}, user message length: {}", sessionId, userMessage.length());
        return saved;
    }

    /**
     * Đóng session
     */
    public void closeSession(Long sessionId, Long userId) {
        AIChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session ID: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền đóng session này");
        }

        session.setIsActive(false);
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("Closed AI chat session: {}", sessionId);
    }

    /**
     * Xóa session
     */
    public void deleteSession(Long sessionId, Long userId) {
        AIChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy session ID: " + sessionId));

        if (!session.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền xóa session này");
        }

        // Xóa messages trước
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.delete(session);
        log.info("Deleted AI chat session: {}", sessionId);
    }

    /**
     * Feedback cho message (helpful/not helpful)
     */
    public void submitFeedback(Long messageId, boolean isHelpful, String feedback) {
        AIChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy message ID: " + messageId));

        message.setIsHelpful(isHelpful);
        message.setFeedback(feedback);
        messageRepository.save(message);
        log.info("Feedback submitted for message {}: helpful={}", messageId, isHelpful);
    }

    // ==================== Private Helper Methods ====================

    // ==================== DB Context Builder Methods ====================

    /**
     * Build enriched message kèm context từ DB
     */
    private String buildEnrichedMessage(String message, String context) {
        StringBuilder enriched = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            enriched.append("Context: ").append(context).append("\n\n");
        }
        enriched.append(message);
        return enriched.toString();
    }

    /**
     * Build dynamic system prompt chứa dữ liệu sản phẩm thực từ DB
     */
    private String buildDynamicSystemPrompt(String userMessage) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT);
        
        // Thêm catalog tổng quan
        prompt.append("\n\n== DỮ LIỆU DANH MỤC CỬA HÀNG ==\n");
        prompt.append(buildCatalogContext());
        
        // Nếu user hỏi về sách, search và inject kết quả
        String lowerMsg = userMessage.toLowerCase();
        if (containsBookIntent(lowerMsg)) {
            prompt.append("\n\n== SẢN PHẨM LIÊN QUAN TÌM ĐƯỢC ==\n");
            prompt.append(buildBookSearchContext(userMessage));
        }
        
        return prompt.toString();
    }

    /**
     * Kiểm tra xem message có liên quan đến sách/sản phẩm
     */
    private boolean containsBookIntent(String lowerMsg) {
        String[] bookKeywords = {"sách", "book", "tìm", "gợi ý", "mua", "đọc", "mượn",
                "recommend", "giá", "tác giả", "thể loại", "danh mục", "truyện",
                "tiểu thuyết", "giáo khoa", "tham khảo", "self-help", "kinh tế",
                "lập trình", "tiếng anh", "văn học", "khoa học", "lịch sử",
                "trinh thám", "manga", "comic", "bestseller", "bán chạy",
                "it", "công nghệ", "công nghệ thông tin", "ẩm thực", "nấu ăn",
                "truyện tranh", "giáo khoa", "naruto", "doraemon", "conan"};
        for (String keyword : bookKeywords) {
            if (lowerMsg.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Build tổng quan catalog: Fields → Categories
     */
    @Transactional(readOnly = true)
    private String buildCatalogContext() {
        StringBuilder sb = new StringBuilder();
        try {
            List<Field> fields = fieldRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            if (fields.isEmpty()) {
                sb.append("(Chưa có dữ liệu lĩnh vực)\n");
                return sb.toString();
            }
            for (Field field : fields) {
                Optional<Field> fieldWithCats = fieldRepository.findByIdWithCategories(field.getId());
                if (fieldWithCats.isPresent()) {
                    Field f = fieldWithCats.get();
                    sb.append("\n📚 Lĩnh vực: ").append(f.getName());
                    if (f.getDescription() != null) {
                        sb.append(" - ").append(f.getDescription());
                    }
                    sb.append("\n");
                    if (f.getCategories() != null) {
                        for (Category cat : f.getCategories()) {
                            if (cat.getIsActive() != null && cat.getIsActive()) {
                                long bookCount = bookRepository.countByCategoryId(cat.getId());
                                sb.append("   • Danh mục: ").append(cat.getName())
                                  .append(" (").append(bookCount).append(" sách)\n");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error building catalog context: {}", e.getMessage());
            sb.append("(Lỗi khi tải danh mục)\n");
        }
        return sb.toString();
    }

    /**
     * Search sách từ DB dựa trên message user, trả về context text cho AI
     */
    @Transactional(readOnly = true)
    private String buildBookSearchContext(String userMessage) {
        StringBuilder sb = new StringBuilder();
        try {
            List<Book> books = getSuggestedBooks(userMessage);
            if (books.isEmpty()) {
                sb.append("Không tìm thấy sách phù hợp trong kho.\n");
                return sb.toString();
            }
            sb.append("Tìm thấy ").append(books.size()).append(" sách phù hợp:\n");
            for (int i = 0; i < books.size(); i++) {
                Book b = books.get(i);
                sb.append("\n--- Sách ").append(i + 1).append(" ---\n");
                sb.append("Tên: ").append(b.getTitle()).append("\n");
                if (b.getCategory() != null) {
                    sb.append("Danh mục: ").append(b.getCategory().getName()).append("\n");
                }
                if (b.getPrice() != null) {
                    sb.append("Giá: ").append(formatPrice(b.getPrice())).append("\n");
                }
            }
            sb.append("\n(Đây là TẤT CẢ sách có trong danh mục này. CHỈ giới thiệu đúng các sách này, KHÔNG thêm bớt.)\n");
        } catch (Exception e) {
            log.error("Error building book search context: {}", e.getMessage());
            sb.append("(Lỗi khi tìm sách)\n");
        }
        return sb.toString();
    }

    /**
     * Mapping synonyms/aliases cho các từ khóa phổ biến
     * Khi user nói "IT" thì expand thành các từ liên quan trong DB
     */
    private static final Map<String, List<String>> KEYWORD_SYNONYMS = Map.ofEntries(
        // IT/Công nghệ Thông tin
        Map.entry("it", List.of("lập trình", "programming", "công nghệ", "công nghệ thông tin", "java", "python", "web", "phần mềm", "code", "developer", "software", "algorithm")),
        Map.entry("công nghệ thông tin", List.of("it", "lập trình", "programming", "công nghệ", "phần mềm", "web", "java", "python", "software")),
        Map.entry("công nghệ", List.of("lập trình", "it", "công nghệ thông tin", "phần mềm", "web", "java", "software")),
        Map.entry("lập trình", List.of("programming", "code", "java", "python", "web", "phần mềm", "it", "công nghệ thông tin")),
        Map.entry("programming", List.of("lập trình", "code", "java", "python", "web", "it")),
        
        // Truyện tranh
        Map.entry("truyện tranh", List.of("manga", "comic", "doraemon", "conan", "one piece", "naruto", "dragon ball")),
        Map.entry("manga", List.of("truyện tranh", "comic", "doraemon", "conan", "naruto")),
        Map.entry("comic", List.of("truyện tranh", "manga")),
        
        // Sách giáo khoa
        Map.entry("giáo khoa", List.of("sách giáo khoa", "cấp 1", "cấp 2", "cấp 3", "tiểu học", "trung học", "đại học", "toán", "văn", "tiếng việt", "giáo trình")),
        Map.entry("sách giáo khoa", List.of("giáo khoa", "cấp 1", "cấp 2", "cấp 3", "tiểu học", "trung học", "toán", "văn")),
        Map.entry("học", List.of("giáo khoa", "giáo trình", "bài tập", "ôn thi", "sách giáo khoa")),
        
        // Ẩm thực Việt Nam
        Map.entry("ẩm thực", List.of("nấu ăn", "món ăn", "đầu bếp", "ẩm thực việt nam", "công thức", "cooking", "recipe", "phở", "bánh mì", "bún", "chay")),
        Map.entry("ẩm thực việt nam", List.of("nấu ăn", "ẩm thực", "món ăn", "đầu bếp", "phở", "bánh mì", "bún", "chay", "cooking", "recipe", "món việt")),
        Map.entry("nấu ăn", List.of("ẩm thực", "ẩm thực việt nam", "món ăn", "công thức", "cooking", "recipe", "đầu bếp")),
        
        // Tiểu thuyết/Văn học
        Map.entry("tiểu thuyết", List.of("văn học", "truyện", "novel", "fiction")),
        Map.entry("văn học", List.of("tiểu thuyết", "truyện ngắn", "thơ", "tác phẩm")),
        
        // Thiếu nhi
        Map.entry("thiếu nhi", List.of("trẻ em", "truyện tranh", "cổ tích", "tuổi thơ")),
        Map.entry("trẻ em", List.of("thiếu nhi", "truyện tranh", "cổ tích"))
    );

    /**
     * Trích xuất keywords từ message user để search DB
     * Có hỗ trợ synonym expansion
     */
    private List<String> extractSearchKeywords(String lowerMsg) {
        // Loại bỏ các từ phổ biến không mang nghĩa search
        String[] stopWords = {"tôi", "muốn", "tìm", "cho", "mình", "cần", "hãy", "gợi", "ý",
                "được", "không", "có", "một", "những", "các", "và", "hoặc", "hay", "về",
                "của", "với", "này", "kia", "đó", "để", "nào", "bạn", "sách", "cuốn",
                "giúp", "xem", "mua", "đọc", "book", "recommend", "suggest", "tư", "vấn"};
        
        String cleaned = lowerMsg;
        for (String sw : stopWords) {
            cleaned = cleaned.replaceAll("\\b" + sw + "\\b", " ");
        }
        
        // Split thành các cụm từ có nghĩa
        List<String> keywords = new ArrayList<>();
        String[] parts = cleaned.trim().split("\\s+");
        
        // Thử ghép 2-3 từ liên tiếp
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() >= 2) {
                // Cụm 2 từ
                if (i + 1 < parts.length && parts[i + 1].length() >= 2) {
                    keywords.add(parts[i] + " " + parts[i + 1]);
                }
                // Từ đơn
                keywords.add(parts[i]);
            }
        }
        
        // Thêm cả message gốc (đã clean) nếu ngắn
        if (cleaned.trim().length() >= 3 && cleaned.trim().length() <= 50) {
            keywords.add(0, cleaned.trim());
        }
        
        // Expand synonyms cho các keywords tìm được
        List<String> expandedKeywords = new ArrayList<>(keywords);
        for (String kw : keywords) {
            String kwLower = kw.toLowerCase().trim();
            if (KEYWORD_SYNONYMS.containsKey(kwLower)) {
                expandedKeywords.addAll(KEYWORD_SYNONYMS.get(kwLower));
            }
        }
        
        return expandedKeywords.stream().distinct().limit(10).toList();
    }

    /**
     * Format giá VND
     */
    private String formatPrice(BigDecimal price) {
        return VND_FORMAT.format(price) + "đ";
    }

    // ==================== AI API Methods ====================

    /**
     * Gọi Google Gemini API để lấy response (với dynamic prompt)
     */
    private String callAIApi(List<AIChatMessage> history, String userMessage, String dynamicPrompt) {
        // Kiểm tra API key đã được cấu hình chưa
        if (!geminiProperties.isConfigured()) {
            log.warn("Gemini API not configured, using fallback response");
            return generateFallbackResponse(userMessage);
        }

        try {
            String apiUrl = geminiProperties.getGenerateContentUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Build Gemini request body với dynamic prompt
            String requestBody = buildGeminiRequest(history, userMessage, dynamicPrompt);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String response = restTemplate.postForObject(apiUrl, entity, String.class);

            // Parse Gemini response
            String aiResponse = parseGeminiResponse(response);
            if (aiResponse != null) {
                return aiResponse;
            }

            return generateFallbackResponse(userMessage);

        } catch (RestClientException e) {
            log.error("Error calling Gemini API: {}", e.getMessage());
            return generateFallbackResponse(userMessage);
        } catch (Exception e) {
            log.error("Unexpected error calling Gemini API: {}", e.getMessage());
            return generateFallbackResponse(userMessage);
        }
    }

    /**
     * Build Gemini API request body
     * Format: https://ai.google.dev/api/generate-content
     */
    private String buildGeminiRequest(List<AIChatMessage> history, String userMessage, String systemPrompt) {
        StringBuilder contentsJson = new StringBuilder();
        
        // Add conversation history
        for (AIChatMessage msg : history) {
            if (contentsJson.length() > 0) {
                contentsJson.append(",");
            }
            // Gemini uses "model" instead of "assistant"
            String role = "assistant".equals(msg.getRole()) ? "model" : msg.getRole();
            contentsJson.append(buildGeminiContentJson(role, msg.getContent()));
        }
        
        // Add current user message
        if (contentsJson.length() > 0) {
            contentsJson.append(",");
        }
        contentsJson.append(buildGeminiContentJson("user", userMessage));

        // Build full request with system instruction and generation config
        return String.format("""
            {
                "contents": [%s],
                "systemInstruction": {
                    "parts": [{"text": "%s"}]
                },
                "generationConfig": {
                    "maxOutputTokens": %d,
                    "temperature": %s,
                    "topP": %s,
                    "topK": %d
                }
            }
            """,
            contentsJson.toString(),
            escapeJson(systemPrompt),
            geminiProperties.getMaxOutputTokens(),
            geminiProperties.getTemperature(),
            geminiProperties.getTopP(),
            geminiProperties.getTopK()
        );
    }

    /**
     * Build single content object for Gemini
     */
    private String buildGeminiContentJson(String role, String text) {
        return String.format("""
            {"role": "%s", "parts": [{"text": "%s"}]}
            """, role, escapeJson(text));
    }

    /**
     * Parse Gemini API response
     * Response format: {"candidates": [{"content": {"parts": [{"text": "..."}]}}]}
     */
    @SuppressWarnings("unchecked")
    private String parseGeminiResponse(String response) {
        if (response == null) return null;
        
        try {
            Map<String, Object> responseMap = jsonParser.parseMap(response);
            
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                log.warn("Gemini response has no candidates");
                return null;
            }
            
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            if (content == null) {
                log.warn("Gemini response has no content");
                return null;
            }
            
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                log.warn("Gemini response has no parts");
                return null;
            }
            
            String text = (String) parts.get(0).get("text");
            log.debug("Gemini response parsed successfully, length: {}", text != null ? text.length() : 0);
            return text;
            
        } catch (ClassCastException | NullPointerException | IndexOutOfBoundsException e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Escape special characters for JSON string
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String generateFallbackResponse(String userMessage) {
        String lowerMsg = userMessage.toLowerCase();

        // Chào hỏi
        if (lowerMsg.contains("xin chào") || lowerMsg.contains("hello") || lowerMsg.contains("hi") || lowerMsg.contains("hey")) {
            return "Hii bạn ơi! Mình là MiniVerse Assistant nè~ " +
                    "Mình chuyên tư vấn sách và hỗ trợ mượn sách thôi nha!" +
                    "Bạn muốn mình giúp gì nè?";
        }

        // Mượn sách
        if (lowerMsg.contains("mượn") || lowerMsg.contains("thư viện") || lowerMsg.contains("library")) {
            return "Bạn muốn mượn sách hả? Cool vậy! \n" +
                    "Để mượn sách online, bạn cần:\n" +
                    "1. Đăng ký thẻ thư viện MiniVerse\n" +
                    "2. Tìm sách muốn mượn và bấm 'Mượn sách'\n" +
                    "3. Mượn tối đa 5 cuốn, thời hạn 14 ngày, gia hạn 2 lần nha!\n" +
                    "Cần mình tìm sách gì để mượn không? ";
        }

        // Đơn hàng
        if (lowerMsg.contains("đơn hàng") || lowerMsg.contains("order") || lowerMsg.contains("giao hàng")) {
            return "Bạn ơi, để check đơn hàng thì vào mục 'Đơn hàng của tôi' " +
                    "trong trang cá nhân nha! Nếu cần mình tư vấn thêm sách gì thì cứ hỏi nha~";
        }

        // Liên quan đến sách → TÌM trong DB trước khi trả lời
        if (containsBookIntent(lowerMsg)) {
            try {
                List<Book> books = getSuggestedBooks(userMessage);
                if (!books.isEmpty()) {
                    // Có sách trong DB → giới thiệu sách thật (chỉ tên + giá, không cần link)
                    StringBuilder sb = new StringBuilder();
                    sb.append("Mình tìm được mấy cuốn sách hay cho bạn nè!\n\n");
                    for (Book b : books) {
                        sb.append("📖 **").append(b.getTitle()).append("**");
                        if (b.getPrice() != null) {
                            sb.append(" - Giá: ").append(formatPrice(b.getPrice()));
                        }
                        sb.append("\n");
                    }
                    sb.append("\nBạn xem sản phẩm bên dưới nha! ");
                    return sb.toString();
                } else {
                    return "Ôi, shop nhỏ bé của mình chưa có sản phẩm này, bạn thông cảm nha " +
                            "Bạn muốn tham khảo sản phẩm khác không nè?";
                }
            } catch (Exception e) {
                log.error("Error in fallback book search: {}", e.getMessage());
                return "Oa bạn ơi, mình đang gặp chút trục trặc khi tìm sách nè! " +
                        "Bạn thử hỏi lại sau chút nha~";
            }
        }

        // Ngoài phạm vi
        return "Ây da, câu hỏi này hơi ngoài tầm của mình rồi bạn ơi! 😅 " +
                "Mình chỉ chuyên tư vấn sách và hỗ trợ mượn sách thôi nha~ " +
                "Bạn có muốn mình giúp tìm sách hay gì không? 📚";
    }

    private String generateSessionTitle(String firstMessage) {
        if (firstMessage.length() <= 30) {
            return firstMessage;
        }
        return firstMessage.substring(0, 30) + "...";
    }
}
