package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.UserGame;
import com.thespawnpoint.backend.entity.UserGameId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserGameRepository extends JpaRepository<UserGame, UserGameId> {

    List<UserGame> findByIdUserId(Long userId);

    List<UserGame> findByIdGameId(Long gameId);
}

