# Security and safe development

- Never commit broker/API credentials, TOTP secrets, Telegram bot tokens, passwords, private keys, database dumps, or production exports.
- Use environment variables or an ignored local configuration file for secrets.
- If any real credentials were included in an archive, rotate them before using the application again.
- Start with a disposable local database and a restricted database user.
- Do not enable Hibernate `ddl-auto=update` on an existing database until schema differences have been reviewed.
- **Important existing-code hazard:** script-master synchronization contains `TRUNCATE TABLE nfo_scripts` and `TRUNCATE TABLE equity_scripts`. Do not run that job against data you need to preserve without reviewing and changing its refresh strategy.
- Trading/order execution must remain disabled during initial development. Use paper-trading simulation and explicit risk gates first.
- Never log access tokens or full authorization headers.
