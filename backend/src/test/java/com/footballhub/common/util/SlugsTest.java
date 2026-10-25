package com.footballhub.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugsTest {

    @Test
    void producesLowercaseHyphenatedAscii() {
        assertThat(Slugs.slugify("  Ødegaard's Role: The #8 & Beyond!  ")).isEqualTo("odegaards-role-the-8-and-beyond");
        assertThat(Slugs.slugify("Müller über alles")).isEqualTo("muller-uber-alles");
    }

    @Test
    void fallsBackWhenNothingUsableRemains() {
        assertThat(Slugs.slugify("!!!")).isEqualTo("post");
        assertThat(Slugs.slugify(null)).isEqualTo("post");
    }

    @Test
    void capsLengthWithoutTrailingHyphen() {
        String slug = Slugs.slugify("word ".repeat(40));
        assertThat(slug.length()).isLessThanOrEqualTo(80);
        assertThat(slug).doesNotEndWith("-");
    }

    @Test
    void randomSuffixHasRequestedLength() {
        assertThat(Slugs.randomSuffix(6)).hasSize(6).matches("[a-z0-9]+");
    }
}
