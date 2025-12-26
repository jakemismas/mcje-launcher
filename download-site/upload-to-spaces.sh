#!/bin/bash
# Upload MCJE Launcher files to Digital Ocean Spaces
#
# Prerequisites: Install s3cmd
#   brew install s3cmd
#   s3cmd --configure (use your DO Spaces keys)

set -e

# Configuration
BUCKET_NAME="mcje-bucket"
REGION="sfo3"
ENDPOINT="https://${REGION}.digitaloceanspaces.com"
BUCKET_URL="https://${BUCKET_NAME}.${REGION}.digitaloceanspaces.com"

# Paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MAC_INSTALLER="$PROJECT_ROOT/dist/macos/MCJE-Launcher-Installer.dmg"
WINDOWS_INSTALLER="$PROJECT_ROOT/dist/windows/MCJE-Launcher-Setup.exe"
INDEX_HTML="$SCRIPT_DIR/index.html"

echo "=========================================="
echo "  Uploading MCJE Launcher to Spaces"
echo "=========================================="
echo ""

# Check if s3cmd is installed
if ! command -v s3cmd &> /dev/null; then
    echo "ERROR: s3cmd is not installed."
    echo ""
    echo "Install with: brew install s3cmd"
    echo "Configure with: s3cmd --configure"
    echo ""
    echo "When configuring, use:"
    echo "  - Access Key: Your DO Spaces Access Key"
    echo "  - Secret Key: Your DO Spaces Secret Key"
    echo "  - S3 Endpoint: ${ENDPOINT}"
    echo "  - DNS-style bucket: %(bucket)s.${REGION}.digitaloceanspaces.com"
    exit 1
fi

# Upload index.html
echo "[1/3] Uploading download page..."
if [ -f "$INDEX_HTML" ]; then
    s3cmd put "$INDEX_HTML" \
        "s3://${BUCKET_NAME}/index.html" \
        --acl-public \
        --mime-type="text/html" \
        --host="${REGION}.digitaloceanspaces.com" \
        --host-bucket="${REGION}.digitaloceanspaces.com"
    echo "  ✓ index.html uploaded"
    echo "  → ${BUCKET_URL}/index.html"
else
    echo "  ✗ index.html not found"
fi

echo ""

# Upload macOS installer
echo "[2/3] Uploading macOS installer..."
if [ -f "$MAC_INSTALLER" ]; then
    s3cmd put "$MAC_INSTALLER" \
        "s3://${BUCKET_NAME}/MCJE-Launcher-Installer.dmg" \
        --acl-public \
        --mime-type="application/x-apple-diskimage" \
        --host="${REGION}.digitaloceanspaces.com" \
        --host-bucket="${REGION}.digitaloceanspaces.com"
    echo "  ✓ MCJE-Launcher-Installer.dmg uploaded"
    echo "  → ${BUCKET_URL}/MCJE-Launcher-Installer.dmg"
else
    echo "  ✗ macOS installer not found at: $MAC_INSTALLER"
    echo "  Run: cd installer/macos && ./build-installer.sh"
fi

echo ""

# Upload Windows installer
echo "[3/3] Uploading Windows installer..."
if [ -f "$WINDOWS_INSTALLER" ]; then
    s3cmd put "$WINDOWS_INSTALLER" \
        "s3://${BUCKET_NAME}/MCJE-Launcher-Setup.exe" \
        --acl-public \
        --mime-type="application/x-msdownload" \
        --host="${REGION}.digitaloceanspaces.com" \
        --host-bucket="${REGION}.digitaloceanspaces.com"
    echo "  ✓ MCJE-Launcher-Setup.exe uploaded"
    echo "  → ${BUCKET_URL}/MCJE-Launcher-Setup.exe"
else
    echo "  ✗ Windows installer not found at: $WINDOWS_INSTALLER"
    echo "  Build on Windows: cd installer\\windows && .\\build-installer.ps1"
fi

echo ""
echo "=========================================="
echo "  Upload Complete!"
echo "=========================================="
echo ""
echo "Download page: ${BUCKET_URL}/index.html"
echo ""
echo "Direct links:"
echo "  macOS:   ${BUCKET_URL}/MCJE-Launcher-Installer.dmg"
echo "  Windows: ${BUCKET_URL}/MCJE-Launcher-Setup.exe"
echo ""
