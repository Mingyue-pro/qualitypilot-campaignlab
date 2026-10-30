package com.qualitypilot.campaignlab.audience;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"audienceId", "versionNumber"}))
public class AudienceVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String audienceId;
    private int versionNumber;
    @Enumerated(EnumType.STRING)
    private MatchType matchType;
    private boolean published;
    private Instant createdAt;

    protected AudienceVersion() {}
    public AudienceVersion(String audienceId, int versionNumber, MatchType matchType,
                           Instant createdAt) {
        this.audienceId = audienceId;
        this.versionNumber = versionNumber;
        this.matchType = matchType;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public String getAudienceId() { return audienceId; }
    public int getVersionNumber() { return versionNumber; }
    public MatchType getMatchType() { return matchType; }
    public boolean isPublished() { return published; }
    public Instant getCreatedAt() { return createdAt; }
    public void publish() { published = true; }
}
