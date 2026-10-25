@echo off
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d out @sources.txt
if errorlevel 1 pause & exit /b 1
java -cp "out;lib/*" barangaydamagereporter.web.Main
