package com.footballhub.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHashAndType(String tokenHash, TokenType type);

    /** Expires any outstanding tokens of a type when a new one is issued. */
    @Modifying
    @Query("update UserToken t set t.usedAt = :now where t.user.id = :userId and t.type = :type and t.usedAt is null")
    int invalidateOutstanding(@Param("userId") Long userId, @Param("type") TokenType type, @Param("now") Instant now);

    /** Atomically claims a token; returns 0 if it was already used by a concurrent request. */
    @Modifying
    @Query("update UserToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);
}
