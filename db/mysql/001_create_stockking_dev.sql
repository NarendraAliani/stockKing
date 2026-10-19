-- Creates a dedicated disposable local development database only.
-- Review the target before execution. This script does not drop or alter other databases.
CREATE DATABASE IF NOT EXISTS stockking_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

-- Create a local-only least-privilege account separately if desired.
-- Example (set your own strong password locally; never commit it):
-- CREATE USER 'stockking_dev'@'localhost' IDENTIFIED BY 'CHANGE_THIS_LOCALLY';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
--   ON stockking_dev.* TO 'stockking_dev'@'localhost';
