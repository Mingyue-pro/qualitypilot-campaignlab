package com.qualitypilot.campaignlab.audience;

import com.qualitypilot.campaignlab.campaign.CampaignException;
import com.qualitypilot.campaignlab.profile.SyntheticUser;
import com.qualitypilot.campaignlab.profile.SyntheticUserRepository;
import com.qualitypilot.campaignlab.profile.UserProfileSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AudienceService {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "city", "memberLevel", "newUser", "riskLevel", "marketingAllowed");
    private final AudienceRepository audiences;
    private final AudienceVersionRepository versions;
    private final AudienceConditionRepository conditions;
    private final AudienceCalculationRepository calculations;
    private final AudienceMemberRepository members;
    private final UserProfileSource profiles;
    private final SyntheticUserRepository users;

    public AudienceService(AudienceRepository audiences, AudienceVersionRepository versions,
                           AudienceConditionRepository conditions,
                           AudienceCalculationRepository calculations,
                           AudienceMemberRepository members, UserProfileSource profiles,
                           SyntheticUserRepository users) {
        this.audiences = audiences;
        this.versions = versions;
        this.conditions = conditions;
        this.calculations = calculations;
        this.members = members;
        this.profiles = profiles;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PreviewView preview(String role, CreateAudienceCommand command) {
        requireOperator(role);
        validate(command);
        List<SyntheticUser> selected = select(command.matchType(), command.conditions());
        return new PreviewView(selected.size(), selected.stream().map(SyntheticUser::getUserId).toList());
    }

    @Transactional
    public AudienceView create(String role, CreateAudienceCommand command) {
        requireOperator(role);
        validate(command);
        // Save always recalculates from the current profile data; it never trusts an older preview.
        List<SyntheticUser> selected = select(command.matchType(), command.conditions());
        Audience audience = audiences.save(new Audience(UUID.randomUUID().toString(), command.name(),
                command.type(), normalizedFrequency(command), Instant.now()));
        AudienceVersion version = versions.save(new AudienceVersion(
                audience.getId(), 1, command.matchType(), Instant.now()));
        conditions.saveAll(command.conditions().stream()
                .map(rule -> new AudienceConditionEntity(version.getId(), rule)).toList());
        AudienceCalculation calculation = saveCalculation(audience, version, 1, selected);
        return view(audience, version, calculation);
    }

    @Transactional
    public AudienceView refreshDynamic(String role, String audienceId) {
        requireOperator(role);
        Audience audience = findAudience(audienceId);
        if (audience.getType() != AudienceType.DYNAMIC) {
            throw new CampaignException("STATIC_AUDIENCE_NOT_REFRESHABLE",
                    "static audience members are fixed; create a new rule version instead",
                    HttpStatus.CONFLICT);
        }
        AudienceVersion version = latestVersion(audienceId);
        List<AudienceCondition> savedConditions = rules(version);
        List<SyntheticUser> selected = select(version.getMatchType(), savedConditions);
        int nextBatch = latestCalculation(audienceId, version.getVersionNumber()).getBatchNumber() + 1;
        return view(audience, version, saveCalculation(audience, version, nextBatch, selected));
    }

    @Transactional
    public AudienceView publish(String role, String audienceId, int versionNumber) {
        requireOperator(role);
        Audience audience = findAudience(audienceId);
        AudienceVersion version = findVersion(audienceId, versionNumber);
        version.publish();
        return view(audience, version, latestCalculation(audienceId, versionNumber));
    }

    @Transactional(readOnly = true)
    public List<AudienceView> list() {
        return audiences.findAll().stream()
                .sorted(Comparator.comparing(Audience::getCreatedAt).reversed())
                .map(audience -> {
                    AudienceVersion version = latestVersion(audience.getId());
                    return view(audience, version,
                            latestCalculation(audience.getId(), version.getVersionNumber()));
                }).toList();
    }

    @Transactional(readOnly = true)
    public AudienceView get(String audienceId, int versionNumber) {
        Audience audience = findAudience(audienceId);
        AudienceVersion version = findVersion(audienceId, versionNumber);
        return view(audience, version, latestCalculation(audienceId, versionNumber));
    }

    @Transactional(readOnly = true)
    public List<SyntheticUser> getMembers(String audienceId, int versionNumber) {
        AudienceCalculation calculation = latestCalculation(audienceId, versionNumber);
        List<String> ids = members.findByCalculationIdOrderByUserId(calculation.getId())
                .stream().map(AudienceMember::getUserId).toList();
        return users.findAllById(ids).stream()
                .sorted(Comparator.comparing(SyntheticUser::getUserId)).toList();
    }

    @Transactional
    public void deleteAll() {
        members.deleteAll();
        calculations.deleteAll();
        conditions.deleteAll();
        versions.deleteAll();
        audiences.deleteAll();
    }

    private AudienceCalculation saveCalculation(Audience audience, AudienceVersion version,
                                                  int batchNumber, List<SyntheticUser> selected) {
        AudienceCalculation calculation = calculations.save(new AudienceCalculation(
                audience.getId(), version.getVersionNumber(), batchNumber, selected.size(), Instant.now()));
        members.saveAll(selected.stream()
                .map(user -> new AudienceMember(calculation.getId(), user.getUserId())).toList());
        return calculation;
    }

    private List<AudienceCondition> rules(AudienceVersion version) {
        return conditions.findByVersionIdOrderById(version.getId()).stream()
                .map(item -> new AudienceCondition(item.getFieldName(), item.getOperator(),
                        item.getConditionValue())).toList();
    }

    private List<SyntheticUser> select(MatchType matchType, List<AudienceCondition> rules) {
        try {
            return profiles.select(matchType, rules);
        } catch (IllegalArgumentException error) {
            throw new CampaignException("INVALID_AUDIENCE_RULE", error.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void validate(CreateAudienceCommand command) {
        if (command.name() == null || command.name().isBlank() || command.type() == null ||
                command.conditions() == null || command.conditions().isEmpty() || command.matchType() == null) {
            throw new CampaignException("INVALID_AUDIENCE_RULE",
                    "name, type, matchType and at least one condition are required", HttpStatus.BAD_REQUEST);
        }
        if (command.type() == AudienceType.DYNAMIC && command.refreshFrequency() != RefreshFrequency.DAILY) {
            throw new CampaignException("INVALID_REFRESH_FREQUENCY",
                    "dynamic audience requires DAILY refresh frequency", HttpStatus.BAD_REQUEST);
        }
        for (AudienceCondition condition : command.conditions()) {
            if (condition == null || condition.field() == null || condition.operator() == null ||
                    condition.value() == null || !ALLOWED_FIELDS.contains(condition.field())) {
                throw new CampaignException("INVALID_AUDIENCE_RULE",
                        "unsupported or incomplete audience condition", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private RefreshFrequency normalizedFrequency(CreateAudienceCommand command) {
        return command.type() == AudienceType.STATIC ? RefreshFrequency.NONE : command.refreshFrequency();
    }

    private Audience findAudience(String id) {
        return audiences.findById(id).orElseThrow(() -> new CampaignException(
                "RESOURCE_NOT_FOUND", "audience not found", HttpStatus.NOT_FOUND));
    }

    private AudienceVersion findVersion(String id, int number) {
        return versions.findByAudienceIdAndVersionNumber(id, number).orElseThrow(() ->
                new CampaignException("RESOURCE_NOT_FOUND", "audience version not found", HttpStatus.NOT_FOUND));
    }

    private AudienceVersion latestVersion(String id) {
        return versions.findTopByAudienceIdOrderByVersionNumberDesc(id).orElseThrow(() ->
                new CampaignException("RESOURCE_NOT_FOUND", "audience version not found", HttpStatus.NOT_FOUND));
    }

    private AudienceCalculation latestCalculation(String id, int ruleVersion) {
        return calculations.findTopByAudienceIdAndRuleVersionOrderByBatchNumberDesc(id, ruleVersion)
                .orElseThrow(() -> new CampaignException(
                        "RESOURCE_NOT_FOUND", "audience calculation not found", HttpStatus.NOT_FOUND));
    }

    private AudienceView view(Audience audience, AudienceVersion version,
                              AudienceCalculation calculation) {
        return new AudienceView(audience.getId(), audience.getName(), audience.getType(),
                audience.getRefreshFrequency(), version.getVersionNumber(), version.getMatchType(),
                version.isPublished(), calculation.getBatchNumber(), calculation.getMemberCount(),
                calculation.getCalculatedAt());
    }

    private void requireOperator(String role) {
        if (!"operator".equals(role)) {
            throw new CampaignException("PERMISSION_DENIED", "operation requires role: operator",
                    HttpStatus.FORBIDDEN);
        }
    }

    public record CreateAudienceCommand(String name, AudienceType type,
                                        RefreshFrequency refreshFrequency, MatchType matchType,
                                        List<AudienceCondition> conditions) {}
    public record PreviewView(int estimatedCount, List<String> memberIds) {}
    public record AudienceView(String audienceId, String name, AudienceType type,
                               RefreshFrequency refreshFrequency, int ruleVersion,
                               MatchType matchType, boolean published, int latestBatch,
                               int memberCount, Instant calculatedAt) {}
}
