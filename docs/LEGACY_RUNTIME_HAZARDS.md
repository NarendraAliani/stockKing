# Legacy Runtime Hazard Register

This register captures concrete hazards found during static review of the inherited scanner source. It is a review aid, not a claim that the legacy application is integrated or tested.

## Confirmed findings

| Area | Finding | Required treatment before import |
|---|---|---|
| Database schema | Legacy configuration uses Hibernate schema validation in the inspected defaults, but environment overrides must be checked. | Force schema mutation off; use reviewed, versioned migrations only. Test against a disposable database. |
| Script catalog refresh | The sync service truncates `nfo_scripts` and `equity_scripts` before repopulating them. Scheduled and manual entry points can invoke it asynchronously. | Make refresh explicitly opt-in, add a staging table/transactional swap and rollback, and test row preservation. Never point it at the existing database during initial validation. |
| Startup broker connection | Startup readiness logic may connect to an active broker when auto-connect conditions are met. A configured Dhan token can make this path eligible. | Remove startup broker connection behavior. Require explicit, authenticated user action and keep it unavailable in paper-only mode unless a read-only market-data adapter is separately approved. |
| UI connection paths | A signal-details page may try to connect if disconnected; some backtest handlers may also initiate connections. | Rendering a page or running a backtest must not connect to a broker. Separate market-data reads from order/execution capabilities and test call boundaries. |
| Notification delivery | One Telegram service builds a request but has the HTTP POST commented out, then reports success. A separate Java HTTP client service checks the provider response. | Consolidate callers onto the response-checking implementation; report success only after provider acceptance. Test failures and timeouts. |
| Browser secret exposure | Settings mapping can include the Telegram bot token in a settings DTO returned to the browser. | Never serialize tokens to the UI. Use write-only secret inputs and masked configured/not-configured status. Redact secrets from logs and exceptions. |
| Third-party stream adapter | SmartStream dependency is missing from the archive and referenced classes are stubs. | Do not invent a replacement API or bundle an unlicensed JAR. Define an adapter interface and either obtain an approved dependency or disable that integration. |
| Scheduled jobs | Script sync and daily report scheduling are enabled in the legacy application; reports may send Telegram messages. | Make scheduling disabled by default and individually opt-in. Test with a fixed clock and fake notification provider. |
| Order placement | The static review summarized so far has not established the full order-placement call graph. | Do not import or enable any order-routing path until all broker order calls, retries, cancel/modify operations, and config gates are located and reviewed. |

## Required review before integrating any legacy runtime code

1. Search all call sites for broker connect, place order, modify order, cancel order, and session-token use.
2. Trace controller → service → broker boundaries for every endpoint and scheduled job.
3. Ensure ordinary startup, page rendering, and backtest execution cannot connect or place orders.
4. Replace destructive catalog refresh with a staged, rollback-capable process and test against a disposable database.
5. Remove secret fields from browser-facing DTOs and templates; verify serialized responses.
6. Replace false-positive notification success with provider-confirmed outcomes.
7. Add automated tests for disabled-by-default schedules, paper-mode execution isolation, and redaction.
8. Keep the new StockKing foundation separate until the full import passes clean build and test in CI.

## Verification boundary

These are static source-review findings. No broker was contacted, no notification was sent, no SQL was executed, and no existing database was opened or changed during this review. Runtime behavior remains unverified.
