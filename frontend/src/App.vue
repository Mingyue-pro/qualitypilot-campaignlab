<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api, type AudienceCommand, type AudienceView, type MarketingPlan, type NodeType, type RunView, type Journey, type Trace } from './api'
import FlowBranch from './FlowBranch.vue'

type ViewName = 'list' | 'create' | 'detail' | 'planlist' | 'plancanvas' | 'monitor'
const fields = [['city','城市'],['memberLevel','会员等级'],['newUser','是否新用户'],['riskLevel','风险等级'],['marketingAllowed','是否允许营销']]
const view = ref<ViewName>('list')
const audiences = ref<AudienceView[]>([])
const current = ref<AudienceView>()
const members = ref<unknown[]>([])
const previewResult = ref<{ estimatedCount: number; memberIds: string[] }>()
const loading = ref(false)
const error = ref('')
const notice = ref('')
const plans = ref<MarketingPlan[]>([])
const planName = ref('欢迎旅程')
const planNodes = ref([{key:'start',type:'START' as NodeType,name:'开始',config:'立即',x:60,y:120}])
const planEdges = ref<{source:string;target:string;branchLabel:string|null}[]>([])
const runs = ref<RunView[]>([]); const runJourneys=ref<Journey[]>([]); const selectedRun=ref<RunView>(); const selectedTrace=ref<Trace[]>([])
const form = reactive<AudienceCommand>({ name:'上海VIP新用户', type:'STATIC', refreshFrequency:'NONE', matchType:'ALL', conditions:[] })
const dynamic = computed(() => form.type === 'DYNAMIC')
const branchTargetKeys = computed(() => new Set(planEdges.value.filter(edge => edge.branchLabel).map(edge => edge.target)))
const mainPlanNodes = computed(() => planNodes.value.filter(node => !branchTargetKeys.value.has(node.key)))
function nodeByKey(key:string){ return planNodes.value.find(node => node.key === key) }
const startNext = computed(()=>{const edge=planEdges.value.find(e=>e.source==='start'&&!e.branchLabel);return edge?nodeByKey(edge.target):undefined})

