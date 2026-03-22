# ADR-001: Sports Predictor MCP Server - Feature Definition

**Status:** Accepted **Date:** 2026-03-21 **Decision Makers:** Project Owner

## Context

This project pairs an AI agent with an MCP (Model Context Protocol) server for sports betting
predictions. It is a hobby project for fun and learning — no real gambling. The MCP server is the
data backbone, exposing tools, resources, and prompts that an AI agent calls to gather data, run
analysis, and produce predictions.

## Decision

### Data Sources (Free/Low-Cost)

| Source                       | Provides                                             | Cost              |
| ---------------------------- | ---------------------------------------------------- | ----------------- |
| The Odds API                 | Live odds from 40+ bookmakers, line history          | Free: 500 req/mo  |
| ESPN Hidden API              | Scores, schedules, rosters, standings, stats         | Free, unofficial  |
| Sportsipy / sports-reference | Deep historical stats                                | Scraping or API   |
| API-Sports                   | Structured stats for NFL/NBA/MLB/NHL/soccer          | Free: 100 req/day |
| Open-Meteo                   | Weather forecasts + historical weather               | Free, no key      |
| Local DB                     | Bankroll, bet history, odds snapshots, model results | Free              |

---

## Feature Inventory

### 1. Data Ingestion Tools

| Feature                       | Description                                                          | Type | Priority |
| ----------------------------- | -------------------------------------------------------------------- | ---- | -------- |
| `get_live_odds`               | Fetch current odds for a sport/event/market from multiple bookmakers | Tool | MVP      |
| `get_game_schedule`           | Upcoming games with date, time, teams, venue, broadcast              | Tool | MVP      |
| `get_team_stats`              | Season/per-game stats (off/def ratings, ATS record, O/U record)      | Tool | MVP      |
| `get_player_stats`            | Player season averages, recent game logs, career numbers             | Tool | MVP      |
| `get_injury_report`           | Current injuries with status (out/doubtful/questionable/probable)    | Tool | MVP      |
| `get_weather_forecast`        | Weather conditions for venue at game time (outdoor sports)           | Tool | MVP      |
| `get_game_results`            | Final scores and box scores for completed games                      | Tool | MVP      |
| `get_historical_odds`         | Closing odds for completed games (for backtesting/CLV)               | Tool | MVP      |
| `get_historical_team_stats`   | Season-by-season team stats for past N years                         | Tool | MVP      |
| `get_historical_player_stats` | Career game logs and season-by-season player stats                   | Tool | MVP      |
| `get_head_to_head_history`    | Historical matchup results between two teams (last N meetings)       | Tool | MVP      |
| `get_historical_standings`    | Past season final standings and playoff results                      | Tool | Phase 2  |
| `get_betting_splits`          | Public betting percentages (ticket + handle %)                       | Tool | Phase 2  |
| `get_venue_info`              | Venue metadata: location, surface, dome/outdoor, elevation           | Tool | Phase 2  |
| `get_referee_officials`       | Assigned officials for upcoming games                                | Tool | Phase 3  |
| `get_news_feed`               | Aggregated sports news for a team/player                             | Tool | Phase 3  |

### 2. Core Analysis Tools

| Feature                         | Description                                                                  | Type | Priority |
| ------------------------------- | ---------------------------------------------------------------------------- | ---- | -------- |
| `lookup_team_record`            | W/L with splits: home/away, ATS, O/U, conference, last N games               | Tool | MVP      |
| `compare_matchup`               | Side-by-side stat comparison for two teams                                   | Tool | MVP      |
| `analyze_trends`                | Performance trends over a window (last N games, splits)                      | Tool | MVP      |
| `calculate_implied_probability` | Convert odds formats, calculate vig/juice                                    | Tool | MVP      |
| `calculate_expected_value`      | EV calculation + Kelly Criterion sizing given predicted probability and odds | Tool | MVP      |
| `analyze_rest_and_travel`       | Rest days, travel distance, timezone changes, back-to-backs                  | Tool | Phase 2  |
| `analyze_pace_and_totals`       | Projected game pace and total based on matchup                               | Tool | Phase 2  |
| `analyze_situational_stats`     | Performance as favorite/underdog, indoor/outdoor, after loss, etc.           | Tool | Phase 2  |
| `run_power_rankings`            | Quantitative power rankings (SRS, Elo, net rating)                           | Tool | Phase 2  |
| `simulate_game_monte_carlo`     | Monte Carlo simulation with score distributions and confidence intervals     | Tool | Phase 3  |
| `analyze_correlations`          | Estimate correlation between parlay legs for SGP analysis                    | Tool | Phase 3  |

