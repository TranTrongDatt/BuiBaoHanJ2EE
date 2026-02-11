package fit.hutech.BuiBaoHan.controllers.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.AIChatService;
import fit.hutech.BuiBaoHan.services.BlogChatService;
import fit.hutech.BuiBaoHan.services.UserService;
import lombok.RequiredArgsConstructor;

/**
 * Chat Web Controller
 */
@Controller
@RequestMapping("/chat")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatController {

    private final AIChatService aiChatService;
    private final BlogChatService blogChatService;
    private final UserService userService;
    private final AuthResolver authResolver;

    /**
     * Chat home - list of conversations
     */
    @GetMapping
    public String chatHome(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        model.addAttribute("rooms", blogChatService.getUserRooms(user));
        model.addAttribute("unreadCount", blogChatService.getUnreadCount(user));
        return "chat/index";
    }

    /**
     * AI Chat page
     */
    @GetMapping("/ai")
    public String aiChat(@AuthenticationPrincipal Object principal, Model model) {
        User user = authResolver.resolveUser(principal);
        model.addAttribute("chatHistory", aiChatService.getChatHistory(user, 50));
        model.addAttribute("suggestions", aiChatService.getConversationStarters());
        return "chat/ai";
    }

    /**
     * Chat room page
     */
    @GetMapping("/room/{roomId}")
    public String chatRoom(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long roomId,
            Model model) {
        User user = authResolver.resolveUser(principal);
        
        return blogChatService.getRoomById(roomId, user)
                .map(room -> {
                    // Mark messages as read
                    blogChatService.markAsRead(roomId, user);
                    
                    // Get other user from members (private room has exactly 2 members)
                    User otherUser = room.getMembers().stream()
                            .filter(member -> !member.getId().equals(user.getId()))
                            .findFirst()
                            .orElse(user);
                    
                    model.addAttribute("room", room);
                    model.addAttribute("otherUser", otherUser);
                    model.addAttribute("messages", blogChatService.getRoomMessages(roomId, user, 
                            org.springframework.data.domain.PageRequest.of(0, 100)).getContent());
                    
                    return "chat/room";
                })
                .orElse("redirect:/chat");
    }

    /**
     * Start chat with user
     */
    @GetMapping("/with/{userId}")
    public String startChatWithUser(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User currentUser = authResolver.resolveUser(principal);
        
        try {
            var room = blogChatService.getOrCreateRoom(currentUser, userId);
            return "redirect:/chat/room/" + room.getId();
        } catch (IllegalArgumentException e) {
            return "redirect:/chat";
        }
    }

    /**
     * User profile popup data (for starting chats)
     */
    @GetMapping("/user/{userId}")
    public String userProfileForChat(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId,
            Model model) {
        User currentUser = authResolver.resolveUser(principal);
        
        return userService.findById(userId)
                .map(user -> {
                    model.addAttribute("targetUser", user);
                    model.addAttribute("canChat", !user.getId().equals(currentUser.getId()));
                    return "chat/user-profile";
                })
                .orElse("redirect:/chat");
    }
}
