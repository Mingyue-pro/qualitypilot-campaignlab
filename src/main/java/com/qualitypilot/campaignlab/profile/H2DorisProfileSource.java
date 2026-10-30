package com.qualitypilot.campaignlab.profile;

import com.qualitypilot.campaignlab.audience.AudienceCondition;
import com.qualitypilot.campaignlab.audience.ConditionOperator;
import com.qualitypilot.campaignlab.audience.MatchType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class H2DorisProfileSource implements UserProfileSource {
    private static final Map<String, String> FIELDS = Map.of(
            "city", "city",
            "memberLevel", "memberLevel",
            "newUser", "newUser",
            "riskLevel", "riskLevel",
            "marketingAllowed", "marketingAllowed"
    );

    private final EntityManager entityManager;

    public H2DorisProfileSource(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<SyntheticUser> select(MatchType matchType, List<AudienceCondition> conditions) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<SyntheticUser> query = builder.createQuery(SyntheticUser.class);
        Root<SyntheticUser> user = query.from(SyntheticUser.class);
        List<Predicate> predicates = new ArrayList<>();

        for (AudienceCondition condition : conditions) {
            String attribute = FIELDS.get(condition.field());
            if (attribute == null) {
                throw new IllegalArgumentException("unsupported audience field: " + condition.field());
            }
            Object value = typedValue(condition.field(), condition.value());
            Predicate predicate = condition.operator() == ConditionOperator.EQUALS
                    ? builder.equal(user.get(attribute), value)
                    : builder.notEqual(user.get(attribute), value);
            predicates.add(predicate);
        }

        Predicate combined = matchType == MatchType.ALL
                ? builder.and(predicates.toArray(Predicate[]::new))
                : builder.or(predicates.toArray(Predicate[]::new));
        query.select(user).where(combined).orderBy(builder.asc(user.get("userId")));
        return entityManager.createQuery(query).getResultList();
    }

    private Object typedValue(String field, String value) {
        if (field.equals("newUser") || field.equals("marketingAllowed")) {
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException(field + " requires true or false");
            }
            return Boolean.valueOf(value);
        }
        return value;
    }
}
