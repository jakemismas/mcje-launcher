#!/bin/bash
# MCJE Launcher macOS Installer Build Script
# This script:
# 1. Downloads Adoptium Temurin Java 21 JRE
# 2. Builds the launcher bootstrap
# 3. Creates an macOS .app bundle
# 4. Creates a DMG installer

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"
APP_NAME="MCJE Launcher"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"
DMG_NAME="MCJE-Launcher-Installer"
DIST_DIR="$PROJECT_ROOT/dist/macos"
JAVA_VERSION="21"

echo "========================================"
echo "  MCJE Launcher macOS Installer Build"
echo "========================================"
echo ""

# Detect architecture
ARCH=$(uname -m)
if [ "$ARCH" = "arm64" ]; then
    ADOPTIUM_ARCH="aarch64"
    echo "Detected Apple Silicon (arm64)"
else
    ADOPTIUM_ARCH="x64"
    echo "Detected Intel (x64)"
fi

# Create directories
echo "[1/6] Creating directories..."
mkdir -p "$BUILD_DIR"
mkdir -p "$DIST_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"
mkdir -p "$APP_BUNDLE/Contents/PlugIns"

# Step 1: Download Java 21 JRE
echo "[2/6] Downloading Adoptium Temurin Java $JAVA_VERSION JRE..."
JRE_API_URL="https://api.adoptium.net/v3/assets/latest/$JAVA_VERSION/hotspot?architecture=$ADOPTIUM_ARCH&image_type=jre&os=mac&vendor=eclipse"

echo "  Fetching from: $JRE_API_URL"
JRE_INFO=$(curl -s "$JRE_API_URL")
JRE_URL=$(echo "$JRE_INFO" | python3 -c "import sys, json; print(json.load(sys.stdin)[0]['binary']['package']['link'])")
JRE_FILENAME=$(echo "$JRE_INFO" | python3 -c "import sys, json; print(json.load(sys.stdin)[0]['binary']['package']['name'])")

echo "  Download URL: $JRE_URL"
echo "  File: $JRE_FILENAME"

JRE_ARCHIVE="$SCRIPT_DIR/$JRE_FILENAME"
if [ ! -f "$JRE_ARCHIVE" ]; then
    echo "  Downloading..."
    curl -L -o "$JRE_ARCHIVE" "$JRE_URL"
else
    echo "  Using cached download: $JRE_ARCHIVE"
fi

# Extract JRE
echo "  Extracting JRE..."
rm -rf "$BUILD_DIR/temp_jre"
mkdir -p "$BUILD_DIR/temp_jre"
tar -xzf "$JRE_ARCHIVE" -C "$BUILD_DIR/temp_jre"

# Move JRE to PlugIns directory (standard macOS app bundle location for embedded JRE)
JRE_EXTRACTED=$(ls "$BUILD_DIR/temp_jre")
rm -rf "$APP_BUNDLE/Contents/PlugIns/jre"
mv "$BUILD_DIR/temp_jre/$JRE_EXTRACTED" "$APP_BUNDLE/Contents/PlugIns/jre"
rm -rf "$BUILD_DIR/temp_jre"

echo "  JRE extracted to: $APP_BUNDLE/Contents/PlugIns/jre"

# Step 2: Build launcher bootstrap
echo "[3/6] Building launcher bootstrap..."
cd "$PROJECT_ROOT"
./gradlew :launcher-bootstrap:shadowJar --no-daemon
echo "  Bootstrap built successfully"

# Copy bootstrap JAR
BOOTSTRAP_JAR=$(ls "$PROJECT_ROOT/launcher-bootstrap/build/libs/launcher-bootstrap-"*.jar | head -1)
cp "$BOOTSTRAP_JAR" "$APP_BUNDLE/Contents/Resources/launcher-bootstrap.jar"
echo "  Copied: $BOOTSTRAP_JAR"

