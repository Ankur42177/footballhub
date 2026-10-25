package com.footballhub.club;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.footballhub.club.dto.ClubDtos.ClubResponse;
import com.footballhub.club.dto.ClubDtos.FollowResponse;
import com.footballhub.common.exception.ApiException;
import com.footballhub.security.AppUserDetails;
import com.footballhub.security.CurrentUser;
import com.footballhub.user.User;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClubService {

    private final ClubRepository clubs;
    private final CurrentUser currentUser;
    private final NamedParameterJdbcTemplate jdbc;

    public ClubService(ClubRepository clubs, CurrentUser currentUser, NamedParameterJdbcTemplate jdbc) {
        this.clubs = clubs;
        this.currentUser = currentUser;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> listClubs(AppUserDetails viewer) {
        List<Club> all = clubs.findAllByOrderByNameAsc();
        Map<Long, Long> followers = countBy("SELECT club_id AS id, COUNT(*) AS n FROM user_clubs GROUP BY club_id");
        Map<Long, Long> posts = countBy("""
                SELECT club_id AS id, COUNT(*) AS n FROM blogs
                WHERE club_id IS NOT NULL AND status = 'PUBLISHED' AND hidden = FALSE
                GROUP BY club_id
                """);
        Set<Long> followed = viewer == null ? Set.of() : followedClubIds(viewer.getId());
        return all.stream().map(club -> toResponse(club, followers.getOrDefault(club.getId(), 0L),
                posts.getOrDefault(club.getId(), 0L), followed.contains(club.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public ClubResponse getClub(String slug, AppUserDetails viewer) {
        Club club = clubs.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Club"));
        long postCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM blogs
                WHERE club_id = :clubId AND status = 'PUBLISHED' AND hidden = FALSE
                """, new MapSqlParameterSource("clubId", club.getId()), Long.class);
        boolean following = viewer != null && followedClubIds(viewer.getId()).contains(club.getId());
        return toResponse(club, followerCount(club.getId()), postCount, following);
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> followedClubs(AppUserDetails principal) {
        User user = currentUser.require(principal);
        Set<Long> followed = followedClubIds(user.getId());
        return listClubs(principal).stream().filter(club -> followed.contains(club.id())).toList();
    }

    public FollowResponse follow(Long clubId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        Club club = clubs.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        jdbc.update("""
                INSERT INTO user_clubs (user_id, club_id) VALUES (:userId, :clubId)
                ON CONFLICT (user_id, club_id) DO NOTHING
                """, params(user.getId(), club.getId()));
        return new FollowResponse(club.getId(), true, followerCount(club.getId()));
    }

    public FollowResponse unfollow(Long clubId, AppUserDetails principal) {
        User user = currentUser.require(principal);
        Club club = clubs.findById(clubId).orElseThrow(() -> ApiException.notFound("Club"));
        jdbc.update("DELETE FROM user_clubs WHERE user_id = :userId AND club_id = :clubId",
                params(user.getId(), club.getId()));
        return new FollowResponse(club.getId(), false, followerCount(club.getId()));
    }

    private long followerCount(Long clubId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM user_clubs WHERE club_id = :clubId",
                new MapSqlParameterSource("clubId", clubId), Long.class);
        return count == null ? 0 : count;
    }

    private Set<Long> followedClubIds(Long userId) {
        return new HashSet<>(jdbc.queryForList("SELECT club_id FROM user_clubs WHERE user_id = :userId",
                new MapSqlParameterSource("userId", userId), Long.class));
    }

    private Map<Long, Long> countBy(String sql) {
        Map<Long, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getLong("id"), rs.getLong("n"));
        });
        return result;
    }

    private static MapSqlParameterSource params(Long userId, Long clubId) {
        return new MapSqlParameterSource().addValue("userId", userId).addValue("clubId", clubId);
    }

    private static ClubResponse toResponse(Club club, long followers, long posts, boolean following) {
        return new ClubResponse(club.getId(), club.getName(), club.getSlug(), club.getShortName(),
                club.getPrimaryColor(), club.getLeague(), club.getDescription(), followers, posts, following);
    }
}
