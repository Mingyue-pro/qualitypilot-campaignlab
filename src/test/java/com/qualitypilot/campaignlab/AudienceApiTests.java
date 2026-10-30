package com.qualitypilot.campaignlab;

import com.qualitypilot.campaignlab.config.TestDataService;
import com.qualitypilot.campaignlab.profile.SyntheticUser;
import com.qualitypilot.campaignlab.profile.SyntheticUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AudienceApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired TestDataService testData;
    @Autowired SyntheticUserRepository users;

    @BeforeEach
    void reset() {
        testData.reset();
    }

    @Test
    void staticAudienceSavesCurrentMembersFromSyntheticDorisTable() throws Exception {
        String response = createAudience("operator", staticAudienceJson())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberCount").value(1))
                .andExpect(jsonPath("$.ruleVersion").value(1))
                .andExpect(jsonPath("$.latestBatch").value(1))
                .andExpect(jsonPath("$.type").value("STATIC"))
                .andExpect(jsonPath("$.refreshFrequency").value("NONE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode body = mapper.readTree(response);

        mvc.perform(get("/api/audiences/{id}/versions/1/members", body.get("audienceId").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("U001"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void saveRecalculatesInsteadOfTrustingOlderPreview() throws Exception {
        mvc.perform(post("/api/audiences/preview")
                        .header("X-Role", "operator").contentType("application/json")
                        .content(staticAudienceJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedCount").value(1));

        users.save(new SyntheticUser("U001", "Shanghai", "NORMAL", true, "LOW", true));

        createAudience("operator", staticAudienceJson())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberCount").value(0));
    }

    @Test
    void dynamicRefreshCreatesNewBatchWithoutChangingRuleVersion() throws Exception {
        String response = createAudience("operator", dynamicAudienceJson())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(response).get("audienceId").asText();

        users.save(new SyntheticUser("U001", "Shanghai", "NORMAL", true, "LOW", true));

        mvc.perform(post("/api/audiences/{id}/refresh", id).header("X-Role", "operator"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleVersion").value(1))
                .andExpect(jsonPath("$.latestBatch").value(2))
                .andExpect(jsonPath("$.memberCount").value(0));
    }

    @Test
    void staticAudienceCannotBeAutomaticallyRefreshed() throws Exception {
        String response = createAudience("operator", staticAudienceJson())
                .andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(response).get("audienceId").asText();

        mvc.perform(post("/api/audiences/{id}/refresh", id).header("X-Role", "operator"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATIC_AUDIENCE_NOT_REFRESHABLE"));
    }

    @Test
    void managementListShowsStaticAndDynamicAudiences() throws Exception {
        createAudience("operator", staticAudienceJson()).andExpect(status().isCreated());
        createAudience("operator", dynamicAudienceJson()).andExpect(status().isCreated());

        mvc.perform(get("/api/audiences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rejectsUnsupportedProfileFieldWithoutSavingAudience() throws Exception {
        createAudience("operator", """
                {"name":"unsafe","type":"STATIC","matchType":"ALL","conditions":[
                  {"field":"phoneNumber","operator":"EQUALS","value":"13800000000"}
                ]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AUDIENCE_RULE"));
    }

    @Test
    void nonOperatorCannotCreateAudience() throws Exception {
        createAudience("approver", staticAudienceJson())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void dynamicAudienceRequiresDailyRefreshFrequency() throws Exception {
        createAudience("operator", dynamicAudienceJson().replace("DAILY", "NONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_FREQUENCY"));
    }

    private org.springframework.test.web.servlet.ResultActions createAudience(String role, String json)
            throws Exception {
        return mvc.perform(post("/api/audiences")
                .header("X-Role", role).contentType("application/json").content(json));
    }

    private String staticAudienceJson() {
        return audienceJson("STATIC", "NONE");
    }

    private String dynamicAudienceJson() {
        return audienceJson("DYNAMIC", "DAILY");
    }

    private String audienceJson(String type, String frequency) {
        return """
                {"name":"上海VIP新用户","type":"%s","refreshFrequency":"%s","matchType":"ALL","conditions":[
                  {"field":"city","operator":"EQUALS","value":"Shanghai"},
                  {"field":"memberLevel","operator":"EQUALS","value":"VIP"},
                  {"field":"newUser","operator":"EQUALS","value":"true"}
                ]}
                """.formatted(type, frequency);
    }
}
