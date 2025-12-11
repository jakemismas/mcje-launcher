@echo off
REM MCJE Launcher - Windows Launcher Script
REM This script launches the bootstrap JAR with the bundled Java runtime

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set "LAUNCHER_DIR=%~dp0"
set "LAUNCHER_DIR=%LAUNCHER_DIR:~0,-1%"

REM Path to bundled Java
set "JAVA_EXE=%LAUNCHER_DIR%\jre\bin\javaw.exe"

REM Path to bootstrap JAR
set "BOOTSTRAP_JAR=%LAUNCHER_DIR%\launcher-bootstrap.jar"

REM Check if bundled Java exists
if not exist "%JAVA_EXE%" (
    echo Error: Java runtime not found at %JAVA_EXE%
    echo Please reinstall the MCJE Launcher.
    pause
    exit /b 1
)

REM Check if bootstrap JAR exists
if not exist "%BOOTSTRAP_JAR%" (
    echo Error: Launcher bootstrap not found at %BOOTSTRAP_JAR%
    echo Please reinstall the MCJE Launcher.
    pause
    exit /b 1
)

REM Launch the bootstrap with javaw (no console window)
start "" "%JAVA_EXE%" -jar "%BOOTSTRAP_JAR%" %*
