package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByUser1Id(Long userId);

    List<Friendship> findByUser2Id(Long userId);

    Optional<Friendship> findByUser1IdAndUser2Id(Long userId1, Long userId2);

    boolean existsByUser1IdAndUser2Id(Long userId1, Long userId2);
}
