package com.qualitypilot.campaignlab.campaign;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"campaignId", "userId"}))
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String campaignId;
    private String userId;
    private Instant claimedAt;

    protected Claim() {}

    public Claim(String campaignId, String userId, Instant claimedAt) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.claimedAt = claimedAt;
    }

    public Long getId() { return id; }
    public String getCampaignId() { return campaignId; }
    public String getUserId() { return userId; }
    public Instant getClaimedAt() { return claimedAt; }
}
