# ADR-002: Technology Stack & Implementation Plan

**Status:** Accepted
**Date:** 2026-03-21
**Decision Makers:** Project Owner

## Context

ADR-001 defines a comprehensive Sports Predictor MCP server with 72 tools, 18 resources, and 12 prompts across three priority phases. This ADR selects the complete technology stack, project structure, developer tooling, and implementation approach. The project is a greenfield Java learning project — decisions favor modern, interesting choices that maximize educational value. The scope covers the server itself, not deployment.

---

## Decisions

### 1. Java 21 (LTS)

Java 21 is the current LTS release with features that make this project more interesting:

- **Virtual threads** (Project Loom) for efficient concurrent external API calls
- **Record patterns** for clean DTOs and value objects
- **Pattern matching** for `switch` and `instanceof`
- **Sealed classes** for modeling bet types and status enums
- **Sequenced collections** for ordered data

Managed via **mise** (see section 10).

### 2. Build System: Gradle (Kotlin DSL)

Gradle 8.x with Kotlin DSL (`build.gradle.kts`).

**Why Gradle over Maven:**
- Kotlin DSL provides type-safe build scripts with IDE autocompletion
- Faster incremental builds, build cache, and parallel execution
- Spring Boot's Gradle plugin integrates seamlessly
- Version catalogs (`libs.versions.toml`) centralize all dependency versions

Note: Kotlin DSL is for build scripts only — all application code is Java.

**Version catalog** at `gradle/libs.versions.toml` centralizes every dependency and plugin version in one file, keeping `build.gradle.kts` clean.

### 3. MCP Server Framework: Spring Boot 3.5 + Spring AI

**Starter:** `spring-ai-starter-mcp-server-webmvc`
**Server type:** SYNC

Key configuration:

```yaml
spring:
  ai:
    mcp:
      server:
        name: sports-predictor
        version: 1.0.0
        type: SYNC
        instructions: "Sports prediction MCP server providing odds, stats, analysis, predictions, and emulated betting"
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: true
        sse-message-endpoint: /mcp/messages
        stdio: true
```

**Why WebMVC + STDIO:**
- SSE transport allows web-based MCP clients, multiple simultaneous clients, and easy debugging (curl/browser)
- `stdio: true` enables STDIO transport for Claude Desktop and other CLI-based MCP hosts
- SYNC type is simpler — virtual threads handle concurrency without reactive complexity
- WebMVC (not WebFlux) — servlet-based is easier to reason about and debug

**Annotation approach** — declarative model exclusively:
- `@McpTool` + `@McpToolParam` for all 72 tools
- `@McpResource` for all 18 resources
- `@McpPrompt` + `@McpPromptArg` for all 12 prompts

### 4. Database: SQLite + Flyway + Spring Data JPA

**Database:** SQLite via `org.xerial:sqlite-jdbc`
**Dialect:** `org.hibernate.community:hibernate-community-dialects` (SQLite dialect)
**Migrations:** Flyway with plain SQL files
**Data access:** Spring Data JPA

**Why SQLite:**
- Zero setup — no Docker, no server process, no connection management
- Data lives in a single file (`data/sports-predictor.db`) that's trivial to back up or reset
- Sufficient for a single-user hobby project
- Easy to migrate to PostgreSQL later if needed — JPA abstracts most differences

**SQLite adaptations:**
- No JSONB — use `TEXT` columns with Jackson serialization for flexible fields (bet metadata, odds snapshots, prediction details)
- No `TIMESTAMPTZ` — use `TEXT` with ISO-8601 strings (Hibernate handles conversion)
- Flyway migrations must use SQLite-compatible SQL syntax (no `ALTER COLUMN`, limited `ALTER TABLE`)
- DB file path configured via `spring.datasource.url=jdbc:sqlite:data/sports-predictor.db`

**Why Flyway:** Plain SQL migration files (e.g., `V1__create_bankroll.sql`) — simpler and more educational than Liquibase XML/YAML. SQL-first approach means learning real SQL, not a migration DSL.

