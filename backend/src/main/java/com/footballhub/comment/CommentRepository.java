package com.footballhub.comment;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Top-level comments that are visible, plus removed/hidden ones that still have visible
     * replies (shown as a placeholder so the thread keeps its context).
     */
    @Query(value = """
            select c from Comment c
            where c.blog.id = :blogId and c.parentId is null
              and (c.status = :visible
                   or exists (select r.id from Comment r where r.parentId = c.id and r.status = :visible))
            """, countQuery = """
            select count(c) from Comment c
            where c.blog.id = :blogId and c.parentId is null
              and (c.status = :visible
                   or exists (select r.id from Comment r where r.parentId = c.id and r.status = :visible))
            """)
    Page<Comment> findThreadRoots(@Param("blogId") Long blogId, @Param("visible") CommentStatus visible,
            Pageable pageable);

    @Query("""
            select c from Comment c
            where c.parentId in :rootIds and c.status = :visible
            order by c.createdAt asc, c.id asc
            """)
    List<Comment> findRepliesFor(@Param("rootIds") Collection<Long> rootIds,
            @Param("visible") CommentStatus visible);
}
