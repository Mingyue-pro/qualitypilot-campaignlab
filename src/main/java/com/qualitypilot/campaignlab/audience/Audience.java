package com.qualitypilot.campaignlab.audience;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class Audience {
    @Id
    private String id;
    private String name;
    @Enumerated(EnumType.STRING)
    private AudienceType type;
    @Enumerated(EnumType.STRING)
    private RefreshFrequency refreshFrequency;
    private Instant createdAt;

    protected Audience() {}
    public Audience(String id, String name, AudienceType type,
                    RefreshFrequency refreshFrequency, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.refreshFrequency = refreshFrequency;
        this.createdAt = createdAt;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public AudienceType getType() { return type; }
    public RefreshFrequency getRefreshFrequency() { return refreshFrequency; }
    public Instant getCreatedAt() { return createdAt; }
}
