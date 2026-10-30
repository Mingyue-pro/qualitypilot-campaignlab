package com.qualitypilot.campaignlab.campaign;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
public class Campaign {
    @Id
    private String id;
    private String name;
    private Instant startsAt;
    private Instant endsAt;
    private int inventory;
    @Enumerated(EnumType.STRING)
    private CampaignStatus status;
    @Version
    private long version;

    protected Campaign() {}

    public Campaign(String id, String name, Instant startsAt, Instant endsAt, int inventory) {
        this.id = id;
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.inventory = inventory;
        this.status = CampaignStatus.DRAFT;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public int getInventory() { return inventory; }
    public CampaignStatus getStatus() { return status; }
    public void activate() { status = CampaignStatus.ACTIVE; }
    public void decrementInventory() { inventory -= 1; }
}
