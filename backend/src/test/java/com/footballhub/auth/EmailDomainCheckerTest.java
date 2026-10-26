package com.footballhub.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class EmailDomainCheckerTest {

    private final EmailDomainChecker checker = new EmailDomainChecker(Set.of("gmail.com", "example.org")::contains);

    @Test
    void acceptsDomainsThatExist() {
        assertThat(checker.canReceiveMail("fan@gmail.com")).isTrue();
        assertThat(checker.canReceiveMail("Fan@Example.ORG")).isTrue();
    }

    @Test
    void rejectsTyposAndMalformedAddresses() {
        assertThat(checker.canReceiveMail("fan@gmial.co")).isFalse();
        assertThat(checker.canReceiveMail("fan@localhost")).isFalse();
        assertThat(checker.canReceiveMail("fan@")).isFalse();
        assertThat(checker.canReceiveMail("@gmail.com")).isFalse();
        assertThat(checker.canReceiveMail("fan@.com")).isFalse();
        assertThat(checker.canReceiveMail(null)).isFalse();
    }
}
