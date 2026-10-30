package com.qualitypilot.campaignlab.config;

import com.qualitypilot.campaignlab.audience.AudienceService;
import com.qualitypilot.campaignlab.campaign.CampaignRepository;
import com.qualitypilot.campaignlab.campaign.ClaimRepository;
import com.qualitypilot.campaignlab.profile.SyntheticUserData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestDataService {
    private final AudienceService audiences;
    private final SyntheticUserData userData;
    private final ClaimRepository claims;
    private final CampaignRepository campaigns;

    public TestDataService(AudienceService audiences, SyntheticUserData userData,
                           ClaimRepository claims, CampaignRepository campaigns) {
        this.audiences = audiences;
        this.userData = userData;
        this.claims = claims;
        this.campaigns = campaigns;
    }

    @Transactional
    public void reset() {
        audiences.deleteAll();
        claims.deleteAll();
        campaigns.deleteAll();
        userData.reset();
    }
}
