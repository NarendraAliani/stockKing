# Paper journal persistence design

## Status

Implemented behind explicit opt-in configuration. Persistence is disabled by default; when enabled, the paper ledger uses the local JSON journal store.

## Constraints

- Paper trading only. Live trading and broker order routing remain disabled.
- Do not connect to, inspect, migrate, or modify the user's existing database.
- Persistence is local and opt-in until recovery behavior is verified.
- A failed write must not be reported as durable success.
- Corrupt or unsupported journal files must fail closed with a clear error; never silently overwrite them with an empty journal.
- Journal paths are operator-configured and must not be derived from signal IDs or other untrusted input.

## Proposed format and write protocol

Use a versioned JSON document containing a schema version and the complete ordered list of paper trades. Keep the journal separate from application configuration and credentials.

For each update:
1. Serialize a consistent ledger snapshot.
2. Create a temporary file in the same directory as the target journal.
3. Write all bytes and flush the file to disk where supported.
4. Replace the target with an atomic move when supported; otherwise use a documented replace fallback.
5. Clean up the temporary file after failures without deleting the last known-good journal.

On startup, load and validate the version before making the ledger available. Missing journal means an empty ledger only when persistence is explicitly enabled and the configured path does not exist. Invalid JSON, unsupported versions, invalid trade states, and duplicate signal or trade IDs should prevent startup rather than silently discard history.

## Rollout stages

1. Implement the standalone store with schema/version validation and atomic replacement.
2. Add tests for missing file, round-trip of open and closed trades, malformed JSON, unsupported schema, duplicate IDs, and write failure.
3. Add explicit opt-in configuration, defaulting off.
4. Integrate ledger snapshots and startup restore behind the opt-in configuration. (Implemented.)
5. Add configuration-level tests proving disabled mode leaves an existing journal untouched, enabled mode restores through application configuration, and invalid journals fail closed. (Covered by PaperTradingConfigurationTest; full Spring-context startup coverage remains a follow-up.)

## Explicitly out of scope

No broker integration, live orders, database connectivity, cloud sync, automatic journal deletion, or claims of guaranteed durability across all filesystems.


## Current implementation notes

- Configuration keys: `stockking.paper.persistence.enabled` (default `false`) and `stockking.paper.persistence.path` (default `./data/paper-journal.json`). Environment overrides are `STOCKKING_PAPER_PERSISTENCE_ENABLED` and `STOCKKING_PAPER_JOURNAL_PATH`.
- The journal is limited to 10 MiB on both load and save. Saved snapshots are written to a sibling temporary file, flushed, and moved into place; atomic replacement is used when supported.
- Trade records are validated for unique signal and trade IDs, positive prices and quantities, stop/target direction, and consistent open/closed fields. Unsupported schemas and malformed data fail closed.
- Risk restoration counts entries opened on the current trading date and realized P&L for trades closed on that date. P&L is gross and does not include fees, taxes, or slippage.
- A local journal is not encrypted and is not a database or backup system. Keep it in a private, access-controlled directory and back it up separately if required.