function resetForm() {
  Object.assign(form, { name:'上海VIP新用户', type:'STATIC', refreshFrequency:'NONE', matchType:'ALL', conditions:[
    { field:'city', operator:'EQUALS', value:'Shanghai' },
    { field:'memberLevel', operator:'EQUALS', value:'VIP' },
    { field:'marketingAllowed', operator:'EQUALS', value:'true' }
  ]})
  previewResult.value = undefined
  error.value = ''
}
function goList() { view.value='list'; current.value=undefined; members.value=[]; error.value='' }
function goCreate() { resetForm(); view.value='create' }
function openDetail(item: AudienceView) { current.value=item; members.value=[]; error.value=''; view.value='detail' }
function setType() { form.refreshFrequency = dynamic.value ? 'DAILY' : 'NONE' }
function addCondition() { form.conditions.push({ field:'city', operator:'EQUALS', value:'' }) }
function removeCondition(index: number) { form.conditions.splice(index, 1) }
async function run<T>(action: () => Promise<T>): Promise<T | undefined> {
  loading.value=true; error.value=''
  try { return await action() } catch (e) { error.value=e instanceof Error?e.message:'请求失败' } finally { loading.value=false }
}
async function loadList() { const data=await run(api.list); if(data) audiences.value=data }
async function preview() { const data=await run(()=>api.preview(form)); if(data) previewResult.value=data }
async function save() {
  const item=await run(()=>api.create(form))
  if(item){ notice.value=`客群“${item.name}”已保存为草稿`; await loadList(); goList() }
}
async function publish() {
  if(!current.value)return
  const item=await run(()=>api.publish(current.value!)); if(item){ current.value=item; notice.value='当前规则版本已发布'; await loadList() }
}
async function refreshAudience() {
  if(!current.value)return
  const item=await run(()=>api.refresh(current.value!)); if(item){ current.value=item; notice.value=`动态计算批次 R${item.latestBatch} 已完成`; await loadList() }
}
async function loadMembers() { if(current.value){ const data=await run(()=>api.members(current.value!)); if(data) members.value=data } }
onMounted(()=>{ resetForm(); loadList() })
async function goPlans(){view.value='planlist';const data=await run(api.plans);if(data)plans.value=data}
function goCanvas(){planName.value='欢迎旅程';planNodes.value=[{key:'start',type:'START',name:'开始',config:'立即',x:60,y:120}];planEdges.value=[];view.value='plancanvas'}
function addNode(type:NodeType){const labels={START:'开始',MARKETING:'营销节点',DECISION:'判断节点',PLACEHOLDER:'空白节点',DELAY:'延迟节点',END:'结束'};const defaults={START:'立即',MARKETING:'SMS',DECISION:'LABEL:memberLevel=VIP',PLACEHOLDER:'',DELAY:'5',END:''};const previous=planNodes.value.at(-1)!;const key=`n${planNodes.value.length}`;planNodes.value.push({key,type,name:labels[type],config:defaults[type],x:300,y:80+planNodes.value.length*150});planEdges.value.push({source:previous.key,target:key,branchLabel:null})}
function addDecision(kind:'AB'|'LABEL'|'RECEIPT'){const previous=planNodes.value.at(-1)!;const key=`n${planNodes.value.length}`;const config=kind==='AB'?'AB:50:50':kind==='LABEL'?'LABEL:memberLevel=VIP':'RECEIPT';planNodes.value.push({key,type:'DECISION',name:kind==='AB'?'A/B判断':kind==='LABEL'?'标签判断':'回执判断',config,x:300,y:80+planNodes.value.length*150});planEdges.value.push({source:previous.key,target:key,branchLabel:null})}
function decisionLabels(config:string){return config.startsWith('AB:')?['A','B']:config.startsWith('LABEL:')?['YES','NO']:['SUCCESS','FAILURE']}
function branchTarget(source:string,label:string){const edge=planEdges.value.find(e=>e.source===source&&e.branchLabel===label);return edge?nodeByKey(edge.target):undefined}
function addBranchNode(source:string,label:string,type:'MARKETING'|'PLACEHOLDER'|'DELAY'){const key=`n${Date.now()}-${planNodes.value.length}`;const defaults={MARKETING:'SMS',PLACEHOLDER:'',DELAY:'5'};const names={MARKETING:'营销节点',PLACEHOLDER:'空白节点',DELAY:'延迟节点'};planNodes.value.push({key,type,name:names[type],config:defaults[type],x:label==='A'||label==='YES'||label==='SUCCESS'?120:480,y:360});planEdges.value.push({source,target:key,branchLabel:label})}
function addConnectedNode(payload:{source:string,type:NodeType,branchLabel:string|null,decisionKind?:'AB'|'LABEL'|'RECEIPT'}){const key=`n${Date.now()}-${planNodes.value.length}`;const kind=payload.decisionKind;const defaults:Record<NodeType,string>={START:'立即',MARKETING:'SMS',DECISION:kind==='AB'?'AB:50:50':kind==='LABEL'?'LABEL:memberLevel=VIP':'RECEIPT',PLACEHOLDER:'',DELAY:'5',END:''};const names:Record<NodeType,string>={START:'开始',MARKETING:'营销节点',DECISION:kind==='AB'?'A/B判断':kind==='LABEL'?'标签判断':'回执判断',PLACEHOLDER:'空白节点',DELAY:'延迟节点',END:'结束'};planNodes.value.push({key,type:payload.type,name:names[payload.type],config:defaults[payload.type],x:300,y:120+planNodes.value.length*140});planEdges.value.push({source:payload.source,target:key,branchLabel:payload.branchLabel})}
async function savePlan(){const audience=audiences.value.find(a=>a.published);if(!audience){error.value='请先发布一个客群版本';return}const data=await run(()=>api.createPlan({name:planName.value,audienceId:audience.audienceId,audienceVersion:audience.ruleVersion,nodes:planNodes.value,edges:planEdges.value}));if(data){notice.value='营销计划草稿已保存';await goPlans()}}
async function publishPlan(item:MarketingPlan){const data=await run(()=>api.publishPlan(item.id));if(data){notice.value='营销计划已发布';await goPlans()}}
async function startPlan(item:MarketingPlan){const data=await run(()=>api.startPlan(item.id));if(data){notice.value=`执行批次已启动：R${data.audienceBatch}`;await goMonitor()}}
async function goMonitor(){view.value='monitor';const data=await run(api.runs);if(data)runs.value=data}
async function openRun(item:RunView){selectedRun.value=item;selectedTrace.value=[];const data=await run(()=>api.journeys(item.id));if(data)runJourneys.value=data}
async function openTrace(item:Journey){if(selectedRun.value){const data=await run(()=>api.trace(selectedRun.value!.id,item.userId));if(data)selectedTrace.value=data}}
</script>

