# MCJE Launcher Icon Converter
# Converts the PNG icon to ICO format using .NET

param(
    [string]$SourcePng = "..\..\launcher\src\main\resources\com\skcraft\launcher\icon.png",
    [string]$OutputIco = "resources\icon.ico"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SourcePath = Join-Path $ScriptDir $SourcePng
$OutputPath = Join-Path $ScriptDir $OutputIco

Write-Host "Converting PNG to ICO..."
Write-Host "Source: $SourcePath"
Write-Host "Output: $OutputPath"

if (-not (Test-Path $SourcePath)) {
    Write-Host "ERROR: Source PNG not found at $SourcePath" -ForegroundColor Red
    exit 1
}

# Ensure output directory exists
$outputDir = Split-Path -Parent $OutputPath
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}

try {
    # Load the source image
    $sourceImage = [System.Drawing.Image]::FromFile($SourcePath)

    # Create icon sizes (Windows standard sizes)
    $sizes = @(16, 32, 48, 64, 128, 256)

    # For a simple ICO, we'll use the largest size we can
    # .NET's Icon.Save method is limited, so we use a workaround

    # Create a bitmap at 256x256
    $bitmap = New-Object System.Drawing.Bitmap(256, 256)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($sourceImage, 0, 0, 256, 256)
    $graphics.Dispose()

    # Convert to icon
    $icon = [System.Drawing.Icon]::FromHandle($bitmap.GetHicon())

    # Save the icon
    $stream = [System.IO.File]::Create($OutputPath)
    $icon.Save($stream)
    $stream.Close()

    # Cleanup
    $icon.Dispose()
    $bitmap.Dispose()
    $sourceImage.Dispose()

    Write-Host "Icon created successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "NOTE: This creates a simple single-size ICO (256x256)." -ForegroundColor Yellow
    Write-Host "For a multi-resolution ICO, use an online converter like:" -ForegroundColor Yellow
    Write-Host "  - https://convertico.com/" -ForegroundColor Cyan
    Write-Host "  - https://icoconvert.com/" -ForegroundColor Cyan
}
catch {
    Write-Host "ERROR: Failed to convert icon: $_" -ForegroundColor Red
    exit 1
}
