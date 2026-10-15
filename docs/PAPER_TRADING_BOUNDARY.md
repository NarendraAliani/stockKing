# Paper-Trading Execution Boundary

## Purpose

Define the first runtime feature to implement before importing broker-facing legacy code: a local paper-trading lifecycle that cannot call a broker. This document is an implementation contract, not a claim that the lifecycle is already implemented.

## Scope of the first vertical slice

1. Accept a strategy signal only after server-side validation.
2. Apply the configured risk policy before creating a paper trade.
3. Create a paper order and deterministic simulated fill without broker connectivity.
4. Track lifecycle state and an auditable reason for each transition.
5. Expose read-only status and trade history through the application API/UI.
6. Keep all broker adapters, credentials, database connections, and scheduled jobs absent or disabled in this slice.

## Explicit non-goals

- No live order placement, broker login, token refresh, or broker session management.
- No use of the inherited scanner's controllers or services.
- No writes to the existing F&O scanner database.
- No claims of profitability or realistic execution quality from the initial simulator.
- No external notifications in automated tests.

## Suggested domain boundary

Keep the core model independent of Spring MVC, persistence, and broker SDK types.

- StrategySignal: strategy identifier, instrument identifier, direction, observation time, entry reference price, stop, target, and a stable signal ID.
- RiskDecision: accepted/rejected result, quantity, maximum permitted loss, and machine-readable rejection reason.
- PaperOrder: local ID, signal ID, side, requested quantity, reference price, status, and timestamps.
- PaperTrade: local ID, entry and exit records, realized P&L, fees/slippage assumptions, and final state.
- PaperExecutionPort: interface for paper-only order creation/fill simulation. It must not accept broker clients or credentials.
- RiskPolicy: validates signal fields and computes permitted quantity from available paper capital and configured risk limits.

Names are proposals; align them with existing package conventions before coding.

## Required safety invariants

1. live-enabled defaults to false and cannot be enabled by a UI request in this stage.
2. Every order created by this slice has an explicit PAPER execution mode.
3. The paper execution implementation has no dependency on broker interfaces or SDKs.
4. Invalid, stale, duplicate, or risk-rejected signals cannot create orders.
5. Quantity is positive, respects configured capital and risk limits, and uses a documented lot-size rule where applicable.
6. A stop/target or price input that is missing, non-finite, non-positive, or directionally inconsistent is rejected.
7. Replaying the same signal ID is idempotent and does not create duplicate trades.
8. Every transition records timestamp, prior state, new state, and reason.
9. Tests use a fixed clock and deterministic fill model; no network calls are permitted.
10. The API does not accept a mode override that bypasses server-side policy.

## Minimum test matrix

| Scenario | Expected result |
|---|---|
| Valid signal within risk limits | Paper order/trade created with PAPER mode |
| Live mode requested by client | Rejected; no broker or order-routing call |
| Risk limit exceeded | Rejected with reason; no order created |
| Zero/negative quantity or invalid price | Rejected |
| Stop/target inconsistent with direction | Rejected |
| Duplicate signal ID | Existing result returned; no duplicate order |
| Stale signal | Rejected according to configured freshness limit |
| Fill/exit event replayed | Idempotent state transition |
| Application startup | No broker connection, database write, or scheduled trading action |
| Any test execution | No external network access or real notification |

## Implementation sequence

1. Inspect the actual foundation source tree and existing risk-policy tests.
2. Implement the domain records and pure risk calculation with unit tests.
3. Implement deterministic in-memory paper execution and lifecycle transitions.
4. Add API endpoints only after core invariants pass; expose DTOs, not internal mutable entities.
5. Add a status view showing paper mode and risk rejection reasons.
6. Add CI tests that assert no broker adapter is wired into the paper execution module.
7. Only then evaluate persistence, using a new isolated schema and reviewed migrations—not the existing scanner database.
8. Import legacy strategy/scanner code incrementally behind interfaces, one module at a time.

## Current verification status

This contract is documentation only. It does not indicate that paper order lifecycle, persistence, broker isolation tests, or a trading UI have been implemented. No SQL was executed, no database was opened, and no broker or notification provider was contacted.
