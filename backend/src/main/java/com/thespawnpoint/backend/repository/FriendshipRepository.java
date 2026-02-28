package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.Friendship;
import com.thespawnpoint.backend.entity.FriendshipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, FriendshipId> {

    List<Friendship> findByUser1Id(Long userId);

    List<Friendship> findByUser2Id(Long userId);
}

