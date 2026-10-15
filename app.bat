@echo off
setlocal EnableExtensions
title StockKing - Automatic Safe Launcher
cd /d "%~dp0"

rem StockKing safe defaults. Live trading remains disabled.
set "SERVER_PORT=8080"
set "STOCKKING_TRADING_MODE=paper"
set "STOCKKING_TRADING_LIVE_ENABLED=false"
set "STOCKKING_RISK_PER_TRADE_PERCENT=1.0"
set "STOCKKING_RISK_MAX_DAILY_LOSS_PERCENT=2.0"
set "STOCKKING_RISK_MAX_CONSECUTIVE_LOSSES=2"
set "STOCKKING_RISK_MAX_ENTRIES_PER_DAY=3"
set "SK_BRANCH=feat/windows-app-launcher"

echo ============================================================
echo   StockKing - Automatic Local Setup
echo ============================================================
echo   Checking Git repository and updating before launch...
echo   Mode: PAPER ONLY   Live trading: DISABLED
echo.

rem Verify this is a Git checkout.
git rev-parse --is-inside-work-tree >nul 2>&1
if errorlevel 1 (
  echo ERROR: This folder is not a Git repository.
  echo Download or clone the StockKing repository, then run app.bat again.
  goto fatal
)

rem Refuse to overwrite local edits. This avoids silently losing user work.
git diff --quiet
if errorlevel 1 (
  echo ERROR: Local tracked changes were found.
  echo Commit or back up your edits before using automatic update.
  goto fatal
)
git diff --cached --quiet
if errorlevel 1 (
  echo ERROR: Staged local changes were found.
  echo Commit or back up your edits before using automatic update.
  goto fatal
)
for /f "delims=" %%S in ('git status --porcelain') do (
  echo ERROR: The working tree is not clean: %%S
  echo Move or back up untracked files before automatic update.
  goto fatal
)

rem Verify network/remote access and fetch the latest branch.
git remote get-url origin >nul 2>&1
if errorlevel 1 (
  echo ERROR: Git remote 'origin' is not configured.
  goto fatal
)
git fetch origin "%SK_BRANCH%"
if errorlevel 1 (
  echo ERROR: Could not fetch updates. Check internet access and GitHub authentication.
  goto fatal
)

rem Require the expected launcher branch, then fast-forward only.
for /f "delims=" %%B in ('git branch --show-current') do set "SK_CURRENT_BRANCH=%%B"
if /I not "%SK_CURRENT_BRANCH%"=="%SK_BRANCH%" (
  echo ERROR: Current branch is "%SK_CURRENT_BRANCH%", expected "%SK_BRANCH%".
  echo Switch to the expected branch and retry:
  echo git switch %SK_BRANCH%
  goto fatal
)

git pull --ff-only origin "%SK_BRANCH%"
if errorlevel 1 (
  echo ERROR: Fast-forward update failed. No automatic merge was attempted.
  echo Resolve the Git branch state manually, then run app.bat again.
  goto fatal
)

echo.
echo Repository is up to date.
echo Checking required software...

call :check_java
if errorlevel 1 goto fatal
call :check_gradle
if errorlevel 1 goto fatal

echo.
echo Running tests before starting StockKing...
call gradle --no-daemon clean test
if errorlevel 1 (
  echo ERROR: Tests failed. Server will not be started.
  goto fatal
)

echo.
echo Tests passed. Starting StockKing with paper-only settings...
echo No database or broker credentials are configured by this launcher.
echo Keep this window open while the server runs.
echo Dashboard: http://localhost:%SERVER_PORT%/paper
echo Status:    http://localhost:%SERVER_PORT%/api/status
echo.
call gradle --no-daemon bootRun --args="--server.port=%SERVER_PORT% --stockking.trading.mode=paper --stockking.trading.live-enabled=false --stockking.risk.per-trade-percent=%STOCKKING_RISK_PER_TRADE_PERCENT% --stockking.risk.max-daily-loss-percent=%STOCKKING_RISK_MAX_DAILY_LOSS_PERCENT% --stockking.risk.max-consecutive-losses=%STOCKKING_RISK_MAX_CONSECUTIVE_LOSSES% --stockking.risk.max-entries-per-day=%STOCKKING_RISK_MAX_ENTRIES_PER_DAY%"
if errorlevel 1 (
  echo.
  echo StockKing stopped or failed to start. Review the output above.
  goto fatal
)
goto end

:check_java
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java was not found on PATH. Install JDK 21 and reopen this window.
  exit /b 1
)
java -version 2>&1 | findstr /C:"21." >nul
if errorlevel 1 (
  echo ERROR: Java 21 is required. Ensure JAVA_HOME and PATH point to JDK 21.
  exit /b 1
)
exit /b 0

:check_gradle
where gradle >nul 2>&1
if errorlevel 1 (
  echo ERROR: Gradle was not found on PATH.
  echo Install Gradle 8.x and add its bin folder to PATH, then reopen this window.
  echo The repository does not include a complete Gradle wrapper.
  exit /b 1
)
exit /b 0

:fatal
echo.
echo StockKing launcher did not continue because a required step failed.
echo Fix the issue shown above and double-click app.bat again.
pause
goto end

:end
endlocal
exit /b 0
