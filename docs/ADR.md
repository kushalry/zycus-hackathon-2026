# Architecture Decision Record

Living document — updated as decisions are made.

## ADR-001: Technology Stack

**Context:** Zycus AI-Native Java Hackathon, 5 hours, solo, need to build 
LLM-integrated Java backend + frontend.

**Options considered:**
1. Java 17 + Spring Boot 3.4 + React 18 (Vite) + H2 → PostgreSQL
2. Java 17 + Spring Boot 3.4 + Angular 17 + PostgreSQL
3. Java 21 + Spring Boot 3.4 + minimal setup

**Decision:** Option 1 — Java 17, Spring Boot 3.4, React 18 with Vite, 
H2 baseline with Postgres fallback config.

**Consequences:** 
- Fastest iteration with React + Vite HMR
- H2 zero-setup, but Postgres wired ready for production
- Java 17 matches production experience — no version-fighting

**Would revisit if:** need horizontal scaling or graph traversal (Postgres preferred over H2 there).

## ADR-002: LLM Provider Strategy

**Context:** Need reliable LLM access for 5 hours of sustained testing 
and demo. Rate limits and provider outages are real risks.

**Options considered:**
1. Single provider (Gemini) — simpler, one point of failure
2. Primary + fallback (Gemini → Groq) — complexity but resilience
3. Local model (Ollama) — no rate limits but slower + local resources

**Decision:** Option 2. Gemini 3.5 Flash primary (reliable JSON mode + 
free tier) with Groq Llama 3.1 as automatic fallback (OpenAI-compatible 
API, fast inference).

**Consequences:**
- Gateway abstraction is more complex but system stays live if either fails
- Two provider keys to manage
- Explicit failure handling required (which we need anyway)

**Would revisit if:** paid tier gives reliable single-provider SLA.

## ADR-003: JPA Entity to Table Mapping

**Context:** Seed script from Addendum A uses plural table names 
(agents, orders) and snake_case columns. Hibernate's default naming 
singularizes class names (Agent → agent), causing "Table AGENTS not 
found" on seed execution.

**Options considered:**
1. Rewrite seed to match Hibernate defaults (singular names)
2. Use @Table and @Column on entities to match seed contract
3. Configure custom PhysicalNamingStrategy globally to pluralize

**Decision:** Option 2. Added explicit @Table("agents"), @Table("orders"), 
@Table("reassignment_suggestions"). Snake_case columns kept via 
@Column for fields like current_load and assigned_agent_id.

**Consequences:**
- Explicit annotations trade slight verbosity for clarity — no hidden 
  naming convention magic
- Seed script from Addendum A stays as provided (contract respected)
- Any future entity additions must remember @Table (small cost)

**Would revisit if:** switching to a persistence layer that doesn't 
respect JPA @Table annotations, or if seed script is regenerated.

## ADR-004: Service Layer Pattern

**Context:** Controllers were initially calling JPA repositories directly. 
As the routing engine, LLM integration, event publishing, and idempotency 
logic are added, this would concentrate too many concerns in the 
controller layer — a well-known design smell.

**Options considered:**
1. Keep repositories in controllers — simplest, but doesn't scale as 
   business logic grows
2. Rich domain model — put behavior on entity classes
3. Service Layer — dedicated services for business logic, transactional 
   boundaries, orchestration

**Decision:** Option 3. Introduced OrderService and AgentService. Controllers 
become thin — only HTTP parsing, validation, status code selection. Services 
own business logic and @Transactional boundaries.

**Consequences:**
- Routing logic (T-2) will live in a dedicated RoutingService, called from 
  services — not from controllers
- Async event publishing (T-4) will fire from AgentService, keeping the 
  controller off the request-path complexity
- Explicit transactional boundaries at service methods, not accidentally 
  at HTTP layer
- Slight verbosity cost for clear separation of concerns

**Would revisit if:** the application stays trivially CRUD forever — but 
given routing, LLM, and agentic loop coming, this pattern earns its keep.

## ADR-005: HTTP Error Semantics

**Context:** Initial exception handling conflated "resource not found" 
with "invalid input" by using IllegalArgumentException for both. Both 
returned 400, which is semantically wrong for missing resources.