# Step 3: Create launcher script
echo "[4/6] Creating launcher script..."
cat > "$APP_BUNDLE/Contents/MacOS/MCJE Launcher" << 'EOF'
#!/bin/bash
# MCJE Launcher - macOS Launcher Script

# Get the directory of the app bundle
APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# Path to bundled Java
JAVA_HOME="$APP_DIR/PlugIns/jre/Contents/Home"
JAVA_EXE="$JAVA_HOME/bin/java"

# Path to bootstrap JAR
BOOTSTRAP_JAR="$APP_DIR/Resources/launcher-bootstrap.jar"

# Verify Java exists
if [ ! -f "$JAVA_EXE" ]; then
    osascript -e 'display dialog "Error: Java runtime not found.\n\nPlease reinstall the MCJE Launcher." with title "MCJE Launcher Error" buttons {"OK"} default button 1 with icon stop'
    exit 1
fi

# Verify bootstrap JAR exists
if [ ! -f "$BOOTSTRAP_JAR" ]; then
    osascript -e 'display dialog "Error: Launcher bootstrap not found.\n\nPlease reinstall the MCJE Launcher." with title "MCJE Launcher Error" buttons {"OK"} default button 1 with icon stop'
    exit 1
fi

# Launch the bootstrap
exec "$JAVA_EXE" \
    -Xdock:name="MCJE Launcher" \
    -Xdock:icon="$APP_DIR/Resources/icon.icns" \
    -jar "$BOOTSTRAP_JAR" \
    "$@"
EOF
chmod +x "$APP_BUNDLE/Contents/MacOS/MCJE Launcher"

# Step 4: Create Info.plist
echo "[5/6] Creating Info.plist..."
cat > "$APP_BUNDLE/Contents/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>MCJE Launcher</string>
    <key>CFBundleIconFile</key>
    <string>icon</string>
    <key>CFBundleIdentifier</key>
    <string>com.mcje.launcher</string>
    <key>CFBundleName</key>
    <string>MCJE Launcher</string>
    <key>CFBundleDisplayName</key>
    <string>MCJE Launcher</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1.0.0</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.13</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
    <key>LSApplicationCategoryType</key>
    <string>public.app-category.games</string>
    <key>NSHumanReadableCopyright</key>
    <string>Copyright © MCJE. All rights reserved.</string>
</dict>
</plist>
EOF

# Check for icon file
ICON_SOURCE="$SCRIPT_DIR/resources/icon.icns"
if [ -f "$ICON_SOURCE" ]; then
    cp "$ICON_SOURCE" "$APP_BUNDLE/Contents/Resources/icon.icns"
    echo "  Icon copied"
else
    echo "  WARNING: icon.icns not found at $ICON_SOURCE"
    echo "  You can create an ICNS file from a PNG using:"
    echo "  - iconutil (macOS built-in)"
    echo "  - https://cloudconvert.com/png-to-icns"
    echo ""
    echo "  Use the icon from: launcher/src/main/resources/com/skcraft/launcher/icon.png"
fi

# Step 5: Create DMG
echo "[6/6] Creating DMG installer..."

# Remove old DMG if exists
rm -f "$DIST_DIR/$DMG_NAME.dmg"

# Create a temporary directory for DMG contents
DMG_TEMP="$BUILD_DIR/dmg_temp"
rm -rf "$DMG_TEMP"
mkdir -p "$DMG_TEMP"

# Copy app bundle
cp -R "$APP_BUNDLE" "$DMG_TEMP/"

# Create a symbolic link to Applications folder
ln -s /Applications "$DMG_TEMP/Applications"

# Create DMG
hdiutil create -volname "$APP_NAME" \
    -srcfolder "$DMG_TEMP" \
    -ov -format UDZO \
    "$DIST_DIR/$DMG_NAME.dmg"

# Cleanup
rm -rf "$DMG_TEMP"

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"
echo ""
echo "App Bundle: $APP_BUNDLE"
echo "DMG Installer: $DIST_DIR/$DMG_NAME.dmg"
echo ""
echo "To install, open the DMG and drag MCJE Launcher to Applications."
