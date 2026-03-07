package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.report.ReportStatus;
import com.thespawnpoint.backend.entity.report.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {

    Page<UserReport> findByStatusOrderByCreatedAtDesc(ReportStatus status, Pageable pageable);

    Page<UserReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReportStatus status);
}