**Why Spring Data JPA:** Standard Spring Boot data access with repository pattern, JPQL, native queries, and entity relationships that naturally model the bet/leg/bankroll domain.

### 5. HTTP Client: Spring RestClient

Spring 6.1's `RestClient` (synchronous, fluent API).

**Why RestClient:**
- Native to Spring Framework — no extra dependencies
- Fluent builder API is clean and readable
- Synchronous model pairs with SYNC MCP server and virtual threads
- Declarative `@HttpExchange` interfaces for clean API client definitions

One `@HttpExchange`-annotated interface per external API:

| Client | API | Free Tier |
|---|---|---|
| `OddsApiClient` | The Odds API | 500 req/month |
| `EspnApiClient` | ESPN Hidden API | Unlimited (unofficial) |
| `ApiSportsClient` | API-Sports | 100 req/day |
| `OpenMeteoClient` | Open-Meteo | Unlimited, no key |

### 6. Caching & Resilience

**Caffeine** (in-memory cache) for external API responses to stay within free-tier rate limits:

| Data | TTL | Rationale |
|---|---|---|
| Live odds | 5 minutes | Odds change frequently but not per-second |
| Schedules | 1 hour | Games don't get rescheduled often |
| Team/player stats | 6 hours | Stats update after games complete |
| Weather | 30 minutes | Forecasts update periodically |
| Standings | 1 hour | Change only after game results |

**Resilience4j** for external API protection:
- **Rate limiter** per client (respect The Odds API's 500 req/month, API-Sports' 100 req/day)
- **Circuit breaker** (when ESPN Hidden API goes down, fail fast instead of queuing)
- **Retry with backoff** (transient network failures)

### 7. Lombok

Use Lombok for reducing boilerplate:
- `@Data` / `@Getter` / `@Setter` on JPA entities
- `@Builder` for complex object construction (bets, predictions)
- `@RequiredArgsConstructor` for constructor injection in services
- `@Slf4j` for logging

Java 21 records still used for DTOs, API responses, and value objects where immutability is desired — Lombok and records complement each other.

### 8. Testing

| Layer | Tool | Purpose |
|---|---|---|
| Unit | JUnit 5 + Mockito | Service/tool logic in isolation |
| Integration | Spring Boot Test + `@SpringBootTest` | Full context with wired components |
| Database | `@DataJpaTest` with SQLite | Repository tests against real SQLite |
| External APIs | WireMock 3.x | Deterministic HTTP stubbing with recorded fixtures |
| Architecture | ArchUnit | Enforce package dependency rules |
| Coverage | JaCoCo | Coverage reports, 70% minimum threshold |

**ArchUnit** enforces rules like:
- Tools must not directly access repositories (go through services)
- Clients must not depend on entities
- No circular dependencies between packages

**WireMock fixtures** stored under `src/test/resources/wiremock/` with recorded API responses for deterministic, offline testing.

### 9. Code Quality

| Concern | Tool | Notes |
|---|---|---|
| Formatting | Spotless (Palantir Java Format) | Opinionated formatter, no debates. `task format` to apply |
| Static analysis | SpotBugs | Catches common bug patterns |
| Compile-time checks | Error Prone | Catches bugs at compile time |
| Style | Checkstyle (Google style) | Enforces consistent code style |
| Coverage gate | JaCoCo | Fail build below 70% line coverage |

### 10. Developer Tooling: mise + Taskfile + lefthook

#### mise (`.mise.toml`)

Manages tool versions — ensures every developer uses the same Java, Gradle, etc.

```toml
[tools]
java = "temurin-21"
gradle = "8.12"
"go:github.com/go-task/task/v3" = "3.40"
lefthook = "1.10"
```

#### Taskfile (`Taskfile.yml`)

Developer workflow commands:

