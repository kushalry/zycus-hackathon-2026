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
