package com.footballhub.blog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.footballhub.club.Club;
import com.footballhub.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/** Query building for the public feed: search, club and author filters. */
final class BlogSpecifications {

    private BlogSpecifications() {
    }

    /**
     * @param featuredClub when not null, order the results for the "featured" feed: articles from
     *                     that club first, then the rest, each by upvotes, then newest. An empty
     *                     string means no featured club (everything by upvotes).
     */
    static Specification<Blog> publicFeed(String query, String clubSlug, String authorUsername,
            Instant publishedAfter, String featuredClub) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), BlogStatus.PUBLISHED));
            predicates.add(cb.isFalse(root.<Boolean>get("hidden")));

            if (clubSlug != null && !clubSlug.isBlank()) {
                Join<Blog, Club> club = root.join("club");
                predicates.add(cb.equal(club.get("slug"), clubSlug.trim().toLowerCase(Locale.ROOT)));
            }
            if (authorUsername != null && !authorUsername.isBlank()) {
                Join<Blog, User> author = root.join("author");
                predicates.add(cb.equal(author.get("username"), authorUsername.trim().toLowerCase(Locale.ROOT)));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("excerpt")), pattern, '\\'),
                        cb.like(cb.lower(root.<String>get("content")), pattern, '\\')));
            }
            if (publishedAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("publishedAt"), publishedAfter));
            }
            // Ordering applies to the page query only, not the count query Spring runs alongside it.
            if (featuredClub != null && !Long.class.equals(criteriaQuery.getResultType())) {
                List<Order> orders = new ArrayList<>();
                if (!featuredClub.isEmpty()) {
                    Join<Blog, Club> clubForOrder = root.join("club", JoinType.LEFT);
                    orders.add(cb.desc(cb.<Integer>selectCase()
                            .when(cb.equal(clubForOrder.get("slug"), featuredClub), 1)
                            .otherwise(0)));
                }
                orders.add(cb.desc(root.get("voteCount")));
                orders.add(cb.desc(root.get("publishedAt")));
                orders.add(cb.desc(root.get("id")));
                criteriaQuery.orderBy(orders);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
