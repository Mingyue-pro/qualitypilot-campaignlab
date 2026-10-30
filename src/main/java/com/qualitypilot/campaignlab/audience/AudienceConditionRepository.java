package com.qualitypilot.campaignlab.audience;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AudienceConditionRepository extends JpaRepository<AudienceConditionEntity, Long> {
    List<AudienceConditionEntity> findByVersionIdOrderById(Long versionId);
}
