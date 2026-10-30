package com.qualitypilot.campaignlab.api;

import com.qualitypilot.campaignlab.audience.AudienceService;
import com.qualitypilot.campaignlab.audience.AudienceService.AudienceView;
import com.qualitypilot.campaignlab.audience.AudienceService.CreateAudienceCommand;
import com.qualitypilot.campaignlab.audience.AudienceService.PreviewView;
import com.qualitypilot.campaignlab.profile.SyntheticUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audiences")
public class AudienceController {
    private final AudienceService service;

    public AudienceController(AudienceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AudienceView create(@RequestHeader(value = "X-Role", required = false) String role,
                        @RequestBody CreateAudienceCommand command) {
        return service.create(role, command);
    }

    @PostMapping("/preview")
    PreviewView preview(@RequestHeader(value = "X-Role", required = false) String role,
                        @RequestBody CreateAudienceCommand command) {
        return service.preview(role, command);
    }

    @GetMapping
    List<AudienceView> list() {
        return service.list();
    }

    @PostMapping("/{id}/refresh")
    @ResponseStatus(HttpStatus.CREATED)
    AudienceView refresh(@RequestHeader(value = "X-Role", required = false) String role,
                         @PathVariable String id) {
        return service.refreshDynamic(role, id);
    }

    @PostMapping("/{id}/versions/{version}/publish")
    AudienceView publish(@RequestHeader(value = "X-Role", required = false) String role,
                         @PathVariable String id, @PathVariable int version) {
        return service.publish(role, id, version);
    }

    @GetMapping("/{id}/versions/{version}")
    AudienceView get(@PathVariable String id, @PathVariable int version) {
        return service.get(id, version);
    }

    @GetMapping("/{id}/versions/{version}/members")
    List<SyntheticUser> members(@PathVariable String id, @PathVariable int version) {
        return service.getMembers(id, version);
    }
}