### 3. Prediction Engine

| Feature                             | Description                                                                      | Type | Priority |
| ----------------------------------- | -------------------------------------------------------------------------------- | ---- | -------- |
| `generate_prediction`               | Full prediction: winner, spread, total, confidence, key factors, recommended bet | Tool | MVP      |
| `rank_todays_plays`                 | Scan all games, rank by EV — the "daily card"                                    | Tool | MVP      |
| `generate_prop_prediction`          | Player prop predictions using matchup, pace, minutes projection                  | Tool | Phase 2  |
| `generate_parlay_analysis`          | Evaluate parlay legs, assess correlation, calculate true combined probability    | Tool | Phase 2  |
| `explain_prediction`                | Human-readable narrative of why a prediction was made                            | Tool | Phase 2  |
| `get_prediction_confidence_factors` | Breakdown of what increases/decreases prediction confidence                      | Tool | Phase 3  |

### 4. Emulated Gambling System

Full simulated sportsbook experience — no real money. Track bets, winnings, losses, and performance
over time with all standard bet types.

#### 4a. Supported Bet Types

All bet types below are stored in the database with their specific structure and payout logic:

| Bet Type                         | Description                                                                   | Payout Logic                                                                      | Priority |
| -------------------------------- | ----------------------------------------------------------------------------- | --------------------------------------------------------------------------------- | -------- |
| **Moneyline**                    | Pick the outright winner                                                      | American odds conversion (e.g., -150 pays $66.67 on $100, +150 pays $150 on $100) | MVP      |
| **Point Spread**                 | Team must win/lose by a margin (e.g., Chiefs -3.5)                            | Standard -110 unless otherwise specified; push rules apply on whole numbers       | MVP      |
| **Totals (Over/Under)**          | Combined score over or under a number (e.g., Over 48.5)                       | Standard -110 unless otherwise specified                                          | MVP      |
| **Parlay**                       | Multiple legs combined into one bet; all must hit to win                      | Multiply decimal odds of each leg; one loss kills the parlay                      | MVP      |
| **Player Props**                 | Bet on individual player stats (e.g., LeBron Over 25.5 pts)                   | Odds vary per prop market                                                         | MVP      |
| **Game Props**                   | Bet on game events (e.g., first team to score, will there be OT)              | Odds vary per prop market                                                         | Phase 2  |
| **Teaser**                       | Parlay with adjusted spreads/totals in bettor's favor (e.g., 6-pt NFL teaser) | Reduced payout vs standard parlay; all legs must hit                              | Phase 2  |
| **Round Robin**                  | All possible parlay combinations from a set of selections                     | Each sub-parlay settled independently; total P/L aggregated                       | Phase 2  |
| **Futures**                      | Bet on season outcomes (e.g., Super Bowl winner, MVP)                         | Long-term bets settled at season end; odds locked at placement                    | Phase 2  |
| **Live/In-Game**                 | Bets placed after game starts with shifting odds                              | Odds captured at moment of placement; auto-settle on final result                 | Phase 3  |
| **Same-Game Parlay (SGP)**       | Multiple bets from the same game combined                                     | Correlated legs — adjusted payout accounting for correlation                      | Phase 3  |
| **First Half / First 5 Innings** | Bets on partial game outcomes                                                 | Settled on halftime/5th-inning score, not final                                   | Phase 2  |

#### 4b. Bet Placement & Management Tools

| Feature        | Description                                                                                                                                    | Type | Priority |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ---- | -------- |
| `place_bet`    | Place a simulated bet on any supported type. Validates odds, stake vs bankroll, and stores bet with locked-in odds, timestamp, and bet slip ID | Tool | MVP      |
| `place_parlay` | Place a multi-leg parlay bet. Validates each leg, calculates combined odds and potential payout, stores all legs linked to a single bet slip   | Tool | MVP      |
| `cancel_bet`   | Cancel a pending bet before its game starts. Refunds stake to bankroll                                                                         | Tool | MVP      |
| `edit_bet`     | Modify stake or remove/add legs on a pending bet (pre-game only)                                                                               | Tool | Phase 2  |
| `cash_out_bet` | Early cash-out on a pending bet at reduced value based on current odds/game state                                                              | Tool | Phase 3  |

