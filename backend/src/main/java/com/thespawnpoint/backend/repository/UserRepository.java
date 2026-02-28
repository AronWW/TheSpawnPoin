package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.user.User;
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

    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :excludeId
              AND u.emailVerified = true
              AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY u.displayName ASC
            """)
    List<User> searchByQuery(@Param("q") String q, @Param("excludeId") Long excludeId);
}