| Task | Command | Purpose |
|---|---|---|
| `task build` | `./gradlew build` | Full build with tests |
| `task test` | `./gradlew test` | Run all tests |
| `task test:unit` | `./gradlew test --tests '*Unit*'` | Unit tests only |
| `task test:integration` | `./gradlew test --tests '*Integration*'` | Integration tests only |
| `task format` | `./gradlew spotlessApply` | Auto-format code |
| `task format:check` | `./gradlew spotlessCheck` | Check formatting (no changes) |
| `task lint` | `./gradlew checkstyleMain spotbugsMain` | Run linters |
| `task coverage` | `./gradlew jacocoTestReport` | Generate coverage report |
| `task run` | `./gradlew bootRun` | Start MCP server (WebMVC mode) |
| `task run:stdio` | `./gradlew bootRun --args='--spring.ai.mcp.server.stdio=true'` | Start in STDIO mode |
| `task db:migrate` | `./gradlew flywayMigrate` | Run database migrations |
| `task db:clean` | `./gradlew flywayClean` | Reset database |
| `task clean` | `./gradlew clean` | Clean build artifacts |
| `task check` | format:check + lint + test + coverage | Full quality gate |

#### lefthook (`.lefthook.yml`)

Git hooks for quality gates:

```yaml
pre-commit:
  parallel: true
  commands:
    format-check:
      run: ./gradlew spotlessCheck
      fail_text: "Code not formatted. Run 'task format'"
    compile:
      run: ./gradlew compileJava compileTestJava
      fail_text: "Compilation failed"

pre-push:
  parallel: false
  commands:
    full-check:
      run: ./gradlew check
      fail_text: "Quality checks failed. Run 'task check'"
```

---

## Key Libraries

| Library | Version | Purpose |
|---|---|---|
| Spring Boot | 3.5.x | Application framework |
| Spring AI | 1.1.x | MCP server annotations and auto-config |
| Spring Data JPA | (via Boot) | Database access |
| Hibernate 6 | (via Boot) | JPA implementation |
| Hibernate Community Dialects | 7.x | SQLite dialect for Hibernate |
| SQLite JDBC | 3.x | SQLite driver |
| Flyway | (via Boot) | Schema migrations |
| Jackson | (via Boot) | JSON serialization |
| Lombok | 1.18.x | Boilerplate reduction |
| Caffeine | 3.x | In-memory caching |
| Resilience4j | 2.x | Rate limiting, circuit breakers, retries |
| JUnit 5 | (via Boot) | Testing framework |
| Mockito | (via Boot) | Mocking |
| WireMock | 3.x | HTTP API stubbing |
| ArchUnit | 1.3.x | Architecture rule enforcement |
| Spotless | (Gradle plugin) | Code formatting |
| SpotBugs | (Gradle plugin) | Static analysis |
| Error Prone | (Gradle plugin) | Compile-time bug detection |
| Checkstyle | (Gradle plugin) | Style enforcement |
| JaCoCo | (Gradle plugin) | Coverage reporting |

---

## Project Structure