#### 4c. Bet Settlement Tools

| Feature            | Description                                                                                                                    | Type | Priority |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------ | ---- | -------- |
| `settle_bet`       | Settle a single bet as won/lost/pushed using actual game results. Calculates exact payout and updates bankroll                 | Tool | MVP      |
| `auto_settle_bets` | Batch-settle all pending bets whose games have completed. Fetches results, applies payout logic per bet type, updates bankroll | Tool | MVP      |
| `settle_parlay`    | Settle a parlay by evaluating each leg. One loss = parlay lost; all wins = full payout; push legs reduce the parlay            | Tool | MVP      |
| `void_leg`         | Void a single leg of a parlay (e.g., game cancelled). Recalculates parlay odds with remaining legs                             | Tool | Phase 2  |
| `settle_futures`   | Settle futures bets at end of season/tournament based on final outcomes                                                        | Tool | Phase 2  |

#### 4d. Bankroll Management Tools

| Feature                   | Description                                                                                                                              | Type | Priority |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- | ---- | -------- |
| `get_bankroll_status`     | Current balance, starting balance, total wagered, total won, total lost, net P/L, ROI %, current win/loss streak, number of pending bets | Tool | MVP      |
| `reset_bankroll`          | Reset bankroll to a starting amount. Archives current history as a "season" for comparison                                               | Tool | MVP      |
| `deposit_funds`           | Add simulated funds to the bankroll (top-up)                                                                                             | Tool | MVP      |
| `withdraw_funds`          | Remove simulated funds from the bankroll (take profit)                                                                                   | Tool | MVP      |
| `calculate_kelly_stake`   | Optimal bet size via Kelly Criterion (full/half/quarter) given predicted probability and offered odds                                    | Tool | MVP      |
| `set_bankroll_rules`      | Configure: max single bet (units), max daily exposure, stop-loss threshold, max parlay legs, min confidence for auto-bets, unit size     | Tool | Phase 2  |
| `get_daily_limits_status` | Check remaining daily wagering capacity against configured limits                                                                        | Tool | Phase 2  |

#### 4e. Bet History & Performance Analytics Tools

| Feature                         | Description                                                                                                                                    | Type | Priority |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ---- | -------- |
| `get_bet_history`               | Retrieve bet history with filters: date range, sport, bet type, result (W/L/P), min/max odds, min/max stake                                    | Tool | MVP      |
| `get_bet_slip`                  | Get full details of a single bet: all legs (for parlays), odds at placement, result, payout, timestamps                                        | Tool | MVP      |
| `get_bankroll_analytics`        | Comprehensive stats: profit by sport, by bet type, by day of week, by month; average odds, win rate, CLV, max drawdown, variance, Sharpe ratio | Tool | MVP      |
| `get_streak_history`            | Win/loss streak analysis over time — longest streaks, current streak, streak distribution                                                      | Tool | Phase 2  |
| `get_roi_by_category`           | ROI breakdown: by sport, by bet type (ML/spread/total/parlay/prop), by confidence level, by unit size                                          | Tool | Phase 2  |
| `get_parlay_performance`        | Parlay-specific analytics: hit rate by number of legs, average payout, most profitable leg count, best/worst parlays                           | Tool | Phase 2  |
| `get_profit_graph_data`         | Time-series data of bankroll balance over time for charting (daily/weekly/monthly granularity)                                                 | Tool | Phase 2  |
| `get_closing_line_value_report` | Compare odds at bet placement vs closing odds across all settled bets — positive CLV = skilled bettor signal                                   | Tool | Phase 3  |
| `export_bet_history`            | Export full bet history with all metadata to CSV or JSON for external analysis                                                                 | Tool | Phase 2  |
| `get_season_summary`            | End-of-season (or any date range) summary: total bets, W/L/P record, net units, best bet, worst bet, by sport/type                             | Tool | Phase 2  |

#### 4f. Leaderboard & Gamification Tools

| Feature                 | Description                                                                                  | Type | Priority |
| ----------------------- | -------------------------------------------------------------------------------------------- | ---- | -------- |
| `get_achievements`      | Track milestones: first winning parlay, 10-game win streak, $1000 profit mark, etc.          | Tool | Phase 2  |
| `get_daily_performance` | Today's betting card: bets placed, pending, settled, daily P/L                               | Tool | MVP      |
| `compare_to_random`     | Compare system performance vs random betting and vs always-favorite strategies as benchmarks | Tool | Phase 3  |

