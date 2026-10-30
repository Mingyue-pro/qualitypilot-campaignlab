package com.qualitypilot.campaignlab.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    boolean existsByCampaignIdAndUserId(String campaignId, String userId);
    List<Claim> findByCampaignIdOrderByClaimedAt(String campaignId);
}
