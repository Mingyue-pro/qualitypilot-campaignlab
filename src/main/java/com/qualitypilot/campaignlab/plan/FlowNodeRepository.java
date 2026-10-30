package com.qualitypilot.campaignlab.plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FlowNodeRepository extends JpaRepository<FlowNode,Long>{List<FlowNode> findByPlanIdOrderById(String planId);void deleteByPlanId(String planId);}
