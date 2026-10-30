package com.qualitypilot.campaignlab.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.qualitypilot.campaignlab.profile.SyntheticUserData;

@Configuration
public class SeedDataConfiguration {
    @Bean
    ApplicationRunner seedSyntheticData(SyntheticUserData userData) {
        return args -> userData.seedIfEmpty();
    }
}
