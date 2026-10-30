package com.qualitypilot.campaignlab.audience;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"calculationId", "userId"}))
public class AudienceMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long calculationId;
    private String userId;

    protected AudienceMember() {}
    public AudienceMember(Long calculationId, String userId) {
        this.calculationId = calculationId;
        this.userId = userId;
    }
    public Long getCalculationId() { return calculationId; }
    public String getUserId() { return userId; }
}
