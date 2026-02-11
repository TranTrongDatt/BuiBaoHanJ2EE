package fit.hutech.BuiBaoHan.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fit.hutech.BuiBaoHan.entities.Follow;
import fit.hutech.BuiBaoHan.entities.User;
import fit.hutech.BuiBaoHan.repositories.IFollowRepository;
import fit.hutech.BuiBaoHan.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service quản lý Follow/Follower
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FollowService {

    private final IFollowRepository followRepository;
    private final IUserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Follow một user
     */
    public Follow followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Không thể follow chính mình");
        }

        if (isFollowing(followerId, followingId)) {
            throw new IllegalStateException("Bạn đã follow user này");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + followerId));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user ID: " + followingId));

        Follow follow = Follow.builder()
                .follower(follower)
                .following(following)
                .build();

        Follow saved = followRepository.save(follow);

        // Gửi thông báo
        notificationService.sendFollowNotification(followingId, follower);

        log.info("User {} followed user {}", followerId, followingId);
        return saved;
    }

    /**
     * Unfollow một user
     */
    public void unfollowUser(Long followerId, Long followingId) {
        Optional<Follow> follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId);
        
        if (follow.isEmpty()) {
            throw new IllegalArgumentException("Bạn chưa follow user này");
        }

        followRepository.delete(follow.get());
        log.info("User {} unfollowed user {}", followerId, followingId);
    }

    /**
     * Toggle follow
     */
    public boolean toggleFollow(Long followerId, Long followingId) {
        if (isFollowing(followerId, followingId)) {
            unfollowUser(followerId, followingId);
            return false;
        } else {
            followUser(followerId, followingId);
            return true;
        }
    }

    /**
     * Kiểm tra có đang follow không
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    /**
     * Lấy danh sách người mình đang follow
     */
    @Transactional(readOnly = true)
    public Page<User> getFollowing(Long userId, Pageable pageable) {
        return followRepository.findFollowing(userId, pageable);
    }

    /**
     * Lấy danh sách followers
     */
    @Transactional(readOnly = true)
    public Page<User> getFollowers(Long userId, Pageable pageable) {
        return followRepository.findFollowers(userId, pageable);
    }

    /**
     * Đếm số người đang follow
     */
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    /**
     * Đếm số followers
     */
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    /**
     * Lấy danh sách mutual follows (follow lẫn nhau)
     */
    @Transactional(readOnly = true)
    public List<User> getMutualFollows(Long userId) {
        return followRepository.findMutualFollows(userId);
    }

    /**
     * Gợi ý người để follow
     */
    @Transactional(readOnly = true)
    public List<User> getSuggestedUsers(Long userId, int limit) {
        return followRepository.findSuggestedUsers(userId, limit);
    }

    /**
     * Lấy danh sách followers chung
     */
    @Transactional(readOnly = true)
    public List<User> getCommonFollowers(Long userId1, Long userId2) {
        return followRepository.findCommonFollowers(userId1, userId2);
    }

    /**
     * Xóa tất cả follow của user (khi xóa tài khoản)
     */
    public void removeAllFollows(Long userId) {
        followRepository.deleteByFollowerId(userId);
        followRepository.deleteByFollowingId(userId);
        log.info("Removed all follows for user {}", userId);
    }

    /**
     * Block một user (unfollow + ngăn follow lại)
     */
    public void blockUser(Long blockerId, Long blockedId) {
        // Unfollow nếu đang follow
        if (isFollowing(blockerId, blockedId)) {
            unfollowUser(blockerId, blockedId);
        }
        // Xóa follow ngược lại
        if (isFollowing(blockedId, blockerId)) {
            followRepository.deleteByFollowerIdAndFollowingId(blockedId, blockerId);
        }
        // TODO: Thêm logic block user
        log.info("User {} blocked user {}", blockerId, blockedId);
    }

    // ==================== Controller Wrapper Methods ====================

    /**
     * Toggle follow (wrapper nhận User)
     */
    public boolean toggleFollow(User user, Long followingId) {
        return toggleFollow(user.getId(), followingId);
    }

    /**
     * Unfollow (wrapper nhận User)
     */
    public void unfollow(User user, Long followingId) {
        unfollowUser(user.getId(), followingId);
    }

    /**
     * Kiểm tra có đang follow không (wrapper nhận User)
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(User user, Long followingId) {
        return isFollowing(user.getId(), followingId);
    }

    /**
     * Lấy số lượng followers
     */
    @Transactional(readOnly = true)
    public int getFollowerCount(Long userId) {
        return (int) countFollowers(userId);
    }

    /**
     * Lấy số lượng following
     */
    @Transactional(readOnly = true)
    public int getFollowingCount(Long userId) {
        return (int) countFollowing(userId);
    }

    /**
     * Lấy danh sách followers (trả về Page<Follow>)
     */
    @Transactional(readOnly = true)
    public Page<Follow> getFollowers(User user, Pageable pageable) {
        return followRepository.findByFollowingIdWithFollower(user.getId(), pageable);
    }

    /**
     * Lấy danh sách đang follow (trả về Page<Follow>)
     */
    @Transactional(readOnly = true)
    public Page<Follow> getFollowing(User user, Pageable pageable) {
        return followRepository.findByFollowerIdWithFollowing(user.getId(), pageable);
    }

    /**
     * Lấy danh sách followers theo userId (trả về Page<Follow>)
     */
    @Transactional(readOnly = true)
    public Page<Follow> getFollowersByUserId(Long userId, Pageable pageable) {
        return followRepository.findByFollowingIdWithFollower(userId, pageable);
    }

    /**
     * Lấy danh sách following theo userId (trả về Page<Follow>)
     */
    @Transactional(readOnly = true)
    public Page<Follow> getFollowingByUserId(Long userId, Pageable pageable) {
        return followRepository.findByFollowerIdWithFollowing(userId, pageable);
    }

    /**
     * Gợi ý người để follow (wrapper nhận User)
     */
    @Transactional(readOnly = true)
    public List<User> getSuggestedUsers(User user) {
        return getSuggestedUsers(user.getId(), 10);
    }
}
