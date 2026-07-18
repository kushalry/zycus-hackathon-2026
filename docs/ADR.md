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
