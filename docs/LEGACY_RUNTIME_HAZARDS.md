# Legacy Runtime Hazard Register

This register captures concrete hazards found during static review of the inherited scanner source. It is a review aid, not a claim that the legacy application is integrated or tested.

## Confirmed findings

| Area | Finding | Required treatment before import |
|---|---|---|
| Database schema | Legacy configuration uses Hibernate schema validation in the inspected defaults, but environment overrides must be checked. | Force schema mutation off; use reviewed, versioned migrations only. Test against a disposable database. |
| Script catalog refresh | `com/foscanner/service/ScriptSyncPersistenceSupport.java:56-57` executes `TRUNCATE TABLE nfo_scripts` and `TRUNCATE TABLE equity_scripts`. The scheduled entry point is `com/foscanner/scheduler/ScriptSyncScheduler.java:16`; manual sync is also exposed by `ScriptApiController`. | Make refresh explicitly opt-in, add a staging table/transactional swap and rollback, and test row preservation. Never point it at the existing database during initial validation. |
| Startup broker connection | `com/foscanner/service/ScannerService.java:181-182` has an application-ready event handler. Review indicates startup can connect when active broker and auto-connect conditions are met; a configured Dhan token can make this path eligible. | Remove startup broker connection behavior. Require explicit, authenticated user action and keep it unavailable in paper-only mode unless a read-only market-data adapter is separately approved. |
| UI connection paths | `com/foscanner/controller/BacktestController.java` contains connection handlers and calls to `connectionService.connect(...)`; `ScannerController` also exposes a connect endpoint. Signal-details flows can attempt connection when disconnected. | Rendering a page or running a backtest must not connect to a broker. Separate market-data reads from order/execution capabilities and test call boundaries. |
| Notification delivery | The legacy `TelegramService` constructs a request but has its HTTP POST commented out, then reports success-like status. `com/foscanner/service/notification/TelegramNotificationService.java` has Java HTTP-client POST paths and response handling. | Consolidate callers onto the response-checking implementation; report success only after provider acceptance. Test failures and timeouts. |
| Browser secret exposure | `com/foscanner/service/AppSettingsService.java:170` maps the Telegram bot token into the settings DTO; `com/foscanner/dto/AppSettingsDto.java` has a token field and getter. | Never serialize tokens to the UI. Use write-only secret inputs and masked configured/not-configured status. Redact secrets from logs and exceptions. |
| Third-party stream adapter | The archive does not include the referenced SmartStream JAR; `SmartStreamTickerImpl` is a stub and does not establish that a real WebSocket feed works. | Do not invent a replacement API or bundle an unlicensed JAR. Define an adapter interface and either obtain an approved dependency or disable that integration. |
| Scheduled jobs | Static scan found scheduled methods in `ScriptSyncScheduler.java:16`, `DailyReportService.java:112`, and `ScannerService.java:1032`. Reports may send Telegram messages. | Make scheduling disabled by default and individually opt-in. Test with a fixed clock and fake notification provider. |
| Order placement | Static search across all 156 Java files for `placeOrder`, `place_order`, `OrderRequest`, `modifyOrder`, `cancelOrder`, and related common spellings found no matches. The archive does contain HTTP POST calls for broker connections and market-data endpoints; those are not by themselves proof of order entry or its absence. | Inspect all broker SDK usage and endpoint/service call graphs, including indirect/reflection-based calls, then document whether order entry is present. Do not enable any order-routing path until all order, retry, cancel/modify, and configuration gates are reviewed. |

## Review coverage and limitations

The static scan counted 156 Java source files. It searched for common order API method names and request types, scheduled annotations, truncating SQL, broker connection calls, notification HTTP calls, and Telegram token mappings. The scan is a triage aid, not a semantic call-graph analysis or a runtime test. It cannot rule out indirect SDK use, dynamically constructed requests, reflection, or external services.

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

The source was inspected statically; this is not a runtime test. No broker was contacted, no notification was sent, no SQL was executed, and no existing database was opened or changed during this review. Runtime behavior remains unverified.
