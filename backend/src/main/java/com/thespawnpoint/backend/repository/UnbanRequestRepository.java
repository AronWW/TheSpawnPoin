package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.unban.UnbanRequest;
import com.thespawnpoint.backend.entity.unban.UnbanRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnbanRequestRepository extends JpaRepository<UnbanRequest, Long> {

    List<UnbanRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UnbanRequest> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndStatus(Long userId, UnbanRequestStatus status);

    Page<UnbanRequest> findByStatusOrderByCreatedAtDesc(UnbanRequestStatus status, Pageable pageable);

    Page<UnbanRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(UnbanRequestStatus status);

    long countByUserId(Long userId);
}

