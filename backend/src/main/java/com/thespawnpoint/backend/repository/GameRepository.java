package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.game.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    List<Game> findByGenre(String genre);

    List<Game> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query(value = """
            SELECT * FROM games g
            WHERE (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:genre IS NULL OR g.genre = :genre)
            ORDER BY g.name ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM games g
            WHERE (:q IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:genre IS NULL OR g.genre = :genre)
            """,
            nativeQuery = true)
    Page<Game> searchPaged(
            @Param("q") String q,
            @Param("genre") String genre,
            Pageable pageable
    );
}