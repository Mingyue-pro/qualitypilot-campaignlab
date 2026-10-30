package com.qualitypilot.campaignlab.audience;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AudienceConditionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long versionId;
    private String fieldName;
    @Enumerated(EnumType.STRING)
    private ConditionOperator operator;
    private String conditionValue;

    protected AudienceConditionEntity() {}
    public AudienceConditionEntity(Long versionId, AudienceCondition condition) {
        this.versionId = versionId;
        this.fieldName = condition.field();
        this.operator = condition.operator();
        this.conditionValue = condition.value();
    }
    public Long getVersionId() { return versionId; }
    public String getFieldName() { return fieldName; }
    public ConditionOperator getOperator() { return operator; }
    public String getConditionValue() { return conditionValue; }
}
