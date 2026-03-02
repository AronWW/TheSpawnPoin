package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUser1Id(Long userId);

    List<Friendship> findByUser2Id(Long userId);

    Optional<Friendship> findByUser1IdAndUser2Id(Long userId1, Long userId2);

    boolean existsByUser1IdAndUser2Id(Long userId1, Long userId2);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.user1.id = :userId OR f.user2.id = :userId
            ORDER BY f.friendsSince DESC
            """)
    List<Friendship> findAllByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
            FROM Friendship f
            WHERE (f.user1.id = :id1 AND f.user2.id = :id2)
               OR (f.user1.id = :id2 AND f.user2.id = :id1)
            """)
    boolean areFriends(@Param("id1") Long id1, @Param("id2") Long id2);

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.user1.id = :id1 AND f.user2.id = :id2)
               OR (f.user1.id = :id2 AND f.user2.id = :id1)
            """)
    Optional<Friendship> findBetween(@Param("id1") Long id1, @Param("id2") Long id2);
}
