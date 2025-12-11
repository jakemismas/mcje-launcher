@echo off
REM MCJE Launcher Windows Installer Build
REM Run this script to build the Windows installer

echo.
echo MCJE Launcher Windows Installer Build
echo ======================================
echo.

REM Check if PowerShell is available
where powershell >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: PowerShell is required but not found.
    echo Please install PowerShell or run the build script manually.
    pause
    exit /b 1
)

REM Run the PowerShell build script
powershell -ExecutionPolicy Bypass -File "%~dp0build-installer.ps1"

pause
