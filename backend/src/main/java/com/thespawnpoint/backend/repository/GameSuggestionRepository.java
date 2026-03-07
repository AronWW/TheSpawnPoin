package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.game.GameSuggestion;
import com.thespawnpoint.backend.entity.game.SuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameSuggestionRepository extends JpaRepository<GameSuggestion, Long> {

    List<GameSuggestion> findByStatus(SuggestionStatus status);

    Page<GameSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status, Pageable pageable);

    Page<GameSuggestion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<GameSuggestion> findBySuggestedByIdOrderByCreatedAtDesc(Long userId);

    boolean existsByNameIgnoreCaseAndStatus(String name, SuggestionStatus status);

    long countByStatus(SuggestionStatus status);
}

