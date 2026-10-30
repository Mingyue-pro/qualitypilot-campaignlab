package com.qualitypilot.campaignlab.plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FlowEdgeRepository extends JpaRepository<FlowEdge,Long>{List<FlowEdge> findByPlanIdOrderById(String planId);void deleteByPlanId(String planId);}