```
sports-predictor/
+-- .mise.toml
+-- .lefthook.yml
+-- .gitignore
+-- Taskfile.yml
+-- build.gradle.kts
+-- settings.gradle.kts
+-- gradle/
|   +-- libs.versions.toml
|   +-- wrapper/
+-- data/                               # SQLite DB file (gitignored)
+-- docs/
|   +-- adr/
|       +-- 001-mcp-server-feature-definition.md
|       +-- 002-tech-stack-and-implementation-plan.md
+-- src/
    +-- main/
    |   +-- java/com/sportspredictor/
    |   |   +-- SportsPredictorApplication.java
    |   |   |
    |   |   +-- tool/
    |   |   |   +-- ingestion/          # get_live_odds, get_game_schedule, get_team_stats, etc.
    |   |   |   +-- analysis/           # compare_matchup, analyze_trends, calculate_ev, etc.
    |   |   |   +-- prediction/         # generate_prediction, rank_todays_plays
    |   |   |   +-- betting/            # place_bet, place_parlay, settle_bet, etc.
    |   |   |   +-- bankroll/           # get_bankroll_status, deposit_funds, etc.
    |   |   |   +-- odds/               # compare_odds_across_books, find_value_bets
    |   |   |   +-- history/            # get_bet_history, get_bankroll_analytics
    |   |   |   +-- sport/              # get_mlb_pitching_matchup, sport-specific tools
    |   |   |   +-- notification/       # Alerts, monitoring (Phase 2+)
    |   |   |
    |   |   +-- resource/              # @McpResource classes
    |   |   |   +-- SportsResource.java
    |   |   |   +-- BankrollResource.java
    |   |   |
    |   |   +-- prompt/                # @McpPrompt classes
    |   |   |   +-- PicksPrompt.java
    |   |   |   +-- AnalysisPrompt.java
    |   |   |   +-- BankrollPrompt.java
    |   |   |
    |   |   +-- service/               # Business logic
    |   |   |   +-- odds/
    |   |   |   +-- stats/
    |   |   |   +-- prediction/
    |   |   |   +-- betting/
    |   |   |   +-- bankroll/
    |   |   |   +-- analytics/
    |   |   |
    |   |   +-- client/                # External API clients
    |   |   |   +-- oddsapi/            # The Odds API
    |   |   |   +-- espn/               # ESPN Hidden API
    |   |   |   +-- apisports/          # API-Sports
    |   |   |   +-- openmeteo/          # Open-Meteo weather
    |   |   |
    |   |   +-- entity/                # JPA entities
    |   |   +-- repository/            # Spring Data JPA repositories
    |   |   +-- dto/                   # Java records for API responses & tool I/O
    |   |   +-- config/                # Spring @Configuration classes
    |   |   +-- exception/             # Custom exceptions
    |   |   +-- util/                  # Odds conversion, math helpers
    |   |
    |   +-- resources/
    |       +-- application.yml
    |       +-- application-dev.yml
    |       +-- db/migration/          # Flyway SQL files
    |           +-- V1__create_bankroll_tables.sql
    |           +-- V2__create_bet_tables.sql
    |           +-- V3__create_odds_cache_tables.sql
    |           +-- V4__create_prediction_tables.sql
    |
    +-- test/
        +-- java/com/sportspredictor/
        |   +-- tool/                  # Tool unit tests
        |   +-- service/               # Service unit tests
        |   +-- client/                # WireMock API client tests
        |   +-- repository/            # @DataJpaTest tests
        |   +-- integration/           # Full @SpringBootTest tests
        |   +-- architecture/          # ArchUnit rules
        +-- resources/
            +-- application-test.yml
            +-- wiremock/              # Recorded API response fixtures
                +-- odds-api/
                +-- espn/
                +-- api-sports/
                +-- open-meteo/
```

**Architecture rules** (enforced by ArchUnit):
- `tool.*` depends on `service.*` only (never `repository.*` or `client.*` directly)
- `service.*` depends on `client.*` and `repository.*`
- `client.*` is self-contained (no dependencies on `entity.*` or `service.*`)
- No circular dependencies

---

## Entity Model

### bankroll

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| name | TEXT | e.g., "Season 2026" |
| starting_balance | REAL | Initial deposit amount |
| current_balance | REAL | Updated on every transaction |
| created_at | TEXT | ISO-8601 timestamp |
| archived_at | TEXT | Null if active; set when reset/archived |

### bet

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| bankroll_id | TEXT | FK to bankroll |
| bet_type | TEXT | MONEYLINE, SPREAD, TOTAL, PARLAY, PLAYER_PROP, GAME_PROP, TEASER, ROUND_ROBIN, FUTURES, LIVE, SGP, FIRST_HALF |
| status | TEXT | PENDING, WON, LOST, PUSHED, CANCELLED, VOID |
| stake | REAL | Amount wagered |
| odds | REAL | American odds at placement |
| potential_payout | REAL | Calculated at placement |
| actual_payout | REAL | Null until settled |
| sport | TEXT | NFL, NBA, MLB, NHL, SOCCER, etc. |
| event_id | TEXT | External event identifier |
| description | TEXT | Human-readable bet description |
| placed_at | TEXT | ISO-8601 timestamp |
| settled_at | TEXT | Null until settled |
| metadata | TEXT | JSON string — spread value, total line, prop details, teaser points, etc. |

