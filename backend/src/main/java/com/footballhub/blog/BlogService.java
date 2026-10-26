package com.footballhub.blog;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import com.footballhub.blog.dto.BlogDtos.BlogDetailResponse;
import com.footballhub.blog.dto.BlogDtos.BlogSummaryResponse;
import com.footballhub.blog.dto.BlogDtos.CreateBlogRequest;
import com.footballhub.blog.dto.BlogDtos.UpdateBlogRequest;
import com.footballhub.club.Club;
import com.footballhub.club.ClubRepository;
import com.footballhub.common.dto.PageResponse;
import com.footballhub.common.exception.ApiException;
import com.footballhub.common.util.Pagination;
import com.footballhub.common.util.Slugs;
import com.footballhub.config.AppProperties;
import com.footballhub.moderation.AuditService;
import com.footballhub.review.ReviewService;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.Role;
import com.footballhub.user.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blog business rules (section 9.6): the author always comes from the session, slugs
 * are generated server-side, new posts are drafts unless publishing is requested, and
 * only the author (or an administrator) may change a post.
 */
@Service
@Transactional
public class BlogService {

    public enum FeedSort {
        LATEST, TOP, TRENDING, OLDEST,
        /** The featured club's articles first, then all others; each group by upvotes, then newest. */
        FEATURED;

        static FeedSort parse(String value) {
            if (value == null || value.isBlank()) {
                return LATEST;
            }
            try {
                return FeedSort.valueOf(value.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("INVALID_SORT", "Sort must be featured, latest, top, trending or oldest.");
            }
        }
    }

    private static final Duration TRENDING_WINDOW = Duration.ofDays(14);

    private final BlogRepository blogs;
    private final ClubRepository clubs;
    private final BlogMapper mapper;
    private final CurrentUser currentUser;
    private final AuditService auditService;
    private final AppProperties properties;
    private final ReviewService reviews;

