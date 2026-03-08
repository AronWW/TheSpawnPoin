package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.user.ProfileComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileCommentRepository extends JpaRepository<ProfileComment, Long> {

    Page<ProfileComment> findByProfileUserIdOrderByCreatedAtDesc(Long profileUserId, Pageable pageable);

    void deleteByProfileUserIdAndId(Long profileUserId, Long id);
}

