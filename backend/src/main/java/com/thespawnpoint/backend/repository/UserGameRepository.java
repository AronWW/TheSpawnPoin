package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.UserGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGameRepository extends JpaRepository<UserGame, Long> {

    List<UserGame> findByUserId(Long userId);

    Optional<UserGame> findByUserIdAndGameId(Long userId, Long gameId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    void deleteByUserIdAndGameId(Long userId, Long gameId);
}