    public BlogService(BlogRepository blogs, ClubRepository clubs, BlogMapper mapper, CurrentUser currentUser,
            AuditService auditService, AppProperties properties, ReviewService reviews) {
        this.reviews = reviews;
        this.blogs = blogs;
        this.clubs = clubs;
        this.mapper = mapper;
        this.currentUser = currentUser;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogSummaryResponse> feed(String query, String club, String author, String sort,
            Integer page, Integer size, AppUserDetails viewer) {
        FeedSort feedSort = FeedSort.parse(sort);
        Sort order = switch (feedSort) {
            case TOP, TRENDING -> Sort.by(Sort.Order.desc("voteCount"), Sort.Order.desc("publishedAt"),
                    Sort.Order.desc("id"));
            case OLDEST -> Sort.by(Sort.Order.asc("publishedAt"), Sort.Order.asc("id"));
            case LATEST -> Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
            // Ordered inside the query (the club comparison isn't a plain column sort).
            case FEATURED -> Sort.unsorted();
        };
        Instant publishedAfter = feedSort == FeedSort.TRENDING ? Instant.now().minus(TRENDING_WINDOW) : null;
        String featuredClub = feedSort == FeedSort.FEATURED ? properties.featuredClub() : null;
        PageRequest pageable = Pagination.of(page, size, 12, order);
        Page<Blog> result = blogs.findAll(
                BlogSpecifications.publicFeed(query, club, author, publishedAfter, featuredClub), pageable);
        return PageResponse.of(result, mapper.toSummaries(result.getContent(), viewerId(viewer)));
    }

    /**
     * Published, visible posts are public. Drafts, archived and hidden posts are visible only
     * to their author and staff; everyone else gets a 404 so their existence is not revealed.
     */
    @Transactional(readOnly = true)
    public BlogDetailResponse getBySlug(String slug, AppUserDetails viewer) {
        Blog blog = blogs.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Article"));
        if (!blog.isPubliclyVisible() && !canSeeUnpublished(blog, viewer)) {
            throw ApiException.notFound("Article");
        }
        return mapper.toDetail(blog, viewerId(viewer), viewer == null ? null : viewer.getRole());
    }

    public BlogDetailResponse create(CreateBlogRequest request, AppUserDetails principal) {
        User author = currentUser.require(principal);
        Club club = resolveClub(request.clubId());
        String title = request.title().trim();
        Blog blog = new Blog(author, club, title, uniqueSlug(title), normalizeExcerpt(request.excerpt()),
                request.content().trim());
        if (request.publish()) {
            requirePublishingAllowed(author);
            if (isAdmin(author)) {
                blog.publish();
            }
        }
        blogs.saveAndFlush(blog);
        if (request.publish() && !isAdmin(author)) {
            // Only administrators publish directly; everyone else's story goes to review.
            reviews.submit(blog);
        }
        return mapper.toDetail(blog, author.getId(), author.getRole());
    }

    public BlogDetailResponse update(Long id, UpdateBlogRequest request, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Blog blog = findOwnedOrAdmin(id, actor);
        if (!isAdmin(actor) && blog.getStatus() == BlogStatus.PENDING_REVIEW) {
            // The reviewer must approve exactly what they were sent.
            throw ApiException.conflict("IN_REVIEW",
                    "This story is waiting for review. You can edit it again once it has been reviewed.");
        }
        String title = request.title().trim();
        if (blog.getStatus() == BlogStatus.DRAFT && !title.equals(blog.getTitle())) {
            // Drafts follow their title; published URLs stay stable.
            blog.setSlug(uniqueSlug(title));
        }
        blog.setTitle(title);
        blog.setExcerpt(normalizeExcerpt(request.excerpt()));
        blog.setContent(request.content().trim());
        blog.setClub(resolveClub(request.clubId()));
        blogs.saveAndFlush(blog);
        if (!isAdmin(actor) && blog.getStatus() == BlogStatus.PUBLISHED) {
            // Changes to a live story by a non-admin are reviewed before they go live again.
            reviews.submit(blog);
        }
        auditIfNotAuthor(actor, blog, "BLOG_EDITED");
        return mapper.toDetail(blog, actor.getId(), actor.getRole());
    }

    public BlogDetailResponse publish(Long id, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Blog blog = findOwnedOrAdmin(id, actor);
        if (isAdmin(actor)) {
            if (blog.getStatus() != BlogStatus.PUBLISHED) {
                blog.approve();
                blogs.saveAndFlush(blog);
            }
        }
        else if (blog.getStatus() != BlogStatus.PUBLISHED && blog.getStatus() != BlogStatus.PENDING_REVIEW) {
            requirePublishingAllowed(blog.getAuthor());
            reviews.submit(blog);
        }
        return mapper.toDetail(blog, actor.getId(), actor.getRole());
    }

    public BlogDetailResponse archive(Long id, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Blog blog = findOwnedOrAdmin(id, actor);
        blog.archive();
        blogs.saveAndFlush(blog);
        auditIfNotAuthor(actor, blog, "BLOG_ARCHIVED");
        return mapper.toDetail(blog, actor.getId(), actor.getRole());
    }

    /** Votes, bookmarks, comments and reports are removed by ON DELETE CASCADE. */
    public void delete(Long id, AppUserDetails principal) {
        User actor = currentUser.require(principal);
        Blog blog = findOwnedOrAdmin(id, actor);
        auditIfNotAuthor(actor, blog, "BLOG_DELETED");
        blogs.delete(blog);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlogSummaryResponse> myBlogs(AppUserDetails principal, String status, Integer page,
            Integer size) {
        User user = currentUser.require(principal);
        PageRequest pageable = Pagination.of(page, size, 20,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id")));
        Page<Blog> result;
        if (status == null || status.isBlank()) {
            result = blogs.findByAuthorId(user.getId(), pageable);
        }
        else {
            BlogStatus parsed;
            try {
                parsed = BlogStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("INVALID_STATUS", "Status must be draft, published or archived.");
            }
            result = blogs.findByAuthorIdAndStatus(user.getId(), parsed, pageable);
        }
        return PageResponse.of(result, mapper.toSummaries(result.getContent(), user.getId()));
    }

    /** Used by votes, comments, bookmarks and reports: the post must be live for the public. */
    public Blog requirePubliclyVisible(Long id) {
        Blog blog = blogs.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        if (!blog.isPubliclyVisible()) {
            throw ApiException.notFound("Article");
        }
        return blog;
    }

    private Blog findOwnedOrAdmin(Long id, User actor) {
        Blog blog = blogs.findById(id).orElseThrow(() -> ApiException.notFound("Article"));
        boolean isAuthor = blog.getAuthor().getId().equals(actor.getId());
        if (!isAuthor && actor.getRole() != Role.ADMIN) {
            // Do not reveal unpublished posts to non-owners.
            if (!blog.isPubliclyVisible()) {
                throw ApiException.notFound("Article");
            }
            throw ApiException.forbidden("You can only change your own articles.");
        }
        return blog;
    }

    private static boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean canSeeUnpublished(Blog blog, AppUserDetails viewer) {
        if (viewer == null) {
            return false;
        }
        return blog.getAuthor().getId().equals(viewer.getId()) || viewer.getRole().isStaff();
    }

    private void requirePublishingAllowed(User author) {
        if (properties.requireVerifiedEmailToPublish() && !author.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Please verify your email address before publishing.");
        }
    }

    private Club resolveClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return clubs.findById(clubId)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CLUB", "That club does not exist."));
    }

    private String uniqueSlug(String title) {
        String base = Slugs.slugify(title);
        if (!blogs.existsBySlug(base)) {
            return base;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            String candidate = base + "-" + Slugs.randomSuffix(6);
            if (!blogs.existsBySlug(candidate)) {
                return candidate;
            }
        }
        // The unique index is the final guard; a collision here surfaces as 409.
        return base + "-" + Slugs.randomSuffix(12);
    }

    private static String normalizeExcerpt(String excerpt) {
        if (excerpt == null) {
            return null;
        }
        String trimmed = excerpt.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void auditIfNotAuthor(User actor, Blog blog, String action) {
        if (!blog.getAuthor().getId().equals(actor.getId())) {
            auditService.record(actor, action, "BLOG", blog.getId(), "Article \"" + blog.getTitle() + "\"");
        }
    }

    private static Long viewerId(AppUserDetails viewer) {
        return viewer == null ? null : viewer.getId();
    }
}
