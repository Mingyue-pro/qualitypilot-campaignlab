package com.qualitypilot.campaignlab.audience;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"audienceId", "ruleVersion", "batchNumber"}))
public class AudienceCalculation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String audienceId;
    private int ruleVersion;
    private int batchNumber;
    private int memberCount;
    private Instant calculatedAt;

    protected AudienceCalculation() {}

    public AudienceCalculation(String audienceId, int ruleVersion, int batchNumber,
                               int memberCount, Instant calculatedAt) {
        this.audienceId = audienceId;
        this.ruleVersion = ruleVersion;
        this.batchNumber = batchNumber;
        this.memberCount = memberCount;
        this.calculatedAt = calculatedAt;
    }

    public Long getId() { return id; }
    public String getAudienceId() { return audienceId; }
    public int getRuleVersion() { return ruleVersion; }
    public int getBatchNumber() { return batchNumber; }
    public int getMemberCount() { return memberCount; }
    public Instant getCalculatedAt() { return calculatedAt; }
}
