package com.qualitypilot.campaignlab.profile;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "doris_user_profile")
public class SyntheticUser {
    @Id
    private String userId;
    private String city;
    private String memberLevel;
    private boolean newUser;
    private String riskLevel;
    private boolean marketingAllowed;

    protected SyntheticUser() {}

    public SyntheticUser(String userId, String city, String memberLevel, boolean newUser,
                         String riskLevel, boolean marketingAllowed) {
        this.userId = userId;
        this.city = city;
        this.memberLevel = memberLevel;
        this.newUser = newUser;
        this.riskLevel = riskLevel;
        this.marketingAllowed = marketingAllowed;
    }

    public String getUserId() { return userId; }
    public String getCity() { return city; }
    public String getMemberLevel() { return memberLevel; }
    public boolean isNewUser() { return newUser; }
    public String getRiskLevel() { return riskLevel; }
    public boolean isMarketingAllowed() { return marketingAllowed; }
}
