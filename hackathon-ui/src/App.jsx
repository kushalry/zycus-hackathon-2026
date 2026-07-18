import { useState, useEffect, useCallback } from 'react'

const API = 'http://localhost:8080'

/* ─── tiny keyframe injection ─── */
const STYLES = `
@keyframes pulse-orange {
  0%   { background-color: #f97316; box-shadow: 0 0 0 0 rgba(249,115,22,0.6); }
  50%  { background-color: #ea580c; box-shadow: 0 0 0 6px rgba(249,115,22,0); }
  100% { background-color: #f97316; box-shadow: 0 0 0 0 rgba(249,115,22,0); }
}
.pulse-badge {
  animation: pulse-orange 1.4s ease-in-out infinite;
}
`

function injectStyles() {
  if (!document.getElementById('ops-styles')) {
    const tag = document.createElement('style')
    tag.id = 'ops-styles'
    tag.textContent = STYLES
    document.head.appendChild(tag)
  }
}

/* ─── helpers ─── */
function StatusPill({ status }) {
  const map = {
    AVAILABLE: { bg: '#dcfce7', color: '#15803d', label: 'Available' },
    BUSY:      { bg: '#fef9c3', color: '#854d0e', label: 'Busy' },
    OFFLINE:   { bg: '#fee2e2', color: '#b91c1c', label: 'Offline' },
  }
  const s = map[status] ?? { bg: '#f3f4f6', color: '#374151', label: status }
  return (
    <span style={{
      backgroundColor: s.bg,
      color: s.color,
      borderRadius: 999,
      padding: '2px 10px',
      fontSize: 12,
      fontWeight: 600,
      letterSpacing: 0.3,
    }}>
      {s.label}
    </span>
  )
}

