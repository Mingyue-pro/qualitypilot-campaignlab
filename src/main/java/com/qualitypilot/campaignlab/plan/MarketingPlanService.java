package com.qualitypilot.campaignlab.plan;

import com.qualitypilot.campaignlab.audience.AudienceVersionRepository;
import com.qualitypilot.campaignlab.campaign.CampaignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class MarketingPlanService {
    private final MarketingPlanRepository plans; private final FlowNodeRepository nodes; private final FlowEdgeRepository edges; private final AudienceVersionRepository audiences;
    public MarketingPlanService(MarketingPlanRepository p,FlowNodeRepository n,FlowEdgeRepository e,AudienceVersionRepository a){plans=p;nodes=n;edges=e;audiences=a;}
    public record NodeInput(String key,NodeType type,String name,String config,int x,int y){}
    public record EdgeInput(String source,String target,String branchLabel){}
    public record Command(String name,String audienceId,int audienceVersion,List<NodeInput> nodes,List<EdgeInput> edges){}
    public record View(String id,String name,PlanStatus status,String audienceId,int audienceVersion,List<NodeInput> nodes,List<EdgeInput> edges){}
    @Transactional public View create(String role,Command c){requireOperator(role);basic(c);String id=UUID.randomUUID().toString();plans.save(new MarketingPlan(id,c.name(),c.audienceId(),c.audienceVersion(),Instant.now()));saveGraph(id,c);return get(id);}
    @Transactional(readOnly=true) public List<View> list(){return plans.findAll().stream().map(p->get(p.getId())).toList();}
    @Transactional(readOnly=true) public View get(String id){MarketingPlan p=plans.findById(id).orElseThrow(()->error("PLAN_NOT_FOUND","marketing plan not found",HttpStatus.NOT_FOUND));return new View(p.getId(),p.getName(),p.getStatus(),p.getAudienceId(),p.getAudienceVersion(),nodes.findByPlanIdOrderById(id).stream().map(n->new NodeInput(n.getNodeKey(),n.getType(),n.getName(),n.getConfig(),n.getPositionX(),n.getPositionY())).toList(),edges.findByPlanIdOrderById(id).stream().map(e->new EdgeInput(e.getSourceNodeKey(),e.getTargetNodeKey(),e.getBranchLabel())).toList());}
    @Transactional public View publish(String role,String id){requireOperator(role);MarketingPlan p=plans.findById(id).orElseThrow(()->error("PLAN_NOT_FOUND","marketing plan not found",HttpStatus.NOT_FOUND));validateForPublish(p);p.publish();return get(id);}
    private void saveGraph(String id,Command c){nodes.saveAll(c.nodes().stream().map(n->new FlowNode(id,n.key(),n.type(),n.name(),n.config(),n.x(),n.y())).toList());edges.saveAll(c.edges().stream().map(e->new FlowEdge(id,e.source(),e.target(),e.branchLabel())).toList());}
    private void basic(Command c){if(c==null||c.name()==null||c.name().isBlank()||c.nodes()==null||c.edges()==null)throw error("INVALID_PLAN","name, nodes and edges are required",HttpStatus.BAD_REQUEST);Set<String> keys=new HashSet<>();for(NodeInput n:c.nodes())if(n.key()==null||n.type()==null||!keys.add(n.key()))throw error("INVALID_GRAPH","node keys must be unique",HttpStatus.BAD_REQUEST);for(EdgeInput e:c.edges())if(!keys.contains(e.source())||!keys.contains(e.target()))throw error("INVALID_GRAPH","edge endpoint does not exist",HttpStatus.BAD_REQUEST);}
    private void validateForPublish(MarketingPlan p){
        var av=audiences.findByAudienceIdAndVersionNumber(p.getAudienceId(),p.getAudienceVersion()).orElseThrow(()->error("AUDIENCE_VERSION_NOT_FOUND","audience version not found",HttpStatus.CONFLICT));
        if(!av.isPublished())throw error("AUDIENCE_NOT_PUBLISHED","only a published audience version can be used",HttpStatus.CONFLICT);
        List<FlowNode> ns=nodes.findByPlanIdOrderById(p.getId()); List<FlowEdge> es=edges.findByPlanIdOrderById(p.getId());
        Map<String,FlowNode> byKey=new HashMap<>();ns.forEach(n->byKey.put(n.getNodeKey(),n));
        List<FlowNode> starts=ns.stream().filter(n->n.getType()==NodeType.START).toList();
        if(starts.size()!=1)fail("exactly one start node is required");
        if(ns.stream().noneMatch(n->n.getType()==NodeType.END))fail("at least one end node is required");
        Map<String,List<FlowEdge>> out=new HashMap<>();Map<String,List<FlowEdge>> in=new HashMap<>();
        es.forEach(e->{out.computeIfAbsent(e.getSourceNodeKey(),k->new ArrayList<>()).add(e);in.computeIfAbsent(e.getTargetNodeKey(),k->new ArrayList<>()).add(e);});
        for(FlowNode n:ns){
            String c=n.getConfig()==null?"":n.getConfig(); List<FlowEdge> next=out.getOrDefault(n.getNodeKey(),List.of());
            if(n.getType()==NodeType.MARKETING&&!Set.of("CALL","SMS","COUPON").contains(c))fail("marketing node requires CALL, SMS or COUPON");
            if(n.getType()==NodeType.DELAY&&(!c.matches("[1-9]\\d*")||Long.parseLong(c)>525600))fail("delay minutes must be a positive integer");
            if(n.getType()==NodeType.END&&!next.isEmpty())fail("end node cannot have outgoing edges");
            if(n.getType()!=NodeType.END&&n.getType()!=NodeType.DECISION&&next.size()!=1)fail("non-decision node requires exactly one outgoing edge");
            if(n.getType()==NodeType.DECISION)validateDecision(n,next,in.getOrDefault(n.getNodeKey(),List.of()),byKey);
        }
        FlowEdge startEdge=out.getOrDefault(starts.getFirst().getNodeKey(),List.of()).getFirst();
        if(byKey.get(startEdge.getTargetNodeKey()).getType()!=NodeType.DECISION)fail("start node must directly connect to a decision node");
        Set<String> reached=new HashSet<>();Deque<String> q=new ArrayDeque<>();q.add(starts.getFirst().getNodeKey());
        while(!q.isEmpty()){String k=q.remove();if(reached.add(k))out.getOrDefault(k,List.of()).forEach(e->q.add(e.getTargetNodeKey()));}
        if(reached.size()!=ns.size())fail("all nodes must be reachable from start");
        Map<String,Integer> indegree=new HashMap<>();ns.forEach(n->indegree.put(n.getNodeKey(),0));es.forEach(e->indegree.put(e.getTargetNodeKey(),indegree.get(e.getTargetNodeKey())+1));Deque<String> zero=new ArrayDeque<>();indegree.forEach((k,v)->{if(v==0)zero.add(k);});int visited=0;while(!zero.isEmpty()){String k=zero.remove();visited++;for(FlowEdge e:out.getOrDefault(k,List.of()))if(indegree.compute(e.getTargetNodeKey(),(x,v)->v-1)==0)zero.add(e.getTargetNodeKey());}if(visited!=ns.size())fail("cycle is not allowed");
    }
    private void validateDecision(FlowNode n,List<FlowEdge> next,List<FlowEdge> incoming,Map<String,FlowNode> byKey){String c=n.getConfig()==null?"":n.getConfig();Set<String> labels=next.stream().map(FlowEdge::getBranchLabel).collect(java.util.stream.Collectors.toSet());if(c.startsWith("AB:")){String[] p=c.split(":");try{if(p.length!=3||Integer.parseInt(p[1])+Integer.parseInt(p[2])!=100||Integer.parseInt(p[1])<0||Integer.parseInt(p[2])<0)fail("AB percentages must total 100");}catch(NumberFormatException e){fail("AB percentages must be integers");}if(!labels.equals(Set.of("A","B")))fail("AB decision requires A and B branches");}else if(c.startsWith("LABEL:")&&!c.substring(6).isBlank()){if(!labels.equals(Set.of("YES","NO")))fail("label decision requires YES and NO branches");}else if(c.equals("RECEIPT")){if(!labels.equals(Set.of("SUCCESS","FAILURE")))fail("receipt decision requires SUCCESS and FAILURE branches");if(incoming.size()!=1||byKey.get(incoming.getFirst().getSourceNodeKey()).getType()!=NodeType.MARKETING)fail("receipt decision must directly follow a marketing node");}else fail("decision config must be AB, LABEL or RECEIPT");for(FlowEdge e:next)if(!Set.of(NodeType.MARKETING,NodeType.PLACEHOLDER,NodeType.DELAY).contains(byKey.get(e.getTargetNodeKey()).getType()))fail("decision branch must first connect to marketing, placeholder or delay node");}
    private void fail(String message){throw error("INVALID_GRAPH",message,HttpStatus.CONFLICT);}
    private void requireOperator(String r){if(!"operator".equals(r))throw error("PERMISSION_DENIED","operation requires role: operator",HttpStatus.FORBIDDEN);}
    private CampaignException error(String c,String m,HttpStatus s){return new CampaignException(c,m,s);}
}
