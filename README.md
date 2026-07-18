# Zycus AI-Native Java Hackathon — AI Reassignment Engine

## Overview

An autonomous delivery reassignment system built during the Zycus hackathon. When a delivery agent goes offline, the system uses an LLM to reason about affected orders and queue reassignment suggestions for ops — without human trigger, and without auto-assigning.

## Architecture

- Java 17 + Spring Boot 3.4 backend
- React 18 + Vite frontend
- Strategy pattern for routing (rule-based + AI) with runtime switching
- Google Gemini as primary LLM, rule-based deterministic fallback
- Event-driven agentic loop via ApplicationEventPublisher + @Async
- H2 in-memory DB (Postgres-ready)

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 20+
- Free Gemini API key (aistudio.google.com)

## Quick Start (under 5 minutes)

### Backend

```bash
export LLM_API_KEY=your_gemini_key
mvn spring-boot:run
```

Backend starts at http://localhost:8080. Health: `/actuator/health`

### Frontend

```bash
cd hackathon-ui
npm install
npm run dev
```

UI at http://localhost:5173

## Demo the Agentic Loop (30 sec)

1. Open the UI
2. Click 'Set Offline' next to AGT-001
3. Watch — within 5-10 sec — orange 'AUTO RE-PLAN' badges appear for AGT-001's orders
4. Each shows AI-generated recovery reasoning
5. Click Accept or Reject to close the loop

## Key ADRs

See [docs/ADR.md](docs/ADR.md) for full architectural decisions. Highlights:

- Service layer pattern (ADR-004)
- Strategy pattern with bean registry for routing (ADR-006)
- LLM resilience chain with fallback (ADR-007)
- Async event-driven agentic loop with idempotency (ADR-008)

## Tech Stack

Spring Boot 3.4, Java 17, JPA, H2, React 18, Vite, Gemini 3.5 Flash, GitHub Actions ready.

## What's next

Sprint 2: Zone-aware routing (extension seam ready in `RoutingStrategy`), capacity constraints, SLA-driven proactive re-planning.
