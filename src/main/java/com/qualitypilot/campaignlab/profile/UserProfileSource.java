package com.qualitypilot.campaignlab.profile;

import com.qualitypilot.campaignlab.audience.AudienceCondition;
import com.qualitypilot.campaignlab.audience.MatchType;

import java.util.List;

public interface UserProfileSource {
    List<SyntheticUser> select(MatchType matchType, List<AudienceCondition> conditions);
}