**Options considered:**
1. Special-case each not-found endpoint in the controller
2. Return 400 for everything — simplest but incorrect REST semantics
3. Introduce a NotFoundException hierarchy with dedicated handler

**Decision:** Option 3. Added NotFoundException that maps to 404 via 
GlobalExceptionHandler. IllegalArgumentException stays at 400 for 
validation-style errors.

**Consequences:**
- Correct REST semantics — clients can distinguish missing vs malformed
- One-line exception addition when a new not-found path emerges
- Scaling to more error categories (Conflict, Forbidden) follows the 
  same pattern

**Would revisit if:** we needed richer problem-details format (RFC 7807 
ProblemDetail), which Spring Boot 3 supports natively — worth an 
upgrade path.

## ADR-006: Routing Engine — Strategy Pattern with Bean Registry

**Context:** T-2 requires a routing engine that (a) supports multiple 
strategies today (rule-based + AI), (b) allows runtime switching via config, 
and (c) can accept new strategies (Sprint 2's ZoneAffinityStrategy) without 
touching existing code.

**Options considered:**
1. If/else in a single service — simplest, but violates open/closed and 
   creates a design smell
2. Manual factory — explicit control but requires editing on each new strategy
3. Spring @Qualifier per injection point — fragile, verbose
4. Strategy interface + auto-wired Map<String, RoutingStrategy> keyed by 
   named bean — extensibility without touching existing code

**Decision:** Option 4. RoutingStrategy interface with two @Component 
implementations (bean name = strategy name via @Component('rule-based') and 
@Component('ai')). RoutingService injects Map<String, RoutingStrategy> — 
Spring populates it automatically with every RoutingStrategy bean. Active 
strategy chosen via routing.strategy config property.

**Consequences:**
- Adding ZoneAffinityStrategy = create one @Component('zone-affinity') class 
  implementing RoutingStrategy. Zero changes to RoutingService, RoutingContext, 
  or any existing strategy.
- Runtime switching = change routing.strategy value + restart (Spring Boot 
  config refresh could make this hot-swap without restart in later sprint)
- Both HTTP endpoint and async event handler go through the same RoutingService 
  — no strategy duplication between call sites
- Fallback path exists: if the configured strategy fails or isn't found, 
  log ERROR and fall back to rule-based

**Would revisit if:** we needed strategies that share complex state (currently 
each is stateless), or if we needed strategy composition (e.g., filter chain 
patterns).

**Hot-swap consideration:**  
Runtime switching via config in this build requires a restart because 
@Value is resolved at startup. True zero-restart hot-swap could be added 
via Spring Cloud's @RefreshScope on RoutingService, or by exposing a 
POST /admin/routing/strategy endpoint that mutates activeStrategyName. 
Neither adds complexity to strategy implementations — the design point 
holds.

## ADR-007: LLM Resilience — Fallback Chain and Validation

**Context:** T-3 requires explicit handling of LLM failure modes:
- Timeouts / network errors / 503s
- Malformed JSON responses (LLM ignores JSON schema instruction)
- Hallucinated agent IDs (LLM invents an ID not in the database)

A silent drop in the async re-plan path (T-4) would be worse than a 
rule-based suggestion.

**Options considered:**
1. Throw exceptions upward, let caller decide
2. Return Optional<AgentRecommendation> and let RoutingService orchestrate 
   fallback
3. AiRoutingStrategy handles fallback internally, always returns a 
   non-empty list when agents are available

**Decision:** Option 3. AiRoutingStrategy owns the full resilience chain 
and delegates to RuleBasedRoutingStrategy on any failure. This keeps 
callers (HTTP endpoint AND async event handler in T-4) simple — they 
always get a usable recommendation when agents exist.

Failure modes handled:
- **LLM call fails (network, timeout, 503):** catch exception, log WARN, 
  fall back to rule-based
- **Response unparseable:** LLMResponseParser strips markdown fences, 
  extracts JSON substring, returns Optional.empty() on any parse failure. 
  AiRoutingStrategy falls back to rule-based.
- **Hallucinated agent ID (not in DB):** existsById() check, log WARN, 
  fall back to rule-based.
- **LLM picked ineligible agent (e.g., BUSY when only AVAILABLE agents 
  were provided):** check against the availableAgents list, log WARN, 
  fall back to rule-based.

**Consequences:**
- AiRoutingStrategy becomes the resilience boundary; callers don't need to 
  handle LLM failures
- Every fallback is logged, so ops can see LLM health via log volume
- The rule-based suggestion is always available if agents exist — no 
  silent drops
- Testing: can force each failure mode by breaking config (base-url), 
  malforming a mock response, or seeding an unknown agent ID

**Would revisit if:** we need per-failure-mode retry policies, circuit 
breakers to stop hammering a dead LLM, or observability metrics beyond 
logs.

### Critical-path note:
The synchronous /suggest endpoint blocks on the LLM (2-5 sec typical). 
The client explicitly requested the routing decision, so this is 
acceptable UX. The critical-path constraint from the brief applies to 
the agentic loop (T-4), where PATCH /agents/status returns immediately 
and re-planning happens on an async thread pool. LLM slowness there 
cannot degrade user-facing operations because the endpoint has already 
returned 200 by the time re-planning starts.

## ADR-008: Agentic Loop — Async Event-Driven Re-planning

**Context:** T-4 requires that PATCH /agents/{id}/status returns 
immediately while re-planning happens autonomously in the background. 
Additionally, the same agent flipping OFFLINE twice quickly must not 
create duplicate suggestions (idempotency).

**Options considered:**
1. Synchronous re-planning inside the PATCH handler — simple but 
   violates the "off request path" constraint
2. Scheduled poller checking for OFFLINE agents every N seconds — 
   would fire because a timer ticked, not because state changed
3. Spring's ApplicationEventPublisher + @EventListener + @Async — 
   fires because state changed, decoupled from HTTP, uses a dedicated 
   thread pool

**Decision:** Option 3. AgentService publishes AgentWentOfflineEvent 
after the status update commits. AgentOfflineEventHandler listens 
@Async on a dedicated ThreadPoolTaskExecutor (4 core, 8 max, 50 queue). 
The HTTP endpoint returns in ~50ms; async processing takes 3-10 sec 
depending on LLM response time.

**Idempotency:** Before creating a new AGENT_OFFLINE suggestion for an 
order, check `existsByOrderIdAndTriggerReasonAndStatus`. If a PENDING 
AGENT_OFFLINE suggestion already exists, skip. This handles the 
"same agent flipping offline twice" case cleanly at the database level.

**Failure handling:** If routing fails during async re-plan (LLM down + 
rule-based somehow throws), we log ERROR but do not crash the handler 
— the next event triggers a fresh attempt. Silent drops of individual 
orders are logged as WARN so ops can see them.

**Consequences:**
- PATCH endpoint response time is bounded (no LLM in the request path)
- Duplicate PATCH calls have no duplicative effect (idempotency)
- Adding a new trigger (SLA breach in Sprint 3) means firing a new 
  event type — the handler pattern is reusable
- Failure of async re-planning doesn't affect the HTTP call that 
  triggered it, but IS surfaced via logs for observability

**Would revisit if:** we need durable event delivery guarantees 
(currently events are in-JVM only — a JVM restart mid-processing 
loses in-flight events). Kafka or an outbox pattern would harden this.

## ADR-009: UI Framework — React 18 with Vite

**Context:** T-5 requires an ops interface built in React 18 or Angular 17. 
The candidate's production experience is Flutter (Dart) for mobile with 
some foundational HTML/CSS/JavaScript from a 2022 internship, no recent 
React or Angular production work.

**Options considered:**
1. React 18 with Vite — mental model closest to Flutter widgets, largest 
   ecosystem, fastest scaffolding, Cursor Agent Mode support strong
2. Angular 17 with standalone components — more opinionated framework, 
   TypeScript-heavy, standalone API is a meaningful shift from older Angular
3. Server-rendered Thymeleaf UI — simpler but goes against brief's 
   React/Angular constraint

**Decision:** Option 1 — React 18 with Vite. Native fetch API for HTTP, 
useState + useEffect for state and polling. Single-file App.jsx for 
simplicity in a 5-hour build. Inline styles (no external UI library) 
to keep the surface area small.

**Consequences:**
- Fast scaffold (5 min from `npm create vite` to running dev server)
- Component mental model maps to Flutter widgets I know well
- 5-second polling implemented via useEffect with setInterval
- No TypeScript — trade type safety for velocity in a 5-hour build
- Inline styles trade design system polish for simplicity — "usable, 
  not beautiful" is exactly what the brief asked for

**Scope call — partial ceiling delivered:**
Agent load visualization was added as a small ceiling reach — each agent card shows a horizontal capacity bar (assumed capacity 5). Color-coded: green for 0-1 orders, amber for 2-3, red for 4-5. Cost: ~10 minutes. Value: makes agent capacity visible at a glance without adding backend complexity.

The other ceiling items — full dispatch board across all statuses, SLA countdown with color coding, zone-aware roster — were consciously left unbuilt. The brief itself says "a clean functional floor beats an ambitious ceiling that cost you something in the backend." I prioritized routing engine quality (T-2, including hot-swap admin endpoint), AI resilience across all failure modes (T-3), and the async agentic loop with idempotency (T-4) over UI depth.

In a follow-up sprint, extensions are straightforward:
- Full dispatch board — a new component reusing the existing fetch pattern against GET /orders (already implemented)
- SLA countdown — needs seed data with slaDeadline populated (nullable field already exists on Order entity), then a JavaScript countdown timer per order card
- Zone roster — needs zone seeding on agents (nullable field already exists), then grouping in the UI

All three extensions plug into the existing schema. No refactoring required.

**Would revisit if:** ops teams needed a mission-control view of all 
active orders + SLA countdown for a real deployment. The dispatch-board 
extension is a straightforward add — same fetch pattern, richer table 
component.

## ADR-010: Deliberate Exclusions and Follow-up Roadmap

**Context:** In a 5-hour build, some things get consciously deferred. This entry names what and why, to make priority choices explicit rather than implicit.

**Explicit exclusions:**

1. **SSE streaming for LLM responses** (T-3 bonus, +5 pts). Would stream Gemini's reasoning token-by-token via SseEmitter and Gemini's streamGenerateContent endpoint. Cost: ~60 minutes with real risk of parsing chunked JSON envelopes. Deferred because the async agentic loop with idempotency (T-4) and full LLM resilience (T-3) were correctness requirements, and SSE is an enhancement. In a follow-up sprint I'd add POST /orders/{id}/suggest/stream with SseEmitter and React EventSource.
2. **Full UI ceiling** (T-5, up to +8 pts). Partial ceiling delivered — agent load bars per card. The remaining items — full dispatch board, SLA countdown color coding, zone-aware roster — were consciously left. Rationale: the brief itself says "a clean functional floor beats an ambitious ceiling that cost you something in the backend." Extensions are straightforward: dispatch board reuses the fetch pattern; SLA countdown uses the already-nullable slaDeadline field on Order; zone roster uses the already-nullable zone field on Agent.
3. **Admin endpoint authentication.** Hot-swap and future admin endpoints are unguarded. In production I would gate them behind Spring Security with role-based access — routing strategy changes are ops-level actions, not open to the world.
4. **Order state machine enforcement.** SuggestionService enforces PENDING → ACCEPTED or REJECTED. Order status transitions are currently direct writes in AgentOfflineEventHandler. In a follow-up I would centralize order transitions in OrderService with the same validation pattern — reject invalid transitions like DELIVERED → ASSIGNED.
5. **Durable event delivery.** ApplicationEventPublisher is in-JVM. A JVM restart mid-processing loses in-flight events. Kafka or an outbox pattern would harden this for production.

**Decision framework applied:** For each deferred item, I asked "is this a correctness requirement or an enhancement?" Enhancements got deferred. Correctness requirements (idempotency, fallback chain, event decoupling) were built end-to-end.

**Would revisit any of these if:** the system needed to scale beyond ops-team internal usage (auth); handle 100s of concurrent agent events (Kafka); or if walkthrough scoring showed the UI ceiling was more valuable than the design depth I chose to invest in.
