package com.qualitypilot.campaignlab.audience;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AudienceCalculationRepository extends JpaRepository<AudienceCalculation, Long> {
    Optional<AudienceCalculation> findTopByAudienceIdAndRuleVersionOrderByBatchNumberDesc(
            String audienceId, int ruleVersion);
}
