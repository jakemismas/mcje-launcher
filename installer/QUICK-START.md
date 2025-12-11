# Quick Start - Build MCJE Launcher Installer

## Windows (5 minutes)

### Prerequisites
1. Install [Inno Setup 6](https://jrsoftware.org/isinfo.php)
2. Have an internet connection (to download Java)

### Build Steps

1. **Create the icon** (required):
   ```powershell
   cd installer\windows
   .\convert-icon.ps1
   ```
   Or manually convert `launcher\src\main\resources\com\skcraft\launcher\icon.png` to ICO using [convertico.com](https://convertico.com)

2. **Build the installer**:
   ```powershell
   cd installer\windows
   .\build-installer.bat
   ```

3. **Find your installer**:
   ```
   dist\windows\MCJE-Launcher-Setup.exe
   ```

That's it! The installer bundles Java 21 and creates pinnable shortcuts.

---

## macOS (5 minutes)

### Prerequisites
1. Xcode Command Line Tools: `xcode-select --install`

### Build Steps

1. **Create the icon**:
   Convert `launcher/src/main/resources/com/skcraft/launcher/icon.png` to ICNS using [cloudconvert.com](https://cloudconvert.com/png-to-icns)
   Save as `installer/macos/resources/icon.icns`

2. **Build the installer**:
   ```bash
   cd installer/macos
   chmod +x build-installer.sh
   ./build-installer.sh
   ```

3. **Find your installer**:
   ```
   dist/macos/MCJE-Launcher-Installer.dmg
   ```

---

## What Gets Installed

| Component | Location |
|-----------|----------|
| Java 21 JRE | `[Install Dir]\jre\` |
| Bootstrap JAR | `[Install Dir]\launcher-bootstrap.jar` |
| Launcher | Desktop shortcut + Start Menu |
| User Data | `%APPDATA%\..\Documents\MCJE\` (Windows) or `~/.MCJE/` (Mac) |

## Next Steps

After building, upload to your DigitalOcean Spaces:
- The installer (for users to download)
- `launcher-fancy.jar` as `launcher.jar` (for auto-updates)
- `latest.json` pointing to the launcher URL
