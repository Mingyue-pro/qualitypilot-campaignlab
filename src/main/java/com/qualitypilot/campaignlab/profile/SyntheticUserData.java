package com.qualitypilot.campaignlab.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SyntheticUserData {
    private final SyntheticUserRepository users;

    public SyntheticUserData(SyntheticUserRepository users) {
        this.users = users;
    }

    @Transactional
    public void reset() {
        users.deleteAll();
        users.saveAll(List.of(
                new SyntheticUser("U001", "Shanghai", "VIP", true, "LOW", true),
                new SyntheticUser("U002", "Shanghai", "NORMAL", false, "LOW", true),
                new SyntheticUser("U003", "Beijing", "VIP", false, "LOW", true),
                new SyntheticUser("U004", "Shanghai", "VIP", false, "MEDIUM", true),
                new SyntheticUser("U005", "Shanghai", "NORMAL", true, "LOW", true),
                new SyntheticUser("U006", "Shenzhen", "NORMAL", true, "HIGH", false)
        ));
    }

    @Transactional
    public void seedIfEmpty() {
        if (users.count() == 0) {
            reset();
        }
    }
}
