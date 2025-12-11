# MCJE Launcher Windows Installer Build Script
# This script:
# 1. Downloads Adoptium Temurin Java 21 JRE
# 2. Builds the launcher bootstrap
# 3. Creates the EXE wrapper using Launch4j
# 4. Builds the installer using Inno Setup

param(
    [switch]$SkipJavaDownload,
    [switch]$SkipBuild,
    [string]$JavaVersion = "21"
)

$ErrorActionPreference = "Stop"

# Configuration
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Resolve-Path "$ScriptDir\..\.."
$JreDir = "$ScriptDir\jre"
$ResourcesDir = "$ScriptDir\resources"
$DistDir = "$ProjectRoot\dist\windows"

# URLs
$AdoptiumApiUrl = "https://api.adoptium.net/v3/assets/latest/$JavaVersion/hotspot"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MCJE Launcher Windows Installer Build" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Create directories
Write-Host "[1/5] Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $JreDir | Out-Null
New-Item -ItemType Directory -Force -Path $ResourcesDir | Out-Null
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

# Step 1: Download Java 21 JRE
if (-not $SkipJavaDownload) {
    Write-Host "[2/5] Downloading Adoptium Temurin Java $JavaVersion JRE..." -ForegroundColor Yellow

    # Get the latest JRE download URL from Adoptium API
    $apiUrl = "$AdoptiumApiUrl`?architecture=x64&image_type=jre&os=windows&vendor=eclipse"
    Write-Host "  Fetching from: $apiUrl"

    try {
        $response = Invoke-RestMethod -Uri $apiUrl -Method Get
        $downloadUrl = $response[0].binary.package.link
        $fileName = $response[0].binary.package.name
        $checksum = $response[0].binary.package.checksum

        Write-Host "  Download URL: $downloadUrl"
        Write-Host "  File: $fileName"

        $zipPath = "$ScriptDir\$fileName"

        if (-not (Test-Path $zipPath)) {
            Write-Host "  Downloading..."
            Invoke-WebRequest -Uri $downloadUrl -OutFile $zipPath -UseBasicParsing
        } else {
            Write-Host "  Using cached download: $zipPath"
        }

        # Extract JRE
        Write-Host "  Extracting JRE..."
        if (Test-Path $JreDir) {
            Remove-Item -Recurse -Force $JreDir
        }

        Expand-Archive -Path $zipPath -DestinationPath "$ScriptDir\temp_jre" -Force

        # Move contents from nested folder to jre directory
        $extractedDir = Get-ChildItem "$ScriptDir\temp_jre" | Select-Object -First 1
        Move-Item -Path "$($extractedDir.FullName)\*" -Destination $JreDir -Force
        Remove-Item -Recurse -Force "$ScriptDir\temp_jre"

        Write-Host "  JRE extracted to: $JreDir" -ForegroundColor Green
    }
    catch {
        Write-Host "  Error downloading Java: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[2/5] Skipping Java download (using existing)" -ForegroundColor Gray
}

# Step 2: Build launcher bootstrap
if (-not $SkipBuild) {
    Write-Host "[3/5] Building launcher bootstrap..." -ForegroundColor Yellow
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat :launcher-bootstrap:shadowJar --no-daemon
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed"
        }
        Write-Host "  Bootstrap built successfully" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
} else {
    Write-Host "[3/5] Skipping build (using existing)" -ForegroundColor Gray
}

# Copy bootstrap JAR to a fixed name for Inno Setup
$bootstrapJar = Get-ChildItem "$ProjectRoot\launcher-bootstrap\build\libs\launcher-bootstrap-*.jar" | Select-Object -First 1
if ($bootstrapJar) {
    Copy-Item $bootstrapJar.FullName "$ScriptDir\launcher-bootstrap.jar" -Force
    Write-Host "  Copied bootstrap JAR to: $ScriptDir\launcher-bootstrap.jar" -ForegroundColor Green
} else {
    Write-Host "  ERROR: Bootstrap JAR not found!" -ForegroundColor Red
    exit 1
}

# Step 3: Check for icon file
Write-Host "[4/5] Checking resources..." -ForegroundColor Yellow
$iconPath = "$ResourcesDir\icon.ico"
if (-not (Test-Path $iconPath)) {
    Write-Host "  WARNING: icon.ico not found at $iconPath" -ForegroundColor Yellow
    Write-Host "  Please create an icon file or the installer will fail." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  You can create an ICO file from a PNG using online tools like:" -ForegroundColor Cyan
    Write-Host "  - https://convertico.com/" -ForegroundColor Cyan
    Write-Host "  - https://icoconvert.com/" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Use the icon from: launcher\src\main\resources\com\skcraft\launcher\icon.png" -ForegroundColor Cyan

    # Create a placeholder message
    Write-Host ""
    $continue = Read-Host "Continue without icon? (y/n)"
    if ($continue -ne "y") {
        exit 1
    }
}

# Step 4: Create EXE wrapper (using Launch4j if available, otherwise use VBS wrapper)
Write-Host "[5/5] Creating launcher executable..." -ForegroundColor Yellow

$launch4jPath = "C:\Program Files (x86)\Launch4j\launch4jc.exe"
$launch4jAltPath = "C:\Program Files\Launch4j\launch4jc.exe"

if (Test-Path $launch4jPath) {
    Write-Host "  Using Launch4j at: $launch4jPath"
    & $launch4jPath "$ScriptDir\launch4j-config.xml"
} elseif (Test-Path $launch4jAltPath) {
    Write-Host "  Using Launch4j at: $launch4jAltPath"
    & $launch4jAltPath "$ScriptDir\launch4j-config.xml"
} else {
    Write-Host "  Launch4j not found - creating batch wrapper instead" -ForegroundColor Yellow
    Write-Host "  For a proper EXE, install Launch4j from: https://launch4j.sourceforge.net/" -ForegroundColor Cyan

    # Use the VBS script as the launcher (rename to .exe won't work, but we can use a different approach)
    # Create a wrapper batch that can be called
    Copy-Item "$ResourcesDir\launcher.bat" "$ResourcesDir\launcher.exe.bat" -Force
}

# Step 5: Build installer with Inno Setup
Write-Host ""
Write-Host "[FINAL] Building installer with Inno Setup..." -ForegroundColor Yellow

$innoPath = "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
$innoAltPath = "C:\Program Files\Inno Setup 6\ISCC.exe"

if (Test-Path $innoPath) {
    & $innoPath "$ScriptDir\MCJELauncher.iss"
} elseif (Test-Path $innoAltPath) {
    & $innoAltPath "$ScriptDir\MCJELauncher.iss"
} else {
    Write-Host "  Inno Setup not found!" -ForegroundColor Red
    Write-Host "  Please install Inno Setup 6 from: https://jrsoftware.org/isinfo.php" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  After installing, run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Build Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Installer created at: $DistDir\MCJE-Launcher-Setup.exe" -ForegroundColor Cyan