function ConfidenceBar({ value }) {
  const pct = Math.round((value ?? 0) * 100)
  const color = pct >= 70 ? '#16a34a' : pct >= 40 ? '#d97706' : '#dc2626'
  return (
    <div style={{ marginTop: 6 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, color: '#6b7280', marginBottom: 3 }}>
        <span>Confidence</span>
        <span style={{ fontWeight: 600, color }}>{pct}%</span>
      </div>
      <div style={{ height: 7, borderRadius: 999, background: '#e5e7eb', overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', borderRadius: 999, background: color, transition: 'width 0.4s ease' }} />
      </div>
    </div>
  )
}

function TriggerBadge({ reason }) {
  if (reason === 'AGENT_OFFLINE') {
    return (
      <span className="pulse-badge" style={{
        display: 'inline-block',
        color: '#fff',
        borderRadius: 6,
        padding: '3px 10px',
        fontSize: 11,
        fontWeight: 700,
        letterSpacing: 0.5,
        textTransform: 'uppercase',
      }}>
        Auto Re-plan · Agent Offline
      </span>
    )
  }
  return (
    <span style={{
      display: 'inline-block',
      background: '#e5e7eb',
      color: '#6b7280',
      borderRadius: 6,
      padding: '3px 10px',
      fontSize: 11,
      fontWeight: 600,
      letterSpacing: 0.5,
      textTransform: 'uppercase',
    }}>
      Initial Assignment
    </span>
  )
}

/* ─── Agent Card ─── */
function AgentCard({ agent, onStatusChange }) {
  const [patching, setPatching] = useState(null)

  async function setStatus(status) {
    setPatching(status)
    try {
      await fetch(`${API}/agents/${agent.id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      onStatusChange()
    } catch (e) {
      console.error(e)
    } finally {
      setPatching(null)
    }
  }

  const btnBase = {
    padding: '5px 11px',
    fontSize: 12,
    fontWeight: 600,
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    transition: 'opacity 0.15s',
  }

  return (
    <div style={{
      background: '#fff',
      borderRadius: 10,
      padding: '14px 16px',
      marginBottom: 12,
      boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
      border: '1px solid #f0f0f0',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 6 }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#111827' }}>{agent.name}</div>
          <div style={{ fontSize: 11, color: '#9ca3af', marginTop: 2 }}>ID: {agent.id}</div>
        </div>
        <StatusPill status={agent.status} />
      </div>
      <div style={{ fontSize: 13, color: '#6b7280', marginBottom: 10 }}>
        Active orders: <strong style={{ color: '#374151' }}>{agent.activeOrderCount ?? 0}</strong>
      </div>
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        <button
          style={{ ...btnBase, background: '#dcfce7', color: '#15803d', opacity: patching ? 0.6 : 1 }}
          disabled={!!patching}
          onClick={() => setStatus('AVAILABLE')}
        >
          {patching === 'AVAILABLE' ? '…' : 'Set Available'}
        </button>
        <button
          style={{ ...btnBase, background: '#fef9c3', color: '#854d0e', opacity: patching ? 0.6 : 1 }}
          disabled={!!patching}
          onClick={() => setStatus('BUSY')}
        >
          {patching === 'BUSY' ? '…' : 'Set Busy'}
        </button>
        <button
          style={{ ...btnBase, background: '#fee2e2', color: '#b91c1c', opacity: patching ? 0.6 : 1 }}
          disabled={!!patching}
          onClick={() => setStatus('OFFLINE')}
        >
          {patching === 'OFFLINE' ? '…' : 'Set Offline'}
        </button>
      </div>
    </div>
  )
}

/* ─── Suggestion Card ─── */
function SuggestionCard({ suggestion, onAction }) {
  const [acting, setActing] = useState(null)

  async function act(status) {
    setActing(status)
    try {
      await fetch(`${API}/suggestions/${suggestion.id}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status }),
      })
      onAction()
    } catch (e) {
      console.error(e)
    } finally {
      setActing(null)
    }
  }

  const btnBase = {
    flex: 1,
    padding: '8px 0',
    fontSize: 13,
    fontWeight: 700,
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'opacity 0.15s',
  }

  return (
    <div style={{
      background: '#fff',
      borderRadius: 10,
      padding: '16px 18px',
      marginBottom: 14,
      boxShadow: '0 1px 5px rgba(0,0,0,0.09)',
      border: '1px solid #f0f0f0',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
        <div style={{ fontWeight: 800, fontSize: 15, color: '#111827' }}>
          Order #{suggestion.orderId}
        </div>
        <TriggerBadge reason={suggestion.triggerReason} />
      </div>

      <div style={{ fontSize: 13, color: '#6b7280', marginBottom: 4 }}>
        Recommended agent: <strong style={{ color: '#374151' }}>{suggestion.recommendedAgentId}</strong>
      </div>

      <ConfidenceBar value={suggestion.confidence} />

      {suggestion.reasoning && (
        <p style={{
          fontSize: 13,
          color: '#6b7280',
          fontStyle: 'italic',
          margin: '10px 0 12px',
          lineHeight: 1.55,
          borderLeft: '3px solid #e5e7eb',
          paddingLeft: 10,
        }}>
          {suggestion.reasoning}
        </p>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <button
          style={{ ...btnBase, background: '#16a34a', color: '#fff', opacity: acting ? 0.6 : 1 }}
          disabled={!!acting}
          onClick={() => act('ACCEPTED')}
        >
          {acting === 'ACCEPTED' ? 'Accepting…' : '✓ Accept'}
        </button>
        <button
          style={{ ...btnBase, background: '#dc2626', color: '#fff', opacity: acting ? 0.6 : 1 }}
          disabled={!!acting}
          onClick={() => act('REJECTED')}
        >
          {acting === 'REJECTED' ? 'Rejecting…' : '✗ Reject'}
        </button>
      </div>
    </div>
  )
}

/* ─── Section wrapper ─── */
function Section({ title, badge, children }) {
  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        marginBottom: 14, paddingBottom: 10,
        borderBottom: '2px solid #f3f4f6',
      }}>
        <h2 style={{ margin: 0, fontSize: 16, fontWeight: 700, color: '#111827' }}>{title}</h2>
        {badge != null && (
          <span style={{
            background: '#f3f4f6', color: '#374151',
            borderRadius: 999, padding: '1px 9px',
            fontSize: 12, fontWeight: 600,
          }}>
            {badge}
          </span>
        )}
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>{children}</div>
    </div>
  )
}

