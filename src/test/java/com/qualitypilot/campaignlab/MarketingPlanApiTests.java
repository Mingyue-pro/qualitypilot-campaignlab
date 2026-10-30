package com.qualitypilot.campaignlab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class MarketingPlanApiTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 @Test void draftPreservesFiveNodeTypesAndCanBeReopened() throws Exception {
   String audience=publishedAudience(); String body=plan(audience,true);
   String response=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.nodes.length()").value(7)).andReturn().getResponse().getContentAsString();
   String id=mapper.readTree(response).get("id").asText();
   mvc.perform(get("/api/marketing-plans/{id}",id)).andExpect(status().isOk()).andExpect(jsonPath("$.nodes[3].type").value("PLACEHOLDER")).andExpect(jsonPath("$.nodes[4].type").value("DELAY"));
 }
 @Test void incompleteDecisionCanBeDraftButCannotPublish() throws Exception {
   String audience=publishedAudience(); String response=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(plan(audience,false))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
   mvc.perform(post("/api/marketing-plans/{id}/publish",mapper.readTree(response).get("id").asText()).header("X-Role","operator")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_GRAPH"));
 }
 @Test void validConnectedGraphPublishes() throws Exception {
   String audience=publishedAudience(); String response=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(plan(audience,true))).andReturn().getResponse().getContentAsString();
   mvc.perform(post("/api/marketing-plans/{id}/publish",mapper.readTree(response).get("id").asText()).header("X-Role","operator")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
 }
 @Test void abPercentagesMustTotalOneHundred() throws Exception {
   String audience=publishedAudience();String body=plan(audience,true).replace("LABEL:memberLevel=VIP","AB:60:30").replace("\"YES\"","\"A\"").replace("\"NO\"","\"B\"");
   String response=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(body)).andReturn().getResponse().getContentAsString();
   mvc.perform(post("/api/marketing-plans/{id}/publish",mapper.readTree(response).get("id").asText()).header("X-Role","operator")).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("AB percentages must total 100"));
 }
 @Test void receiptDecisionMustDirectlyFollowMarketing() throws Exception {
   String audience=publishedAudience();String body=plan(audience,true).replace("LABEL:memberLevel=VIP","RECEIPT").replace("\"YES\"","\"SUCCESS\"").replace("\"NO\"","\"FAILURE\"");
   String response=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(body)).andReturn().getResponse().getContentAsString();
   mvc.perform(post("/api/marketing-plans/{id}/publish",mapper.readTree(response).get("id").asText()).header("X-Role","operator")).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("receipt decision must directly follow a marketing node"));
 }
 @Test void publishedPlanCreatesRunAndUserTrace() throws Exception {
   String audience=publishedAudience();String created=mvc.perform(post("/api/marketing-plans").header("X-Role","operator").contentType("application/json").content(plan(audience,true))).andReturn().getResponse().getContentAsString();String planId=mapper.readTree(created).get("id").asText();mvc.perform(post("/api/marketing-plans/{id}/publish",planId).header("X-Role","operator")).andExpect(status().isOk());
   String run=mvc.perform(post("/api/marketing-plans/{id}/runs",planId).header("X-Role","operator")).andExpect(status().isCreated()).andExpect(jsonPath("$.audienceBatch").value(1)).andReturn().getResponse().getContentAsString();
   mvc.perform(get("/api/runs/{runId}/journeys/U001/trace",mapper.readTree(run).get("id").asText())).andExpect(status().isOk()).andExpect(jsonPath("$[0].nodeType").value("START")).andExpect(jsonPath("$[1].nodeType").value("DECISION"));
 }
 private String publishedAudience() throws Exception {
   String json="""
     {"name":"计划客群","type":"STATIC","refreshFrequency":"NONE","matchType":"ALL","conditions":[{"field":"city","operator":"EQUALS","value":"Shanghai"}]}
     """;
   String a=mvc.perform(post("/api/audiences").header("X-Role","operator").contentType("application/json").content(json)).andReturn().getResponse().getContentAsString();
   var j=mapper.readTree(a);mvc.perform(post("/api/audiences/{id}/versions/1/publish",j.get("audienceId").asText()).header("X-Role","operator")).andExpect(status().isOk());return j.get("audienceId").asText();
 }
 private String plan(String audience,boolean both){
   return """
     {"name":"欢迎旅程","audienceId":"%s","audienceVersion":1,"nodes":[
       {"key":"start","type":"START","name":"开始","config":"立即","x":80,"y":100},
       {"key":"judge","type":"DECISION","name":"是否VIP","config":"LABEL:memberLevel=VIP","x":280,"y":100},
       {"key":"send","type":"MARKETING","name":"发送短信","config":"SMS","x":500,"y":40},
       {"key":"blank","type":"PLACEHOLDER","name":"空白节点","config":"","x":500,"y":180},
       {"key":"delay","type":"DELAY","name":"等待5分钟","config":"5","x":700,"y":40},
       {"key":"end1","type":"END","name":"成功结束","config":"","x":900,"y":40},
       {"key":"end2","type":"END","name":"其他结束","config":"","x":700,"y":180}],
      "edges":[{"source":"start","target":"judge","branchLabel":null},{"source":"judge","target":"send","branchLabel":"YES"},%s{"source":"send","target":"delay","branchLabel":null},{"source":"delay","target":"end1","branchLabel":null},{"source":"blank","target":"end2","branchLabel":null}]}
     """.formatted(audience,both?"{\"source\":\"judge\",\"target\":\"blank\",\"branchLabel\":\"NO\"},":"");
 }
}
