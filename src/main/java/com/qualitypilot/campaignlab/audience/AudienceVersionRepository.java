package com.qualitypilot.campaignlab.audience;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AudienceVersionRepository extends JpaRepository<AudienceVersion, Long> {
    Optional<AudienceVersion> findByAudienceIdAndVersionNumber(String audienceId, int versionNumber);
    Optional<AudienceVersion> findTopByAudienceIdOrderByVersionNumberDesc(String audienceId);
}
