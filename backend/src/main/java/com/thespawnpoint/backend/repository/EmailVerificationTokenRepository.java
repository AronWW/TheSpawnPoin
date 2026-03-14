package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.auth.EmailVerificationToken;
import com.thespawnpoint.backend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByCode(String code);
    Optional<EmailVerificationToken> findByUser(User user);
    void deleteByUser(User user);
}
