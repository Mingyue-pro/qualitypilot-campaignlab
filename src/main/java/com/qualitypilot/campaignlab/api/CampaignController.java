package com.qualitypilot.campaignlab.api;

import com.qualitypilot.campaignlab.campaign.Campaign;
import com.qualitypilot.campaignlab.campaign.CampaignService;
import com.qualitypilot.campaignlab.campaign.Claim;
import com.qualitypilot.campaignlab.config.TestDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CampaignController {
    private final CampaignService service;
    private final TestDataService testData;

    public CampaignController(CampaignService service, TestDataService testData) {
        this.service = service;
        this.testData = testData;
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    Campaign create(
            @RequestHeader(value = "X-Role", required = false) String role,
            @Valid @RequestBody CreateCampaignRequest request) {
        return service.create(role, request.name(), request.startsAt(), request.endsAt(), request.inventory());
    }

    @GetMapping("/campaigns/{id}")
    Campaign get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping("/campaigns/{id}/activate")
    Campaign activate(
            @RequestHeader(value = "X-Role", required = false) String role,
            @PathVariable String id) {
        return service.activate(role, id);
    }

    @PostMapping("/campaigns/{id}/claims")
    @ResponseStatus(HttpStatus.CREATED)
    Claim claim(
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable String id) {
        return service.claim(role, userId, id);
    }

    @GetMapping("/campaigns/{id}/claims")
    List<Claim> listClaims(@PathVariable String id) {
        return service.listClaims(id);
    }

    @PostMapping("/test/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void reset(@RequestHeader(value = "X-Role", required = false) String role) {
        if (!"operator".equals(role)) {
            service.reset(role);
        }
        testData.reset();
    }

    record CreateCampaignRequest(
            @NotBlank String name,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt,
            @Min(0) int inventory) {}
}
