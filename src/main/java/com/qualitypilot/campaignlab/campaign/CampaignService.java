package com.qualitypilot.campaignlab.campaign;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CampaignService {
    private final CampaignRepository campaigns;
    private final ClaimRepository claims;
    private final Clock clock;

    @Autowired
    public CampaignService(CampaignRepository campaigns, ClaimRepository claims) {
        this(campaigns, claims, Clock.systemUTC());
    }

    CampaignService(CampaignRepository campaigns, ClaimRepository claims, Clock clock) {
        this.campaigns = campaigns;
        this.claims = claims;
        this.clock = clock;
    }

    @Transactional
    public Campaign create(String role, String name, Instant startsAt, Instant endsAt, int inventory) {
        requireRole(role, "operator");
        if (name == null || name.isBlank()) {
            throw error("INVALID_CAMPAIGN", "name must not be empty", HttpStatus.BAD_REQUEST);
        }
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            throw error("INVALID_CAMPAIGN", "startsAt must be before endsAt", HttpStatus.BAD_REQUEST);
        }
        if (inventory < 0) {
            throw error("INVALID_CAMPAIGN", "inventory must not be negative", HttpStatus.BAD_REQUEST);
        }
        return campaigns.save(new Campaign(UUID.randomUUID().toString(), name, startsAt, endsAt, inventory));
    }

    @Transactional(readOnly = true)
    public Campaign get(String id) {
        return campaigns.findById(id).orElseThrow(() ->
                error("CAMPAIGN_NOT_FOUND", "campaign not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Campaign activate(String role, String id) {
        requireRole(role, "operator");
        Campaign campaign = get(id);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw error("INVALID_STATE", "only a draft campaign can be activated", HttpStatus.CONFLICT);
        }
        campaign.activate();
        return campaign;
    }

    @Transactional
    public Claim claim(String role, String userId, String campaignId) {
        requireRole(role, "user");
        if (userId == null || userId.isBlank()) {
            throw error("USER_REQUIRED", "X-User-Id header is required", HttpStatus.BAD_REQUEST);
        }
        Campaign campaign = campaigns.findByIdForUpdate(campaignId).orElseThrow(() ->
                error("CAMPAIGN_NOT_FOUND", "campaign not found", HttpStatus.NOT_FOUND));
        Instant now = clock.instant();
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw error("INVALID_STATE", "only an active campaign can be claimed", HttpStatus.CONFLICT);
        }
        if (now.isBefore(campaign.getStartsAt()) || now.isAfter(campaign.getEndsAt())) {
            throw error("OUTSIDE_CLAIM_PERIOD", "campaign is outside its claim period", HttpStatus.CONFLICT);
        }
        if (campaign.getInventory() == 0) {
            throw error("INVENTORY_EXHAUSTED", "campaign inventory is exhausted", HttpStatus.CONFLICT);
        }
        if (claims.existsByCampaignIdAndUserId(campaignId, userId)) {
            throw error("DUPLICATE_CLAIM", "user has already claimed this campaign", HttpStatus.CONFLICT);
        }
        campaign.decrementInventory();
        return claims.save(new Claim(campaignId, userId, now));
    }

    @Transactional(readOnly = true)
    public List<Claim> listClaims(String campaignId) {
        get(campaignId);
        return claims.findByCampaignIdOrderByClaimedAt(campaignId);
    }

    @Transactional
    public void reset(String role) {
        requireRole(role, "operator");
        claims.deleteAll();
        campaigns.deleteAll();
    }

    private void requireRole(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw error("PERMISSION_DENIED", "operation requires role: " + expected, HttpStatus.FORBIDDEN);
        }
    }

    private CampaignException error(String code, String message, HttpStatus status) {
        return new CampaignException(code, message, status);
    }
}