### bet_leg

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| bet_id | TEXT | FK to bet |
| leg_number | INTEGER | Order within parlay/teaser/SGP |
| selection | TEXT | e.g., "Chiefs -3.5", "LeBron Over 25.5 pts" |
| odds | REAL | Individual leg odds |
| status | TEXT | PENDING, WON, LOST, PUSHED, VOID |
| event_id | TEXT | Event for this specific leg |
| sport | TEXT | Sport for this leg |
| result_detail | TEXT | JSON string — actual score, stat line, etc. |

### transaction

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| bankroll_id | TEXT | FK to bankroll |
| type | TEXT | DEPOSIT, WITHDRAWAL, BET_PLACED, BET_WON, BET_LOST, BET_PUSH, BET_CANCELLED |
| amount | REAL | Positive or negative |
| balance_after | REAL | Running balance after this transaction |
| reference_bet_id | TEXT | FK to bet (nullable — null for deposits/withdrawals) |
| created_at | TEXT | ISO-8601 timestamp |

### odds_snapshot

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| event_id | TEXT | External event identifier |
| sport | TEXT | Sport code |
| bookmaker | TEXT | Bookmaker name |
| market | TEXT | h2h, spreads, totals, etc. |
| odds_data | TEXT | JSON string — full odds structure |
| captured_at | TEXT | ISO-8601 timestamp |

### prediction_log

| Column | Type | Notes |
|---|---|---|
| id | TEXT (UUID) | Primary key |
| event_id | TEXT | External event identifier |
| sport | TEXT | Sport code |
| prediction_type | TEXT | SPREAD, MONEYLINE, TOTAL, PROP |
| predicted_outcome | TEXT | JSON string — winner, spread, total, confidence breakdown |
| confidence | REAL | 0.0 to 1.0 |
| actual_outcome | TEXT | JSON string — null until game completes |
| key_factors | TEXT | JSON string — factors that drove the prediction |
| created_at | TEXT | ISO-8601 timestamp |
| settled_at | TEXT | Null until verified against results |

---

## Implementation Phases

These phases align with ADR-001's MVP / Phase 2 / Phase 3. Each phase is broken into implementation milestones.

### Phase 0: Project Scaffolding

Before any features — get the build running and the MCP server responding.

1. Initialize Gradle project: `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`
2. Configure Spring Boot 3.5 + Spring AI MCP WebMVC starter
3. Set up `.mise.toml`, `Taskfile.yml`, `.lefthook.yml`, `.gitignore`
4. Configure code quality plugins (Spotless, Checkstyle, SpotBugs, JaCoCo)
5. Set up SQLite datasource and Flyway
6. Create initial Flyway migrations (bankroll + bet + transaction tables)
7. Configure `application.yml` and `application-dev.yml`
8. Create ArchUnit test with initial architecture rules
9. Verify bare server starts and responds to MCP initialization
10. Create a single "hello world" `@McpTool` to confirm the annotation pipeline works end-to-end

### MVP: 34 Tools, 10 Resources, 6 Prompts

**Milestone MVP-1: External API Clients**
- `OddsApiClient` — The Odds API (odds, historical odds)
- `EspnApiClient` — ESPN Hidden API (scores, schedules, stats, injuries, standings)
- `OpenMeteoClient` — Open-Meteo (weather forecasts)
- Configure Caffeine caching per client with appropriate TTLs
- Configure Resilience4j rate limiting per client
- WireMock tests for all clients with recorded response fixtures

**Milestone MVP-2: Data Ingestion Tools (11 tools)**
- `get_live_odds`, `get_game_schedule`, `get_team_stats`, `get_player_stats`
- `get_injury_report`, `get_weather_forecast`, `get_game_results`
- `get_historical_odds`, `get_historical_team_stats`, `get_historical_player_stats`
- `get_head_to_head_history`
- Sports resources: `sports://leagues`, `sports://{sport}/standings`, `sports://{sport}/schedule/today`, `sports://{sport}/schedule/week`, `sports://{sport}/teams`

