# Database dump safety assessment

## Scope

This assessment covers the file supplied in the current conversation as `foscanner Schema.sql`. It is a static file inspection only. No SQL was executed and no database connection was opened.

## Finding: this file appears to be schema-only

Inspection of the supplied file found:

- 25 `CREATE TABLE` statements.
- 25 `DROP TABLE IF EXISTS` statements.
- Zero `INSERT INTO` statements.
- Zero `REPLACE INTO` statements.
- No `LOAD DATA` statements.
- A `CREATE DATABASE IF NOT EXISTS foscanner` statement and `USE foscanner` directive.

Therefore, this particular file does **not** appear to contain table-row data. The user's description says the dump contains both schema and data, so the supplied file and that description do not currently match. There may be a separate full dump, a companion data file, or another export that has not been inspected yet.

## Destructive-import risk

Do not run this file against the database you use. Each `DROP TABLE IF EXISTS` can delete an existing table and all rows in it before the replacement schema is created. The database-selection directive also targets the database named `foscanner`; it is not a safe way to create an isolated test environment.

Do not remove the `DROP` lines and then assume the import is safe: the schema can still conflict with existing tables, and `CREATE TABLE IF NOT EXISTS` does not reconcile differences between an existing table and the exported definition.

## Required handling

1. Preserve the supplied file as read-only input; do not edit or execute it.
2. Locate the actual full backup/export if row data is expected. Keep it outside Git and do not paste credentials or sensitive rows into issues or pull requests.
3. Verify the full file locally for schema statements, data statements, database name, table count, and any triggers, routines, views, or events.
4. Create a separate disposable database with a distinct name and least-privilege credentials for validation. Never use the existing database as the test target.
5. Restore only into that disposable database, then compare table definitions and row counts. Do not attempt any merge into the existing database until a written migration and rollback plan has been reviewed and explicitly approved.
6. Before any later production migration, create and verify an independent backup and confirm a tested restore procedure.

## Current status

- [x] Supplied `foscanner Schema.sql` inspected statically.
- [x] Destructive statements identified.
- [x] No SQL was executed and no database was modified.
- [ ] Actual full data-bearing dump located and verified.
- [ ] Disposable restore tested.
- [ ] Schema/data reconciliation and rollback plan reviewed.

Until the remaining items are completed, StockKing work must not import this file or connect to the existing database.