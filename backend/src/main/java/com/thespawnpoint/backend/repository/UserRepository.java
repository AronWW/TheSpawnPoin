package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByDisplayNameIgnoreCase(String displayName);

    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :excludeId
              AND u.emailVerified = true
              AND u.role <> com.thespawnpoint.backend.entity.user.Role.ADMIN
              AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.displayName ASC
            """)
    List<User> searchByQuery(@Param("q") String q, @Param("excludeId") Long excludeId);

    long countByBannedTrue();

    Page<User> findByBannedTrue(Pageable pageable);

    @Query(value = """
            SELECT * FROM users u
            WHERE (CAST(:q AS text) IS NULL
                OR LOWER(u.display_name) LIKE LOWER('%' || CAST(:q AS text) || '%')
                OR LOWER(u.email) LIKE LOWER('%' || CAST(:q AS text) || '%'))
            ORDER BY u.created_at DESC
            """, nativeQuery = true)
    Page<User> searchForAdmin(@Param("q") String q, Pageable pageable);
}