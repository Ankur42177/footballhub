package com.footballhub.club;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "clubs")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 80)
    private String league = "Premier League";

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "short_name", length = 5)
    private String shortName;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Club() {
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getLeague() {
        return league;
    }

    public String getDescription() {
        return description;
    }

    public String getShortName() {
        return shortName;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }
}