### 5. Odds & Value Detection

| Feature                        | Description                                                       | Type | Priority |
| ------------------------------ | ----------------------------------------------------------------- | ---- | -------- |
| `compare_odds_across_books`    | Side-by-side odds from all bookmakers, highlight best line        | Tool | MVP      |
| `find_value_bets`              | Identify bets where predicted edge exceeds threshold              | Tool | MVP      |
| `detect_line_movement`         | Track line movement since open, identify sharp vs public moves    | Tool | Phase 2  |
| `find_arbitrage_opportunities` | Scan for guaranteed-profit arb opportunities across books         | Tool | Phase 2  |
| `get_market_consensus`         | Average/median odds across bookmakers as "true" probability proxy | Tool | Phase 2  |
| `calculate_closing_line_value` | Compare bet odds against closing line (best long-term predictor)  | Tool | Phase 3  |
| `monitor_odds_for_event`       | Poll and store odds snapshots over time for an event              | Tool | Phase 3  |

### 6. Sports Coverage

**MVP:** NFL, NBA, MLB, NHL, Soccer (EPL, La Liga, Bundesliga, Serie A, MLS, UCL) **Phase 2:**
College Football (NCAAF), College Basketball (NCAAB) **Phase 3:** MMA/UFC, Golf (PGA), Tennis

Sport-specific tools:

| Feature                       | Description                              | Priority |
| ----------------------------- | ---------------------------------------- | -------- |
| `get_mlb_pitching_matchup`    | Starting pitcher comparison with splits  | MVP      |
| `get_nfl_dvoa_ratings`        | Efficiency ratings adjusted for opponent | Phase 2  |
| `get_nhl_goalie_confirmation` | Confirm starting goaltender              | Phase 2  |
| `get_soccer_form_table`       | Last-N-games form table                  | Phase 2  |
| `get_cfb_sp_plus_ratings`     | Composite team ratings                   | Phase 2  |
| `get_cbb_kenpom_ratings`      | Adjusted efficiency metrics              | Phase 2  |
| `get_nba_lineup_impact`       | Net rating with/without specific lineups | Phase 3  |

### 7. Notifications & Monitoring

| Feature                    | Description                                                              | Type | Priority |
| -------------------------- | ------------------------------------------------------------------------ | ---- | -------- |
| `get_daily_briefing`       | Day's slate summary: games, matchups, injury changes, pending bets       | Tool | Phase 2  |
| `subscribe_line_movement`  | Alert when a line moves past threshold                                   | Tool | Phase 2  |
| `subscribe_injury_updates` | Alert on injury status changes for tracked teams/players                 | Tool | Phase 2  |
| `get_alerts`               | Retrieve all pending alerts (line moves, injuries, results, settlements) | Tool | Phase 2  |
| `subscribe_game_start`     | Alert before game start for last-minute checks                           | Tool | Phase 3  |

### 8. Historical Analysis & Backtesting

| Feature                           | Description                                                        | Type | Priority |
| --------------------------------- | ------------------------------------------------------------------ | ---- | -------- |
| `get_prediction_accuracy`         | Track record: win rate by sport, type, confidence level, P/L       | Tool | MVP      |
| `get_model_performance_by_factor` | Accuracy breakdown by sport, home/away, fav/dog, confidence bucket | Tool | Phase 2  |
| `backtest_strategy`               | Apply rules to historical data, return simulated P/L and stats     | Tool | Phase 2  |
| `export_prediction_log`           | Export all predictions + outcomes to CSV/JSON                      | Tool | Phase 2  |
| `compare_to_closing_line`         | Aggregate CLV analysis across all historical predictions           | Tool | Phase 3  |

---

## MCP Resources

