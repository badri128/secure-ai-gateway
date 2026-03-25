@echo off
setlocal

set "MVN_CMD="

if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
    )
)

if not defined MVN_CMD (
    for /f "delims=" %%I in ('where.exe mvn.cmd 2^>nul') do (
        set "MVN_CMD=%%I"
        goto run_maven
    )
)

if not defined MVN_CMD if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\plugins\maven\lib\maven3\bin\mvn.cmd" (
    set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.4\plugins\maven\lib\maven3\bin\mvn.cmd"
)

:run_maven
if not defined MVN_CMD (
    echo Maven was not found on PATH or in MAVEN_HOME.
    echo Install Maven or run this project from IntelliJ with bundled Maven enabled.
    exit /b 1
)

call "%MVN_CMD%" %*