**Milestone MVP-3: Core Analysis & Prediction (11 tools)**
- Analysis: `lookup_team_record`, `compare_matchup`, `analyze_trends`, `calculate_implied_probability`, `calculate_expected_value`
- Prediction: `generate_prediction`, `rank_todays_plays`, `get_prediction_accuracy`
- Odds: `compare_odds_across_books`, `find_value_bets`
- Sport-specific: `get_mlb_pitching_matchup`
- Prompts: `daily_picks`, `game_breakdown`, `injury_impact`, `value_scan`, `matchup_history`, `bankroll_review`

**Milestone MVP-4: Emulated Betting System (12 tools)**
- Database entities: `Bankroll`, `Bet`, `BetLeg`, `Transaction`
- Bet types: Moneyline, Spread, Totals, Parlay, Player Props
- Placement: `place_bet`, `place_parlay`, `cancel_bet`
- Settlement: `settle_bet`, `auto_settle_bets`, `settle_parlay`
- Bankroll: `get_bankroll_status`, `reset_bankroll`, `deposit_funds`, `withdraw_funds`, `calculate_kelly_stake`
- History: `get_bet_history`, `get_bet_slip`, `get_bankroll_analytics`, `get_daily_performance`
- Resources: `bankroll://status`, `bankroll://pending_bets`, `bankroll://today`, `bankroll://history/summary`

### Phase 2: 28 Tools, 8 Resources, 4 Prompts

- `ApiSportsClient` integration for structured stats
- Advanced analysis: rest/travel, pace/totals, situational stats, power rankings
- Advanced prediction: props, parlays, explain prediction
- Additional bet types: Game Props, Teasers, Round Robin, Futures, First Half
- Bankroll rules engine + daily limits
- Line movement detection, arbitrage finder, market consensus
- Notification/alert system (subscriptions + delivery)
- Backtesting engine
- Performance analytics: ROI by category, streak history, parlay performance, profit graph data, season summary, export
- Gamification: achievements, streaks
- Additional resources and prompts per ADR-001

### Phase 3: 10 Tools, 0 Resources, 2 Prompts

- Monte Carlo simulation engine
- Correlation analysis for same-game parlays
- Live/in-game betting simulation
- Same-game parlay support
- CLV tracking and reporting
- Odds monitoring (polling + snapshots over time)
- Niche sports expansion: MMA/UFC, Golf (PGA), Tennis
- `compare_to_random` benchmark tool
- Prompts: `season_outlook`, `model_audit`

---

## Consequences

**Positive:**
- Java 21 + Spring Boot 3.5 + Spring AI is a modern, cutting-edge learning stack
- Gradle Kotlin DSL + version catalog is the current standard for Java builds
- SQLite is zero-friction — no Docker, no server, single file for all data
- WebMVC + STDIO dual transport supports both web and CLI MCP clients
- Annotation-based MCP approach (`@McpTool`, `@McpResource`, `@McpPrompt`) is clean and declarative
- Caffeine + Resilience4j protect against free-tier API rate limits
- mise + Taskfile + lefthook provide a professional developer experience from day one
- ArchUnit enforces clean architecture as the project grows
- Lombok + Java 21 records together minimize boilerplate while keeping code clear

**Negative:**
- SQLite lacks some features useful for analytics (no window functions before 3.25, no JSONB, limited `ALTER TABLE`)
- Spring AI MCP is relatively new — documentation may be sparse for edge cases
- 72 tools is a large surface area — discipline needed to keep tool implementations thin (delegate to services)
- Gradle has a steeper initial learning curve than Maven

**Risks:**
- Spring AI MCP annotations API may change between versions (mitigate: pin version, follow release notes)
- ESPN Hidden API is unofficial and could break without notice (mitigate: circuit breaker, fallback to API-Sports for overlapping data)
- Free-tier rate limits constrain development velocity (mitigate: aggressive caching, WireMock fixtures for development)
- SQLite may become a bottleneck if the project grows significantly (mitigate: JPA abstraction makes PostgreSQL migration straightforward)
