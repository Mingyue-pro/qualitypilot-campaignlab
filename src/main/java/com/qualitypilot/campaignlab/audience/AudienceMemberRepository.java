package com.qualitypilot.campaignlab.audience;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AudienceMemberRepository extends JpaRepository<AudienceMember, Long> {
    List<AudienceMember> findByCalculationIdOrderByUserId(Long calculationId);
}
