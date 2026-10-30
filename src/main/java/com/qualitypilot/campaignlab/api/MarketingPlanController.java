package com.qualitypilot.campaignlab.api;
import com.qualitypilot.campaignlab.plan.MarketingPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/marketing-plans")
public class MarketingPlanController{
 private final MarketingPlanService service; public MarketingPlanController(MarketingPlanService s){service=s;}
 @GetMapping List<MarketingPlanService.View> list(){return service.list();}
 @GetMapping("/{id}") MarketingPlanService.View get(@PathVariable String id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) MarketingPlanService.View create(@RequestHeader(value="X-Role",required=false)String role,@RequestBody MarketingPlanService.Command body){return service.create(role,body);}
 @PostMapping("/{id}/publish") MarketingPlanService.View publish(@RequestHeader(value="X-Role",required=false)String role,@PathVariable String id){return service.publish(role,id);}
}
