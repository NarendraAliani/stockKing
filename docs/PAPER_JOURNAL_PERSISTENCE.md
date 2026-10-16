# Paper journal persistence design

## Status

Proposed implementation plan. The current paper ledger remains in-memory only; this document does not enable persistence.

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

On startup, load and validate the version before making the ledger available. Missing journal means an empty ledger only when persistence is explicitly enabled and the configured path does not exist. Invalid JSON, unsupported versions, invalid trade states, and duplicate signal IDs should prevent startup rather than silently discard history.

## Rollout stages

1. Implement the standalone store with schema/version validation and atomic replacement.
2. Add tests for missing file, round-trip of open and closed trades, malformed JSON, unsupported schema, duplicate IDs, and write failure.
3. Add explicit opt-in configuration, defaulting off.
4. Integrate ledger snapshots and startup restore only after store tests pass.
5. Add integration tests proving disabled mode performs no file I/O and enabled mode restores paper trades after restart.

## Explicitly out of scope

No broker integration, live orders, database connectivity, cloud sync, automatic journal deletion, or claims of guaranteed durability across all filesystems.
