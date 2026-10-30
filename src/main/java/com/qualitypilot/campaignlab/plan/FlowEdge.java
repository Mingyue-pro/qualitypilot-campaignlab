package com.qualitypilot.campaignlab.plan;

import jakarta.persistence.*;

@Entity
public class FlowEdge {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="plan_id") private String planId; @Column(name="source_node_key") private String sourceNodeKey; @Column(name="target_node_key") private String targetNodeKey; @Column(name="branch_label") private String branchLabel;
    protected FlowEdge() {}
    public FlowEdge(String planId,String source,String target,String label){planId(planId);sourceNodeKey=source;targetNodeKey=target;branchLabel=label;}
    private void planId(String value){this.planId=value;}
    public String getSourceNodeKey(){return sourceNodeKey;} public String getTargetNodeKey(){return targetNodeKey;} public String getBranchLabel(){return branchLabel;}
}
