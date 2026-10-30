package com.qualitypilot.campaignlab;

import com.qualitypilot.campaignlab.campaign.CampaignRepository;
import com.qualitypilot.campaignlab.campaign.ClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignLabApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CampaignRepository campaigns;
    @Autowired ClaimRepository claims;

    @BeforeEach
    void cleanDatabase() {
        claims.deleteAll();
        campaigns.deleteAll();
    }

    @Test
    void completeCreateActivateAndClaimFlow() throws Exception {
        String campaignId = createCampaign("operator", 2);

        mvc.perform(post("/api/campaigns/{id}/activate", campaignId)
                        .header("X-Role", "operator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mvc.perform(post("/api/campaigns/{id}/claims", campaignId)
                        .header("X-Role", "user")
                        .header("X-User-Id", "user-001"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-001"));

        mvc.perform(get("/api/campaigns/{id}", campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventory").value(1));
    }

    @Test
    void userCannotCreateCampaign() throws Exception {
        mvc.perform(post("/api/campaigns")
                        .header("X-Role", "user")
                        .contentType("application/json")
                        .content(campaignJson(2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void draftCampaignCannotBeClaimedAndInventoryStaysUnchanged() throws Exception {
        String campaignId = createCampaign("operator", 2);

        mvc.perform(post("/api/campaigns/{id}/claims", campaignId)
                        .header("X-Role", "user")
                        .header("X-User-Id", "user-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        mvc.perform(get("/api/campaigns/{id}", campaignId))
                .andExpect(jsonPath("$.inventory").value(2));
    }

    @Test
    void duplicateClaimIsRejectedWithoutSecondInventoryChange() throws Exception {
        String campaignId = createCampaign("operator", 2);
        mvc.perform(post("/api/campaigns/{id}/activate", campaignId)
                .header("X-Role", "operator"));
        mvc.perform(post("/api/campaigns/{id}/claims", campaignId)
                .header("X-Role", "user").header("X-User-Id", "user-001"));

        mvc.perform(post("/api/campaigns/{id}/claims", campaignId)
                        .header("X-Role", "user")
                        .header("X-User-Id", "user-001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_CLAIM"));

        mvc.perform(get("/api/campaigns/{id}", campaignId))
                .andExpect(jsonPath("$.inventory").value(1));
    }

    private String createCampaign(String role, int inventory) throws Exception {
        String response = mvc.perform(post("/api/campaigns")
                        .header("X-Role", role)
                        .contentType("application/json")
                        .content(campaignJson(inventory)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("id").asText();
    }

    private String campaignJson(int inventory) {
        return """
                {
                  "name": "Autumn reward",
                  "startsAt": "%s",
                  "endsAt": "%s",
                  "inventory": %d
                }
                """.formatted(
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600),
                inventory);
    }
}