/* ─── Root App ─── */
export default function App() {
  injectStyles()

  const [agents, setAgents] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [agentsLoading, setAgentsLoading] = useState(true)
  const [suggestionsLoading, setSuggestionsLoading] = useState(true)
  const [agentsError, setAgentsError] = useState(null)
  const [suggestionsError, setSuggestionsError] = useState(null)
  const [lastRefresh, setLastRefresh] = useState(null)

  const fetchAgents = useCallback(async () => {
    try {
      const res = await fetch(`${API}/agents`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      setAgents(data)
      setAgentsError(null)
    } catch (e) {
      setAgentsError(e.message)
    } finally {
      setAgentsLoading(false)
    }
  }, [])

  const fetchSuggestions = useCallback(async () => {
    try {
      const res = await fetch(`${API}/suggestions?status=PENDING`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      setSuggestions(data)
      setSuggestionsError(null)
    } catch (e) {
      setSuggestionsError(e.message)
    } finally {
      setSuggestionsLoading(false)
    }
  }, [])

  const refresh = useCallback(() => {
    fetchAgents()
    fetchSuggestions()
    setLastRefresh(new Date())
  }, [fetchAgents, fetchSuggestions])

  useEffect(() => {
    refresh()
    const id = setInterval(refresh, 5000)
    return () => clearInterval(id)
  }, [refresh])

  const fmt = (d) => d ? d.toLocaleTimeString() : '—'

  return (
    <div style={{
      minHeight: '100vh',
      background: '#f8fafc',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
      color: '#111827',
    }}>
      {/* ── Header ── */}
      <header style={{
        background: '#fff',
        borderBottom: '1px solid #e5e7eb',
        padding: '0 28px',
        height: 60,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        position: 'sticky',
        top: 0,
        zIndex: 10,
        boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 20 }}>🤖</span>
          <h1 style={{ margin: 0, fontSize: 18, fontWeight: 800, color: '#111827' }}>
            Ops Dashboard <span style={{ color: '#6b7280', fontWeight: 400 }}>— AI Reassignment Engine</span>
          </h1>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ fontSize: 12, color: '#9ca3af' }}>
            Last refresh: {fmt(lastRefresh)}
          </span>
          <button
            onClick={refresh}
            style={{
              background: '#2563eb',
              color: '#fff',
              border: 'none',
              borderRadius: 8,
              padding: '7px 18px',
              fontSize: 13,
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6,
            }}
          >
            ↻ Refresh
          </button>
        </div>
      </header>

      {/* ── Columns ── */}
      <div style={{
        display: 'flex',
        gap: 0,
        padding: '24px 28px',
        maxWidth: 1400,
        margin: '0 auto',
        height: 'calc(100vh - 60px)',
        boxSizing: 'border-box',
      }}>

        {/* Left: Agents */}
        <div style={{ width: '40%', paddingRight: 20, overflowY: 'auto' }}>
          <Section title="Agents" badge={agents.length}>
            {agentsLoading ? (
              <p style={{ color: '#9ca3af', fontSize: 14 }}>Loading…</p>
            ) : agentsError ? (
              <p style={{ color: '#dc2626', fontSize: 14 }}>Error: {agentsError}</p>
            ) : agents.length === 0 ? (
              <p style={{ color: '#9ca3af', fontSize: 14 }}>No agents found.</p>
            ) : (
              agents.map(agent => (
                <AgentCard key={agent.id} agent={agent} onStatusChange={refresh} />
              ))
            )}
          </Section>
        </div>

        {/* Divider */}
        <div style={{ width: 1, background: '#e5e7eb', flexShrink: 0 }} />

        {/* Right: Suggestions */}
        <div style={{ flex: 1, paddingLeft: 20, overflowY: 'auto' }}>
          <Section title="Pending Suggestions" badge={suggestions.length}>
            {suggestionsLoading ? (
              <p style={{ color: '#9ca3af', fontSize: 14 }}>Loading…</p>
            ) : suggestionsError ? (
              <p style={{ color: '#dc2626', fontSize: 14 }}>Error: {suggestionsError}</p>
            ) : suggestions.length === 0 ? (
              <div style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center',
                justifyContent: 'center', padding: '60px 0', color: '#9ca3af',
              }}>
                <div style={{ fontSize: 40, marginBottom: 12 }}>✅</div>
                <div style={{ fontSize: 15, fontWeight: 600 }}>No pending suggestions</div>
                <div style={{ fontSize: 13, marginTop: 4 }}>All caught up!</div>
              </div>
            ) : (
              suggestions.map(s => (
                <SuggestionCard key={s.id} suggestion={s} onAction={refresh} />
              ))
            )}
          </Section>
        </div>
      </div>
    </div>
  )
}