| URI                                   | Description                                                  | Priority |
| ------------------------------------- | ------------------------------------------------------------ | -------- |
| `sports://leagues`                    | All supported leagues with metadata and season status        | MVP      |
| `sports://{sport}/standings`          | Current league standings                                     | MVP      |
| `sports://{sport}/schedule/today`     | Today's games with odds snapshot                             | MVP      |
| `sports://{sport}/schedule/week`      | This week's full schedule                                    | MVP      |
| `sports://{sport}/teams`              | All teams with metadata and venue info                       | MVP      |
| `bankroll://status`                   | Current balance, P/L, ROI, streak, wagering limits remaining | MVP      |
| `bankroll://pending_bets`             | All unsettled bets with legs, odds, potential payout         | MVP      |
| `bankroll://today`                    | Today's betting card: placed, pending, settled, daily P/L    | MVP      |
| `bankroll://history/summary`          | Rolling summary: last 7/30/90 day W/L record, net units, ROI | MVP      |
| `bankroll://rules`                    | Current bankroll rules (max bet, daily limits, stop-loss)    | Phase 2  |
| `bankroll://achievements`             | Unlocked milestones and progress toward next ones            | Phase 2  |
| `bankroll://seasons`                  | List of archived bankroll "seasons" with final stats         | Phase 2  |
| `sports://{sport}/team/{id}/roster`   | Team roster with player status                               | Phase 2  |
| `predictions://today`                 | Cached predictions for today's games                         | Phase 2  |
| `config://model_settings`             | Model configuration and operating parameters                 | Phase 2  |
| `sports://{sport}/team/{id}/schedule` | Team's full season schedule with results                     | Phase 2  |
| `sports://venues`                     | Venue reference data (location, surface, dome, elevation)    | Phase 2  |

---

## MCP Prompts

Reusable prompt templates the agent (or user) can invoke with parameters. Spring AI MCP supports
these as first-class primitives.

| Prompt               | Description                                                       | Parameters                                                 | Priority |
| -------------------- | ----------------------------------------------------------------- | ---------------------------------------------------------- | -------- |
| `daily_picks`        | Generate a full analysis of today's best bets across all sports   | `sport` (optional), `min_confidence` (1-10), `max_picks`   | MVP      |
| `game_breakdown`     | Deep-dive analysis of a specific matchup with prediction          | `game_id` or `team1` + `team2`, `sport`                    | MVP      |
| `injury_impact`      | Assess how a specific injury affects a game's line and prediction | `player_name`, `team`, `game_id`                           | MVP      |
| `value_scan`         | Find the best value bets across all sports right now              | `sport` (optional), `min_ev_percent`, `bankroll_pct`       | MVP      |
| `bankroll_review`    | Comprehensive review of betting performance and bankroll health   | `time_period` (last week/month/season), `sport` (optional) | MVP      |
| `matchup_history`    | Analyze the historical rivalry and trends between two teams       | `team1`, `team2`, `sport`, `num_games`                     | MVP      |
| `weather_impact`     | Assess weather impact on an outdoor game's totals and spread      | `game_id`, `sport`                                         | Phase 2  |
| `sharp_money_report` | Analyze line movement and betting splits to detect sharp action   | `sport`, `date`                                            | Phase 2  |
| `prop_builder`       | Build a player prop bet card for a specific game                  | `game_id`, `sport`, `num_props`                            | Phase 2  |
| `parlay_optimizer`   | Evaluate and optimize a proposed parlay for correlation and value | `legs` (list of bet selections)                            | Phase 2  |
| `season_outlook`     | Mid/end-of-season team analysis with futures assessment           | `team`, `sport`                                            | Phase 3  |
| `model_audit`        | Analyze where the prediction model is performing well vs poorly   | `sport` (optional), `time_period`                          | Phase 3  |

---

## Priority Summary

| Priority    | Tools | Resources | Prompts | Focus                                                                                                                           |
| ----------- | ----- | --------- | ------- | ------------------------------------------------------------------------------------------------------------------------------- |
| **MVP**     | 34    | 10        | 6       | Core data, analysis, predictions, full emulated betting (all standard bet types + parlays), bankroll management, odds/value     |
| **Phase 2** | 28    | 8         | 4       | Advanced analysis, line movement, teasers/round robins/futures, backtesting, notifications, performance analytics, gamification |
| **Phase 3** | 10    | 0         | 2       | Monte Carlo, correlations, CLV, live betting, SGP, niche sports                                                                 |

**Total: 72 tools, 18 resources, 12 prompts**

## Consequences

- **Positive:** Comprehensive MCP server gives the agent rich data access for informed predictions;
  phased approach keeps MVP manageable
- **Negative:** Free API tiers have rate limits that may constrain real-time usage; ESPN hidden API
  is unofficial and could change; historical data may require initial bulk loading
- **Risks:** The Odds API free tier (500 req/mo) may be insufficient for daily use across 5 sports —
  may need paid tier ($25/mo) early on
