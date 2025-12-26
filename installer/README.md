# MCJE Launcher Installer

This directory contains installer configurations for Windows and macOS that bundle Java 21 and create proper shortcuts that can be pinned to the taskbar/dock.

## What the Installers Do

1. **Download and bundle Java 21** - Uses Adoptium Temurin JRE 21
2. **Install the launcher bootstrap** - The bootstrap auto-updates the main launcher
3. **Create shortcuts** - Desktop and Start Menu/Applications shortcuts
4. **Enable taskbar pinning** - Shortcuts are configured for Windows taskbar pinning

## Prerequisites

### Windows

1. **Inno Setup 6** - Download from [jrsoftware.org](https://jrsoftware.org/isinfo.php)
2. **PowerShell 5.1+** - Included with Windows 10/11
3. **(Optional) Launch4j** - For creating a native EXE wrapper. Download from [launch4j.sourceforge.net](https://launch4j.sourceforge.net/)

### macOS

1. **Xcode Command Line Tools** - Run `xcode-select --install`
2. **Gradle** - Included in project as wrapper

## Building the Installers

### Step 1: Create Icon Files

Before building, you need to create icon files from the existing PNG.

**Source PNG:** `launcher/src/main/resources/com/skcraft/launcher/icon.png`

#### For Windows (icon.ico):
1. Go to [convertico.com](https://convertico.com/) or [icoconvert.com](https://icoconvert.com/)
2. Upload the PNG file
3. Generate ICO with sizes: 16, 32, 48, 64, 128, 256
4. Save as `installer/windows/resources/icon.ico`

#### For macOS (icon.icns):
1. On macOS, use iconutil:
   ```bash
   mkdir icon.iconset
   sips -z 16 16 icon.png --out icon.iconset/icon_16x16.png
   sips -z 32 32 icon.png --out icon.iconset/icon_16x16@2x.png
   sips -z 32 32 icon.png --out icon.iconset/icon_32x32.png
   sips -z 64 64 icon.png --out icon.iconset/icon_32x32@2x.png
   sips -z 128 128 icon.png --out icon.iconset/icon_128x128.png
   sips -z 256 256 icon.png --out icon.iconset/icon_128x128@2x.png
   sips -z 256 256 icon.png --out icon.iconset/icon_256x256.png
   sips -z 512 512 icon.png --out icon.iconset/icon_256x256@2x.png
   sips -z 512 512 icon.png --out icon.iconset/icon_512x512.png
   sips -z 1024 1024 icon.png --out icon.iconset/icon_512x512@2x.png
   iconutil -c icns icon.iconset
   ```
2. Or use [cloudconvert.com](https://cloudconvert.com/png-to-icns)
3. Save as `installer/macos/resources/icon.icns`

### Step 2: Build the Installer

#### Windows

Open PowerShell as Administrator and run:

```powershell
cd installer\windows
.\build-installer.ps1
```

The script will:
1. Download Adoptium Temurin Java 21 JRE
2. Build the launcher bootstrap using Gradle
3. Create the EXE wrapper (if Launch4j is installed)
4. Build the installer using Inno Setup

**Output:** `dist/windows/MCJE-Launcher-Setup.exe`

#### macOS

Open Terminal and run:

```bash
cd installer/macos
chmod +x build-installer.sh
./build-installer.sh
```

The script will:
1. Download Adoptium Temurin Java 21 JRE (arm64 or x64 based on your Mac)
2. Build the launcher bootstrap using Gradle
3. Create a .app bundle
4. Create a DMG installer

**Output:** `dist/macos/MCJE-Launcher-Installer.dmg`

## Installer Options

### Windows - Two Versions

1. **MCJELauncher.iss** - Full version, requires Launch4j for native EXE
2. **MCJELauncher-Simple.iss** - Uses VBScript launcher, no Launch4j needed

To use the simple version, edit `build-installer.ps1` and change:
```powershell
& $innoPath "$ScriptDir\MCJELauncher-Simple.iss"
```

### Customization

Edit these files to customize the installer:
- Version number and metadata in `.iss` files
- Java version in build scripts (default: Java 21)
- Bundle identifier in `Info.plist` (macOS)

## File Structure

```
installer/
├── README.md              # This file
├── windows/
│   ├── MCJELauncher.iss       # Inno Setup script (full)
│   ├── MCJELauncher-Simple.iss # Inno Setup script (simple)
│   ├── build-installer.ps1     # Windows build script
│   ├── launch4j-config.xml     # Launch4j configuration
│   ├── jre/                    # Downloaded Java JRE (created by build)
│   └── resources/
│       ├── icon.ico            # Windows icon (YOU MUST CREATE)
│       ├── launcher.bat        # Batch launcher script
│       └── launcher.vbs        # VBScript launcher (no console)
├── macos/
│   ├── build-installer.sh      # macOS build script
│   ├── build/                  # Build output (created by build)
│   └── resources/
│       └── icon.icns           # macOS icon (YOU MUST CREATE)
└── dist/                       # Final installers (created by build)
    ├── windows/
    │   └── MCJE-Launcher-Setup.exe
    └── macos/
        └── MCJE-Launcher-Installer.dmg
```

## How It Works

### Bootstrap Architecture

The MCJE Launcher uses a bootstrap system:

1. **Installer installs:** Bootstrap JAR + Bundled Java 21
2. **User runs shortcut:** Launches bootstrap with bundled Java
3. **Bootstrap checks:** Downloads/updates main launcher from `latestUrl`
4. **Launcher runs:** Main launcher starts, manages modpacks

This means you only need to rebuild the installer when:
- Changing bundled Java version
- Updating bootstrap code
- Changing installer behavior

The main launcher updates automatically!

### Hosting Requirements

Upload these files to your DigitalOcean Spaces bucket:

```
https://mcje-bucket.sfo3.digitaloceanspaces.com/
├── latest.json           # Bootstrap update info
├── launcher.jar          # Main launcher JAR (launcher-fancy)
├── packages.json         # Modpack listing
├── news.html             # News page
└── [modpack files]       # Your modpacks
```

**latest.json format:**
```json
{
  "version": "4.6.0",
  "url": "https://mcje-bucket.sfo3.digitaloceanspaces.com/launcher.jar"
}
```

## Troubleshooting

### Windows

**"Inno Setup not found"**
- Install Inno Setup 6 from [jrsoftware.org](https://jrsoftware.org/isinfo.php)

**"Java download failed"**
- Check internet connection
- Try running PowerShell as Administrator

**"Icon not found"**
- Create `installer/windows/resources/icon.ico` (see instructions above)

### macOS

**"Permission denied"**
- Run `chmod +x build-installer.sh`

**"hdiutil: create failed"**
- Make sure you have enough disk space
- Try running with `sudo`

**"Could not find tools.jar" or Java JDK errors**
- The project requires Java 8 JDK to compile
- On Apple Silicon Macs, download x64 Java 8 JDK (runs via Rosetta):
  ```bash
  curl -L "https://api.adoptium.net/v3/binary/latest/8/ga/mac/x64/jdk/hotspot/normal/eclipse?project=jdk" -o /tmp/openjdk8.tar.gz
  mkdir -p ~/java
  tar -xzf /tmp/openjdk8.tar.gz -C ~/java/
  ```
- Create `gradle.properties` in project root with:
  ```
  org.gradle.java.installations.paths=/Users/YOUR_USERNAME/java/jdk8u472-b08/Contents/Home
  ```

**App won't open - "damaged or can't be opened"**
- The app needs to be code signed for distribution
- For testing, right-click and select "Open"
- Or run: `xattr -cr "MCJE Launcher.app"`

## Code Signing (Production)

For production distribution:

### Windows
- Get a code signing certificate from a CA
- Sign the installer: `signtool sign /f cert.pfx /p password MCJE-Launcher-Setup.exe`

### macOS
- Join Apple Developer Program ($99/year)
- Sign with: `codesign --deep --force --verify --verbose --sign "Developer ID Application: Your Name" "MCJE Launcher.app"`
- Notarize with: `xcrun notarytool submit MCJE-Launcher-Installer.dmg --apple-id you@email.com --team-id TEAMID --password @keychain:AC_PASSWORD`

## License

This installer configuration is part of the MCJE Launcher project.
