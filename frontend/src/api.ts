export type AudienceType = 'STATIC' | 'DYNAMIC'
export type RefreshFrequency = 'NONE' | 'DAILY'

export interface Condition { field: string; operator: 'EQUALS' | 'NOT_EQUALS'; value: string }
export interface AudienceCommand {
  name: string
  type: AudienceType
  refreshFrequency: RefreshFrequency
  matchType: 'ALL' | 'ANY'
  conditions: Condition[]
}
export interface AudienceView {
  audienceId: string
  name: string
  type: AudienceType
  refreshFrequency: RefreshFrequency
  ruleVersion: number
  matchType: 'ALL' | 'ANY'
  published: boolean
  latestBatch: number
  memberCount: number
  calculatedAt: string
}
export type NodeType = 'START'|'MARKETING'|'DECISION'|'PLACEHOLDER'|'DELAY'|'END'
export interface FlowNode { key:string; type:NodeType; name:string; config:string; x:number; y:number }
export interface FlowEdge { source:string; target:string; branchLabel:string|null }
export interface MarketingPlan { id:string; name:string; status:'DRAFT'|'PUBLISHED'; audienceId:string; audienceVersion:number; nodes:FlowNode[]; edges:FlowEdge[] }
export interface RunView { id:string; planId:string; audienceBatch:number; status:string; totalUsers:number; startedAt:string }
export interface Journey { runId:string; userId:string; status:string }
export interface Trace { id:number; nodeKey:string; nodeType:string; result:string; branchLabel:string|null; executedAt:string; resumeAt:string|null }

async function call<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, options)
  const contentType = response.headers.get('content-type') || ''
  const data: any = contentType.includes('application/json')
    ? await response.json()
    : await response.text()
  if (!response.ok) {
    const message = typeof data === 'object' && data?.message ? data.message : String(data || `HTTP ${response.status}`)
    throw new Error(message)
  }
  return data as T
}

const operatorHeaders = { 'Content-Type': 'application/json', 'X-Role': 'operator' }
export const api = {
  list: () => call<AudienceView[]>('/api/audiences'),
  preview: (body: AudienceCommand) => call<{estimatedCount: number; memberIds: string[]}>('/api/audiences/preview', { method: 'POST', headers: operatorHeaders, body: JSON.stringify(body) }),
  create: (body: AudienceCommand) => call<AudienceView>('/api/audiences', { method: 'POST', headers: operatorHeaders, body: JSON.stringify(body) }),
  publish: (item: AudienceView) => call<AudienceView>(`/api/audiences/${item.audienceId}/versions/${item.ruleVersion}/publish`, { method: 'POST', headers: { 'X-Role': 'operator' } }),
  refresh: (item: AudienceView) => call<AudienceView>(`/api/audiences/${item.audienceId}/refresh`, { method: 'POST', headers: { 'X-Role': 'operator' } }),
  members: (item: AudienceView) => call<unknown[]>(`/api/audiences/${item.audienceId}/versions/${item.ruleVersion}/members`),
  plans: () => call<MarketingPlan[]>('/api/marketing-plans'),
  createPlan: (body: Omit<MarketingPlan,'id'|'status'>) => call<MarketingPlan>('/api/marketing-plans',{method:'POST',headers:operatorHeaders,body:JSON.stringify(body)}),
  publishPlan: (id:string) => call<MarketingPlan>(`/api/marketing-plans/${id}/publish`,{method:'POST',headers:{'X-Role':'operator'}})
  ,startPlan: (id:string) => call<RunView>(`/api/marketing-plans/${id}/runs`,{method:'POST',headers:{'X-Role':'operator'}})
  ,runs: () => call<RunView[]>('/api/runs')
  ,journeys: (runId:string) => call<Journey[]>(`/api/runs/${runId}/journeys`)
  ,trace: (runId:string,userId:string) => call<Trace[]>(`/api/runs/${runId}/journeys/${userId}/trace`)
}
