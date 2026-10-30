<script setup lang="ts">
import { computed } from 'vue'
import type { FlowEdge, FlowNode, NodeType } from './api'
const props=defineProps<{node:FlowNode;nodes:FlowNode[];edges:FlowEdge[]}>()
const emit=defineEmits<{add:[{source:string,type:NodeType,branchLabel:string|null,decisionKind?:'AB'|'LABEL'|'RECEIPT'}]}>()
const outgoing=computed(()=>props.edges.filter(e=>e.source===props.node.key))
const nextNode=computed(()=>{const e=outgoing.value.find(e=>!e.branchLabel);return e?props.nodes.find(n=>n.key===e.target):undefined})
const decisionLabels=computed(()=>props.node.config.startsWith('AB:')?['A','B']:props.node.config.startsWith('LABEL:')?['YES','NO']:['SUCCESS','FAILURE'])
function target(label:string){const e=outgoing.value.find(e=>e.branchLabel===label);return e?props.nodes.find(n=>n.key===e.target):undefined}
function add(type:NodeType,label:string|null=null,decisionKind?:'AB'|'LABEL'|'RECEIPT'){emit('add',{source:props.node.key,type,branchLabel:label,decisionKind})}
const abA=computed({get:()=>Number(props.node.config.split(':')[1]||50),set:v=>props.node.config=`AB:${v}:${100-v}`})
const abB=computed(()=>100-abA.value)
const labelRule=computed({get:()=>props.node.config.replace('LABEL:',''),set:v=>props.node.config=`LABEL:${v}`})
function pathTitle(label:string){if(props.node.config.startsWith('AB:'))return `${label} ${label==='A'?abA.value:abB.value}%`;if(props.node.config.startsWith('LABEL:'))return label==='YES'?'满足条件':'不满足条件';return label==='SUCCESS'?'触达成功':'触达失败'}
</script>

<template>
  <div class="branch-flow">
    <div class="flow-node" :class="node.type.toLowerCase()"><small>{{node.type}}</small><strong>{{node.name}}</strong>
      <select v-if="node.type==='MARKETING'" v-model="node.config"><option value="CALL">打电话</option><option value="SMS">发短信</option><option value="COUPON">发券</option></select>
      <label v-else-if="node.type==='DELAY'">等待分钟数<input v-model="node.config" type="number" min="1"></label>
      <template v-else-if="node.type==='DECISION'&&node.config.startsWith('AB:')"><label>A路径比例<input v-model.number="abA" type="number" min="0" max="100"></label><label>B路径比例<input :value="abB" disabled></label></template>
      <label v-else-if="node.type==='DECISION'&&node.config.startsWith('LABEL:')">标签条件<input v-model="labelRule"></label>
      <span v-else-if="node.type==='DECISION'">根据上一营销节点回执判断</span>
      <span v-else-if="node.type==='PLACEHOLDER'">立即通过，保留统计记录</span>
    </div>
    <div v-if="node.type==='DECISION'" class="branch-tree"><div v-for="label in decisionLabels" :key="label" class="branch-column"><span class="branch-label">{{pathTitle(label)}}</span><i></i><FlowBranch v-if="target(label)" :node="target(label)!" :nodes="nodes" :edges="edges" @add="emit('add',$event)"/><div v-else class="branch-add"><b>＋ 添加路径首节点</b><button class="light small" @click="add('MARKETING',label)">营销</button><button class="light small" @click="add('PLACEHOLDER',label)">空白</button><button class="light small" @click="add('DELAY',label)">延迟</button></div></div></div>
    <template v-else-if="node.type!=='END'"><div class="single-connector">↓</div><FlowBranch v-if="nextNode" :node="nextNode" :nodes="nodes" :edges="edges" @add="emit('add',$event)"/><div v-else class="next-add"><b>＋ 添加下一节点</b><button class="light small" @click="add('MARKETING')">营销</button><button class="light small" @click="add('PLACEHOLDER')">空白</button><button class="light small" @click="add('DELAY')">延迟</button><button class="light small" @click="add('DECISION',null,'AB')">A/B判断</button><button class="light small" @click="add('DECISION',null,'LABEL')">标签判断</button><button v-if="node.type==='MARKETING'" class="light small" @click="add('DECISION',null,'RECEIPT')">回执判断</button><button class="light small" @click="add('END')">结束</button></div></template>
  </div>
</template>
