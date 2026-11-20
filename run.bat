@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
title BarangayFixTracker

echo ============================================
echo   BarangayFixTracker - starting...
echo ============================================

rem --- 1. Is the JDK installed? (javac must exist, a plain JRE is not enough)
where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo [X] Java JDK was not found.
    echo     Install a JDK 11 or newer ^(for example Temurin from https://adoptium.net^),
    echo     tick "Add to PATH" in the installer, then close this window and run again.
    echo.
    pause
    exit /b 1
)

rem --- 2. Download the SQLite database driver once (needs internet only the first time)
if not exist backend\lib mkdir backend\lib
if not exist "backend\lib\sqlite-jdbc-3.41.2.2.jar" (
    echo.
    echo Downloading the SQLite database driver ^(first time only^)...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { Invoke-WebRequest -UseBasicParsing 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.41.2.2/sqlite-jdbc-3.41.2.2.jar' -OutFile 'backend\lib\sqlite-jdbc-3.41.2.2.jar'; Invoke-WebRequest -UseBasicParsing 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar' -OutFile 'backend\lib\slf4j-api-1.7.36.jar' } catch { Write-Host $_.Exception.Message; exit 1 }"
    if errorlevel 1 (
        echo [!] Could not download the driver. Check your internet, or download the 2 jars by hand ^(see README.txt^).
        echo     The website will still run, but it will save to a text file instead of the database.
        if exist "backend\lib\sqlite-jdbc-3.41.2.2.jar" del "backend\lib\sqlite-jdbc-3.41.2.2.jar"
    )
)

rem --- 3. Compile (paths with spaces are handled)
if exist backend\out rmdir /s /q backend\out
mkdir backend\out
if exist sources.txt del sources.txt
for /r "backend\src" %%f in (*.java) do (
    set "p=%%f"
    echo "!p:\=/!">>sources.txt
)
javac -encoding UTF-8 -cp "backend/lib/*" -d backend/out @sources.txt
if errorlevel 1 (
    echo.
    echo [X] Compile failed. Read the error above.
    pause
    exit /b 1
)

rem --- 4. Run
echo.
echo Starting the server. Open http://localhost:8080 in your browser.
echo Keep this window open. Press Ctrl+C to stop.
echo.
java -cp "backend/out;backend/lib/*" barangaydamagereporter.web.Main %1
echo.
echo The server stopped. Read any message above.
pause
