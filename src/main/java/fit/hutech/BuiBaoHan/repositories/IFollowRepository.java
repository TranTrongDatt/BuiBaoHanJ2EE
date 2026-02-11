package fit.hutech.BuiBaoHan.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fit.hutech.BuiBaoHan.entities.Follow;
import fit.hutech.BuiBaoHan.entities.User;

/**
 * Repository cho Follow entity
 */
@Repository
public interface IFollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    // Lấy danh sách người mà user đang follow
    @Query("SELECT f FROM Follow f WHERE f.follower.id = :userId")
    List<Follow> findByFollowerId(@Param("userId") Long userId);

    @Query("SELECT f FROM Follow f LEFT JOIN FETCH f.following WHERE f.follower.id = :userId")
    Page<Follow> findByFollowerIdWithFollowing(@Param("userId") Long userId, Pageable pageable);

    // Lấy danh sách người đang follow user
    @Query("SELECT f FROM Follow f WHERE f.following.id = :userId")
    List<Follow> findByFollowingId(@Param("userId") Long userId);

    @Query("SELECT f FROM Follow f LEFT JOIN FETCH f.follower WHERE f.following.id = :userId")
    Page<Follow> findByFollowingIdWithFollower(@Param("userId") Long userId, Pageable pageable);

    // Count
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    long countFollowing(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    long countFollowers(@Param("userId") Long userId);

    // Alias methods for service compatibility
    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    long countByFollowerId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.following.id = :userId")
    long countByFollowingId(@Param("userId") Long userId);

    // Find following users with pagination
    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :userId")
    Page<User> findFollowing(@Param("userId") Long userId, Pageable pageable);

    // Find followers with pagination
    @Query("SELECT f.follower FROM Follow f WHERE f.following.id = :userId")
    Page<User> findFollowers(@Param("userId") Long userId, Pageable pageable);

    // Find mutual follows (users who follow each other)
    @Query("SELECT f1.following FROM Follow f1 " +
           "WHERE f1.follower.id = :userId " +
           "AND EXISTS (SELECT f2 FROM Follow f2 WHERE f2.follower.id = f1.following.id AND f2.following.id = :userId)")
    List<User> findMutualFollows(@Param("userId") Long userId);

    // Find suggested users to follow
    @Query(value = "SELECT u FROM User u " +
           "WHERE u.id != :userId " +
           "AND NOT EXISTS (SELECT f FROM Follow f WHERE f.follower.id = :userId AND f.following.id = u.id) " +
           "ORDER BY u.createdAt DESC")
    List<User> findSuggestedUsers(@Param("userId") Long userId, @Param("limit") int limit);

    // Find common followers between two users
    @Query("SELECT f1.follower FROM Follow f1, Follow f2 " +
           "WHERE f1.following.id = :userId1 AND f2.following.id = :userId2 " +
           "AND f1.follower.id = f2.follower.id")
    List<User> findCommonFollowers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    // Delete all follows where user is follower
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :userId")
    void deleteByFollowerId(@Param("userId") Long userId);

    // Delete all follows where user is being followed
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.following.id = :userId")
    void deleteByFollowingId(@Param("userId") Long userId);
}