<template>
  <div class="shell">
    <aside>
      <div class="brand"><b>C</b><span><strong>CampaignLab</strong><small>营销实验平台</small></span></div>
      <nav>
        <p>CDP 客群管理</p>
        <a class="active" href="#" @click.prevent="goList">◎ 客群圈选</a>
        <p>MDP 营销管理</p>
        <a href="#" :class="{active:view==='planlist'||view==='plancanvas'}" @click.prevent="goPlans">◇ 营销计划</a><a href="#" :class="{active:view==='monitor'}" @click.prevent="goMonitor">▷ 执行监控</a>
      </nav>
      <footer>● MySQL 业务库 · 模拟 Doris 画像</footer>
    </aside>

    <main>
      <template v-if="view==='planlist'">
        <header><div><label>MDP 营销管理 / 营销计划</label><h1>营销计划</h1><p>通过流程画布设计用户营销旅程。</p></div><i>QualityPilot 被测系统</i></header>
        <div v-if="notice" class="notice">{{notice}}</div><div v-if="error" class="error-box">{{error}}</div>
        <section><div class="section-title"><div><h2>全部计划</h2><p>草稿可继续完善，发布时执行完整画布校验。</p></div><button @click="goCanvas">＋ 创建营销计划</button></div><div class="table"><table><thead><tr><th>计划名称</th><th>客群版本</th><th>节点数</th><th>状态</th><th>操作</th></tr></thead><tbody><tr v-for="p in plans" :key="p.id"><td>{{p.name}}</td><td>{{p.audienceId.slice(0,8)}}… / V{{p.audienceVersion}}</td><td>{{p.nodes.length}}</td><td><span :class="['pill',{ok:p.status==='PUBLISHED'}]">{{p.status==='PUBLISHED'?'已发布':'草稿'}}</span></td><td><button v-if="p.status==='DRAFT'" class="light small" @click="publishPlan(p)">发布校验</button><button v-else class="small" @click="startPlan(p)">启动执行</button></td></tr><tr v-if="!plans.length"><td colspan="5" class="empty">暂无营销计划</td></tr></tbody></table></div></section>
      </template>
      <template v-else-if="view==='plancanvas'">
        <header><div><label>MDP 营销管理 / 营销计划 / 创建计划</label><h1>流程画布</h1><p>组合节点并保存为营销计划草稿。</p></div><i>QualityPilot 被测系统</i></header>
        <div class="back" @click="goPlans">← 返回营销计划列表</div><div v-if="error" class="error-box">{{error}}</div>
        <section><div class="grid"><label>计划名称<input v-model="planName"></label><label>目标客群<select><option v-for="a in audiences.filter(x=>x.published)" :key="a.audienceId">{{a.name}} / V{{a.ruleVersion}}</option></select></label></div>
          <div v-if="planNodes.length===1" class="node-toolbar"><span>进入流程后必须先分流：</span><button class="light" @click="addDecision('AB')">＋ A/B判断</button><button class="light" @click="addDecision('LABEL')">＋ 标签判断</button></div>
          <div class="canvas"><div class="flow-node start"><small>START</small><strong>开始</strong></div><div class="single-connector">↓</div><FlowBranch v-if="startNext" :node="startNext" :nodes="planNodes" :edges="planEdges" @add="addConnectedNode"/></div>
          <div class="actions"><button @click="savePlan">保存草稿</button><span class="pending-note">发布操作在计划列表执行严格校验</span></div></section>
      </template>
      <template v-else-if="view==='monitor'">
        <header><div><label>MDP 营销管理 / 执行监控</label><h1>执行监控</h1><p>查看执行批次、用户状态和逐节点轨迹。</p></div><i>QualityPilot 被测系统</i></header><div v-if="error" class="error-box">{{error}}</div>
        <section><div class="section-title"><div><h2>执行批次</h2><p>每次启动独立保存，不覆盖历史结果。</p></div><button class="light" @click="goMonitor">刷新</button></div><div class="table"><table><thead><tr><th>批次ID</th><th>客群批次</th><th>用户数</th><th>状态</th><th>启动时间</th><th>操作</th></tr></thead><tbody><tr v-for="r in runs" :key="r.id"><td class="mono">{{r.id.slice(0,8)}}…</td><td>R{{r.audienceBatch}}</td><td>{{r.totalUsers}}</td><td>{{r.status}}</td><td>{{new Date(r.startedAt).toLocaleString('zh-CN')}}</td><td><button class="light small" @click="openRun(r)">查看用户</button></td></tr><tr v-if="!runs.length"><td colspan="6" class="empty">暂无执行批次</td></tr></tbody></table></div></section>
        <section v-if="selectedRun"><h2>用户流程</h2><div class="journey-layout"><div><button v-for="j in runJourneys" :key="j.userId" class="journey-item" @click="openTrace(j)">{{j.userId}} · {{j.status}}</button></div><div class="trace"><div v-for="(t,i) in selectedTrace" :key="t.id" class="trace-step"><b>{{i+1}}. {{t.nodeType}}</b><span>{{t.result}}<template v-if="t.branchLabel"> · 路径 {{t.branchLabel}}</template></span><small>{{new Date(t.executedAt).toLocaleString('zh-CN')}}</small></div><p v-if="!selectedTrace.length">选择用户查看节点轨迹</p></div></div></section>
      </template>
      <template v-else>
      <header>
        <div><label>CDP 客群管理 / 客群圈选<span v-if="view==='detail'"> / 客群详情</span><span v-if="view==='create'"> / 创建客群</span></label><h1>{{ view==='list'?'客群列表':view==='create'?'创建客群':'客群详情' }}</h1><p>{{ view==='list'?'管理静态和动态客群，查看发布状态与最新计算结果。':view==='create'?'使用标签条件圈选用户，预览后保存为客群草稿。':'查看客群规则版本、计算批次和成员结果。' }}</p></div>
        <i>QualityPilot 被测系统</i>
      </header>

      <div v-if="notice" class="notice">{{notice}}<button @click="notice=''">×</button></div>
      <div v-if="error" class="error-box">{{error}}</div>

      <template v-if="view==='list'">
        <section class="list-panel">
          <div class="section-title"><div><h2>全部客群</h2><p>草稿和已发布客群都会显示；未来营销计划只允许选择已发布版本。</p></div><button @click="goCreate">＋ 创建客群</button></div>
          <div class="filters"><input placeholder="搜索客群名称（待实现）" disabled><select disabled><option>全部类型</option></select><select disabled><option>全部状态</option></select><button class="light" @click="loadList">刷新</button></div>
          <div class="table"><table><thead><tr><th>客群名称</th><th>类型</th><th>规则版本</th><th>最新批次</th><th>成员数</th><th>状态</th><th>计算时间</th><th>操作</th></tr></thead><tbody>
            <tr v-for="item in audiences" :key="item.audienceId"><td><button class="link" @click="openDetail(item)">{{item.name}}</button></td><td>{{item.type==='STATIC'?'静态':'动态'}}</td><td>V{{item.ruleVersion}}</td><td>R{{item.latestBatch}}</td><td>{{item.memberCount}}</td><td><span :class="['pill',{ok:item.published}]">{{item.published?'已发布':'草稿'}}</span></td><td>{{new Date(item.calculatedAt).toLocaleString('zh-CN')}}</td><td class="row-actions"><button class="light small" @click="openDetail(item)">查看</button><span class="future-action" title="修改与删除将在下一步接入业务规则">修改 · 删除</span></td></tr>
            <tr v-if="!audiences.length"><td colspan="8" class="empty">暂无客群，点击“创建客群”开始圈选</td></tr>
          </tbody></table></div>
        </section>
      </template>

      <template v-else-if="view==='create'">
        <div class="back" @click="goList">← 返回客群列表</div>
        <section><div class="section-title"><div><h2>基本信息与筛选条件</h2><p>预览不保存；正式保存时会按最新用户画像重新计算。</p></div><mark>新建</mark></div>
          <div class="grid"><label>客群名称<input v-model="form.name"></label><label>条件关系<select v-model="form.matchType"><option value="ALL">满足全部条件（AND）</option><option value="ANY">满足任一条件（OR）</option></select></label><label>客群类型<select v-model="form.type" @change="setType"><option value="STATIC">静态客群（保存后成员固定）</option><option value="DYNAMIC">动态客群（按计划更新成员）</option></select></label><label>更新周期<select v-model="form.refreshFrequency" :disabled="!dynamic"><option value="NONE">不自动更新</option><option value="DAILY">每天更新一次</option></select></label></div>
          <h3>标签条件</h3><div v-for="(condition,index) in form.conditions" :key="index" class="condition"><select v-model="condition.field"><option v-for="field in fields" :key="field[0]" :value="field[0]">{{field[1]}}</option></select><select v-model="condition.operator"><option value="EQUALS">等于</option><option value="NOT_EQUALS">不等于</option></select><input v-model="condition.value"><button class="light" @click="removeCondition(index)">删除</button></div>
          <div class="actions split"><button class="light" @click="addCondition">＋ 增加条件</button><span><button class="light" :disabled="loading" @click="preview">预览最新结果</button><button :disabled="loading" @click="save">重新计算并保存草稿</button></span></div>
          <div v-if="previewResult" class="preview-card"><strong>预览结果：{{previewResult.estimatedCount}} 人</strong><span>{{previewResult.memberIds.join('、') || '无匹配用户'}}</span></div>
        </section>
      </template>

      <template v-else-if="current">
        <div class="back" @click="goList">← 返回客群列表</div>
        <section><div class="section-title"><div><h2>{{current.name}}</h2><p class="mono">{{current.audienceId}}</p></div><span :class="['pill large',{ok:current.published}]">{{current.published?'已发布':'草稿'}}</span></div>
          <div class="detail-grid"><div><span>客群类型</span><strong>{{current.type==='STATIC'?'静态客群':'动态客群'}}</strong></div><div><span>规则版本</span><strong>V{{current.ruleVersion}}</strong></div><div><span>最新计算批次</span><strong>R{{current.latestBatch}}</strong></div><div><span>当前成员数</span><strong>{{current.memberCount}}</strong></div><div><span>更新周期</span><strong>{{current.refreshFrequency==='DAILY'?'每天一次':'不自动更新'}}</strong></div><div><span>最近计算时间</span><strong>{{new Date(current.calculatedAt).toLocaleString('zh-CN')}}</strong></div></div>
          <div class="actions"><button class="light" @click="loadMembers">查看成员</button><button v-if="!current.published" @click="publish">发布当前版本</button><button v-if="current.type==='DYNAMIC'" class="light" @click="refreshAudience">模拟动态 Task</button><span class="pending-note">修改规则与删除/停用：下一步实现</span></div>
          <div v-if="members.length" class="members"><h3>当前批次成员</h3><pre>{{JSON.stringify(members,null,2)}}</pre></div>
        </section>
      </template>
      </template>
    </main>
  </div>
</template>
