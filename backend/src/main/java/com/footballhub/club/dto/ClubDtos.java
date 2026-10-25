package com.footballhub.club.dto;

import com.footballhub.club.Club;

public final class ClubDtos {

    private ClubDtos() {
    }

    /** Compact club reference embedded in blog responses. */
    public record ClubSummary(Long id, String name, String slug, String shortName, String primaryColor) {

        public static ClubSummary from(Club club) {
            return club == null ? null
                    : new ClubSummary(club.getId(), club.getName(), club.getSlug(), club.getShortName(),
                            club.getPrimaryColor());
        }
    }

    public record ClubResponse(Long id, String name, String slug, String shortName, String primaryColor,
            String league, String description, long followerCount, long postCount, boolean followedByMe) {
    }

    public record FollowResponse(Long clubId, boolean following, long followerCount) {
    }
}
