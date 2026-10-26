package com.footballhub.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlogCoverTest {

    @Test
    void firstCommonsImageBecomesTheCover() {
        String content = """
                Intro text.

                ![Rodri playing for Spain](https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Rodri.jpg/1280px-Rodri.jpg "Caption||Photo: X||https://commons.wikimedia.org/wiki/File:Rodri.jpg")

                ![Second](https://upload.wikimedia.org/wikipedia/commons/a/b/Other.jpg "x")
                """;
        var cover = BlogMapper.coverOf(content);
        assertThat(cover.url()).isEqualTo("https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7a/Rodri.jpg/1280px-Rodri.jpg");
        assertThat(cover.alt()).isEqualTo("Rodri playing for Spain");
    }

    @Test
    void imagesFromOtherHostsAreIgnored() {
        assertThat(BlogMapper.coverOf("![x](https://evil.example/track.png)")).isNull();
        assertThat(BlogMapper.coverOf("No images here.")).isNull();
        assertThat(BlogMapper.coverOf(null)).isNull();
    }
}
