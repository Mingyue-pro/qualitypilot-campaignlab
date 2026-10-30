package com.qualitypilot.campaignlab.plan;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class MarketingPlan {
    @Id private String id;
    private String name;
    @Enumerated(EnumType.STRING) private PlanStatus status;
    @Column(name="audience_id") private String audienceId;
    @Column(name="audience_version") private int audienceVersion;
    @Column(name="created_at") private Instant createdAt;
    @Version private long version;
    protected MarketingPlan() {}
    public MarketingPlan(String id, String name, String audienceId, int audienceVersion, Instant createdAt) {
        this.id=id; this.name=name; this.audienceId=audienceId; this.audienceVersion=audienceVersion;
        this.createdAt=createdAt; this.status=PlanStatus.DRAFT;
    }
    public String getId(){return id;} public String getName(){return name;} public PlanStatus getStatus(){return status;}
    public String getAudienceId(){return audienceId;} public int getAudienceVersion(){return audienceVersion;}
    public Instant getCreatedAt(){return createdAt;} public void publish(){status=PlanStatus.PUBLISHED;}
}
