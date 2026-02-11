package fit.hutech.BuiBaoHan.controllers.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fit.hutech.BuiBaoHan.dto.ApiResponse;
import fit.hutech.BuiBaoHan.dto.PageResponse;
import fit.hutech.BuiBaoHan.entities.Follow;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.security.AuthResolver;
import fit.hutech.BuiBaoHan.services.FollowService;
import lombok.RequiredArgsConstructor;

/**
 * REST API Controller for User Following
 */
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiFollowController {

    private final FollowService followService;
    private final AuthResolver authResolver;

    /**
     * Follow a user
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<FollowResult>> followUser(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User currentUser = authResolver.resolveUser(principal);
        try {
            boolean following = followService.toggleFollow(currentUser, userId);
            int followerCount = followService.getFollowerCount(userId);
            return ResponseEntity.ok(ApiResponse.success(new FollowResult(following, followerCount)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Unfollow a user
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<FollowResult>> unfollowUser(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User currentUser = authResolver.resolveUser(principal);
        try {
            followService.unfollow(currentUser, userId);
            int followerCount = followService.getFollowerCount(userId);
            return ResponseEntity.ok(ApiResponse.success(new FollowResult(false, followerCount)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if following a user
     */
    @GetMapping("/{userId}/check")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long userId) {
        User currentUser = authResolver.resolveUser(principal);
        boolean following = followService.isFollowing(currentUser, userId);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    /**
     * Get my followers
     */
    @GetMapping("/followers")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getMyFollowers(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = authResolver.resolveUser(principal);
        
        Page<Follow> follows = followService.getFollowers(user, pageable);
        List<UserSummary> dtos = follows.getContent().stream()
                .map(f -> UserSummary.from(f.getFollower()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(follows, dtos)));
    }

    /**
     * Get users I'm following
     */
    @GetMapping("/following")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getMyFollowing(
            @AuthenticationPrincipal Object principal,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = authResolver.resolveUser(principal);
        
        Page<Follow> follows = followService.getFollowing(user, pageable);
        List<UserSummary> dtos = follows.getContent().stream()
                .map(f -> UserSummary.from(f.getFollowing()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(follows, dtos)));
    }

    /**
     * Get user's followers
     */
    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getUserFollowers(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Follow> follows = followService.getFollowersByUserId(userId, pageable);
        List<UserSummary> dtos = follows.getContent().stream()
                .map(f -> UserSummary.from(f.getFollower()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(follows, dtos)));
    }

    /**
     * Get users the user is following
     */
    @GetMapping("/users/{userId}/following")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getUserFollowing(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<Follow> follows = followService.getFollowingByUserId(userId, pageable);
        List<UserSummary> dtos = follows.getContent().stream()
                .map(f -> UserSummary.from(f.getFollowing()))
                .toList();
        
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(follows, dtos)));
    }

    /**
     * Get follow stats for user
     */
    @GetMapping("/users/{userId}/stats")
    public ResponseEntity<ApiResponse<FollowStats>> getUserFollowStats(@PathVariable Long userId) {
        int followers = followService.getFollowerCount(userId);
        int following = followService.getFollowingCount(userId);
        return ResponseEntity.ok(ApiResponse.success(new FollowStats(followers, following)));
    }

    /**
     * Get my follow stats
     */
    @GetMapping("/my-stats")
    public ResponseEntity<ApiResponse<FollowStats>> getMyFollowStats(@AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        int followers = followService.getFollowerCount(user.getId());
        int following = followService.getFollowingCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success(new FollowStats(followers, following)));
    }

    /**
     * Get suggested users to follow
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<UserSummary>>> getSuggestions(
            @AuthenticationPrincipal Object principal) {
        User user = authResolver.resolveUser(principal);
        List<UserSummary> suggestions = followService.getSuggestedUsers(user).stream()
                .map(UserSummary::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    // ==================== Inner Records ====================

    public record FollowResult(boolean following, int followerCount) {}

    public record FollowStats(int followers, int following) {}

    public record UserSummary(
            Long id,
            String username,
            String fullName,
            String avatar,
            String bio
    ) {
        public static UserSummary from(User user) {
            return new UserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getAvatar(),
                    user.getBio()
            );
        }
    }
}
