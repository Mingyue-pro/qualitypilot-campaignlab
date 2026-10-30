package com.qualitypilot.campaignlab.plan;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"plan_id","node_key"}))
public class FlowNode {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="plan_id") private String planId; @Column(name="node_key") private String nodeKey;
    @Enumerated(EnumType.STRING) private NodeType type;
    private String name; @Column(length=2000) private String config;
    @Column(name="position_x") private int positionX; @Column(name="position_y") private int positionY;
    protected FlowNode() {}
    public FlowNode(String planId,String nodeKey,NodeType type,String name,String config,int x,int y){
        this.planId=planId;this.nodeKey=nodeKey;this.type=type;this.name=name;this.config=config;this.positionX=x;this.positionY=y;
    }
    public String getNodeKey(){return nodeKey;} public NodeType getType(){return type;} public String getName(){return name;}
    public String getConfig(){return config;} public int getPositionX(){return positionX;} public int getPositionY(){return positionY;}
}
