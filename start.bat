@echo off
title DepGraph
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo.
  echo  Java was not found on this computer.
  echo  Please install JDK 17 from https://adoptium.net and then double-click start.bat again.
  echo.
  pause
  exit /b
)
echo.
echo  Starting DepGraph... your browser will open in a few seconds.
echo  KEEP THIS WINDOW OPEN while you use DepGraph. Close it to stop.
echo.
java DepGraph.java
pause
