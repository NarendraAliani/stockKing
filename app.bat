@echo off
setlocal EnableExtensions
title StockKing - Safe Local Launcher
cd /d "%~dp0"

rem StockKing safe local defaults. This launcher never enables live trading.
set "SERVER_PORT=8080"
set "STOCKKING_TRADING_MODE=paper"
set "STOCKKING_TRADING_LIVE_ENABLED=false"
set "STOCKKING_RISK_PER_TRADE_PERCENT=1.0"
set "STOCKKING_RISK_MAX_DAILY_LOSS_PERCENT=2.0"
set "STOCKKING_RISK_MAX_CONSECUTIVE_LOSSES=2"
set "STOCKKING_RISK_MAX_ENTRIES_PER_DAY=3"

:menu
cls
echo ============================================================
echo   StockKing - Local Launcher
echo ============================================================
echo   Mode              : PAPER ONLY
echo   Live trading      : DISABLED
echo   Port              : %SERVER_PORT%
echo   Risk per trade    : %STOCKKING_RISK_PER_TRADE_PERCENT%%%
echo   Max daily loss    : %STOCKKING_RISK_MAX_DAILY_LOSS_PERCENT%%%
echo   Max losing streak : %STOCKKING_RISK_MAX_CONSECUTIVE_LOSSES%
echo   Max entries/day   : %STOCKKING_RISK_MAX_ENTRIES_PER_DAY%
echo ============================================================
echo.
echo   1. Start StockKing
echo   2. Run tests
echo   3. Open local status endpoint
echo   4. Exit
echo.
set "SK_CHOICE="
set /p "SK_CHOICE=Choose an option [1-4]: "

if "%SK_CHOICE%"=="1" goto start
if "%SK_CHOICE%"=="2" goto test
if "%SK_CHOICE%"=="3" goto status
if "%SK_CHOICE%"=="4" goto end
echo Invalid option.
timeout /t 2 >nul
goto menu

:check_java
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java was not found on PATH. Install a JDK 21 and reopen this window.
  exit /b 1
)
java -version 2>&1 | findstr /C:"21." >nul
if errorlevel 1 (
  echo ERROR: Java 21 is required. The Java version detected was not recognized as 21.x.
  echo Install a JDK 21 and ensure JAVA_HOME and PATH point to it.
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

:start
call :check_java
if errorlevel 1 goto pause_menu
call :check_gradle
if errorlevel 1 goto pause_menu
echo.
echo Starting StockKing with safe paper-only settings...
echo No database or broker credentials are configured by this launcher.
echo Keep this window open while StockKing is running.
echo.
gradle --no-daemon bootRun --args="--server.port=%SERVER_PORT% --stockking.trading.mode=paper --stockking.trading.live-enabled=false --stockking.risk.per-trade-percent=%STOCKKING_RISK_PER_TRADE_PERCENT% --stockking.risk.max-daily-loss-percent=%STOCKKING_RISK_MAX_DAILY_LOSS_PERCENT% --stockking.risk.max-consecutive-losses=%STOCKKING_RISK_MAX_CONSECUTIVE_LOSSES% --stockking.risk.max-entries-per-day=%STOCKKING_RISK_MAX_ENTRIES_PER_DAY%"
if errorlevel 1 echo StockKing stopped or failed to start. Review the messages above.
goto pause_menu

:test
call :check_java
if errorlevel 1 goto pause_menu
call :check_gradle
if errorlevel 1 goto pause_menu
echo.
echo Running the automated test suite...
gradle --no-daemon clean test
if errorlevel 1 echo Tests failed. Review the Gradle output above.
goto pause_menu

:status
echo Opening http://localhost:%SERVER_PORT%/api/status
start "" "http://localhost:%SERVER_PORT%/api/status"
goto menu

:pause_menu
echo.
pause
goto menu

:end
endlocal
exit /b 0
