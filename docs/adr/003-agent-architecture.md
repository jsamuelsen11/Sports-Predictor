# ADR-003: AI Betting Agent Architecture

**Status:** Accepted **Date:** 2026-04-05 **Decision Makers:** Project Owner

## Context

ADR-001 defined the MCP server as the "data backbone" for an AI agent. ADR-002 selected the tech
stack for the server. This ADR defines the architecture for the autonomous AI agent that connects to
the MCP server, analyzes betting opportunities, and places emulated bets.

## Decision

### 1. Gradle Multi-Module Mono-Repo

The agent lives in the same repository as the MCP server but is a separate Gradle subproject
(`agent/`) with its own Spring Boot application, dependencies, and configuration. This enables:

- Independent deployment and startup (`./gradlew :server:bootRun` vs `./gradlew :agent:bootRun`)
- Shared build infrastructure (Java toolchain, version catalog)
- No code coupling — communication is exclusively via MCP protocol

```
Sports-Predictor/
├── server/    # MCP server (Spring Boot, port 8080)
├── agent/     # AI agent (Spring Boot, port 8081)
└── gradle/    # Shared version catalog
```

### 2. Spring AI for Agent Implementation

The agent uses Spring AI's `ChatClient` with the Anthropic Claude model and `ToolCallbackProvider`
from the MCP client starter. This provides:

- **Auto-configured ChatModel** via `spring-ai-starter-model-anthropic`
- **MCP client** via `spring-ai-starter-mcp-client` connecting over SSE to the server
- **Automatic tool-calling loop** — ChatClient handles the multi-turn tool invocation cycle
- **ToolCallbackProvider** auto-discovers all 40+ MCP tools and converts schemas

### 3. Agent Architecture

```
AgentApplication
├── config/
│   ├── AgentProperties        # Strategy selection, sports, schedule crons
│   ├── ChatClientConfig       # ChatClient bean with MCP tools
│   └── SchedulingConfig       # @EnableScheduling
├── strategy/
│   ├── BettingStrategy        # Sealed interface (conservative/moderate/aggressive)
│   ├── StrategyFactory        # Bean factory selected by config
│   └── {Conservative,Moderate,Aggressive}Strategy
├── prompt/
│   └── AgentPrompts           # System/user prompts per workflow phase
└── workflow/
    ├── DailyCycleWorkflow      # @Scheduled orchestrator
    ├── SettlementWorkflow      # Auto-settle + CLV
    ├── ScanWorkflow            # Opportunity scanning
    ├── AnalyzeAndBetWorkflow   # Deep analysis + bet placement
    └── ReportWorkflow          # Performance reporting
```

### 4. Strategy as Sealed Interface

Betting strategies are Java records implementing a sealed interface. Each strategy defines risk
parameters (min confidence, Kelly fraction, max bets/day, etc.) and can format itself into LLM
system prompt rules via `toPromptRules()`. The active strategy is selected via the `agent.strategy`
config property.

| Parameter        | Conservative | Moderate | Aggressive |
| ---------------- | ------------ | -------- | ---------- |
| min_confidence   | 0.70         | 0.60     | 0.50       |
| min_edge_pct     | 5.0%         | 3.0%     | 1.5%       |
| kelly_fraction   | quarter      | half     | full       |
| max_bets_per_day | 2            | 5        | 10         |
| max_stake_pct    | 1%           | 2%       | 5%         |

### 5. Workflow Design

Each workflow phase is a **single ChatClient conversation**. Claude receives a system prompt with
strategy rules and a user prompt describing the task. Claude autonomously calls MCP tools (via the
tool-calling loop) and reasons about results. The strategy constrains Claude's decisions but does
not dictate them — Claude applies judgment to synthesize contradictory signals.

**Daily cycle:**

1. **Settle** (8 AM): Auto-settle completed bets, calculate CLV
2. **Scan + Bet** (10 AM): Rank opportunities, deep analysis, place bets
3. **Report** (10 PM): Generate daily performance summary

### 6. Stateless Agent

The agent stores no persistent state. All data (bets, bankroll, predictions, odds) lives in the MCP
server's SQLite database. The agent reads state via MCP tools at the start of each workflow. This
keeps the agent simple and avoids shared-database coupling.

### 7. MCP Connection

- **Transport:** SSE to `http://localhost:8080` (server's existing `/mcp/messages` endpoint)
- **Discovery:** `ToolCallbackProvider` auto-discovers all server tools at startup
- **Stateless:** Agent connects/disconnects without affecting server state

## Consequences

- **Positive:** Full Spring AI learning experience; clean separation of concerns; strategy is easily
  extensible (add new sealed interface implementations)
- **Positive:** Agent can be stopped/restarted without losing data
- **Negative:** Requires server to be running before agent starts
- **Negative:** SSE transport adds network overhead vs. direct service calls (acceptable for a hobby
  project; the latency bottleneck is the Anthropic API, not local SSE)
