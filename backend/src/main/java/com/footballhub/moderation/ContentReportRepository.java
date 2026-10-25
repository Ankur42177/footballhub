package com.footballhub.moderation;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    Page<ContentReport> findByStatusIn(Collection<ReportStatus> statuses, Pageable pageable);

    boolean existsByReporterIdAndBlogIdAndStatusIn(Long reporterId, Long blogId, Collection<ReportStatus> statuses);

    boolean existsByReporterIdAndCommentIdAndStatusIn(Long reporterId, Long commentId,
            Collection<ReportStatus> statuses);
}
