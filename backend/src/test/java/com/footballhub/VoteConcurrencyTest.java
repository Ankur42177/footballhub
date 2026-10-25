package com.footballhub;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.footballhub.blog.Blog;
import com.footballhub.blog.BlogRepository;
import com.footballhub.security.AppUserDetails;
import com.footballhub.user.User;
import com.footballhub.vote.VoteService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Acceptance criterion: votes stay unique and consistent under concurrent requests. */
class VoteConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private VoteService voteService;

    @Autowired
    private BlogRepository blogRepository;

    @Test
    void concurrentDuplicateVotesKeepTheCountExact() throws Exception {
        AppUserDetails authorDetails = createUser("author");
        User author = userRepository.findById(authorDetails.getId()).orElseThrow();
        Blog blog = new Blog(author, null, "Under load", "under-load", null, "Body");
        blog.publish();
        blogRepository.saveAndFlush(blog);

        List<AppUserDetails> fans = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            fans.add(createUser("fan" + i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (AppUserDetails fan : fans) {
            for (int attempt = 0; attempt < 3; attempt++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    voteService.vote(blog.getId(), fan);
                    return null;
                }));
            }
        }
        start.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM blog_votes WHERE blog_id = ?", Integer.class,
                blog.getId());
        Integer cached = jdbc.queryForObject("SELECT vote_count FROM blogs WHERE id = ?", Integer.class,
                blog.getId());
        assertThat(rows).isEqualTo(fans.size());
        assertThat(cached).isEqualTo(fans.size());
    }
}
