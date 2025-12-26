# MCJE Launcher Download Site

This directory contains the download page and upload tools for distributing the MCJE Launcher.

## Files

- `index.html` - Auto-detecting download page
- `upload-to-spaces.sh` - Script to upload files to Digital Ocean Spaces
- `README.md` - This file

## Setup

### 1. Install s3cmd

```bash
brew install s3cmd
```

### 2. Configure s3cmd for Digital Ocean Spaces

```bash
s3cmd --configure
```

When prompted, enter:
- **Access Key**: Your Digital Ocean Spaces Access Key
- **Secret Key**: Your Digital Ocean Spaces Secret Key
- **Default Region**: `sfo3`
- **S3 Endpoint**: `sfo3.digitaloceanspaces.com`
- **DNS-style bucket**: `%(bucket)s.sfo3.digitaloceanspaces.com`
- **Encryption password**: (leave blank)
- **Path to GPG program**: (leave blank)
- **Use HTTPS**: Yes
- **HTTP Proxy**: (leave blank)

Test the connection and save the configuration.

## Usage

### Upload all files to Digital Ocean Spaces

```bash
cd download-site
./upload-to-spaces.sh
```

This will upload:
1. `index.html` - The download page
2. `MCJE-Launcher-Installer.dmg` - macOS installer (if exists)
3. `MCJE-Launcher-Setup.exe` - Windows installer (if exists)

### Access URLs

After uploading, your files will be available at:

- **Download Page**: https://mcje-bucket.sfo3.digitaloceanspaces.com/index.html
- **macOS Installer**: https://mcje-bucket.sfo3.digitaloceanspaces.com/MCJE-Launcher-Installer.dmg
- **Windows Installer**: https://mcje-bucket.sfo3.digitaloceanspaces.com/MCJE-Launcher-Setup.exe

## Share with Friends

Send them this link:
```
https://mcje-bucket.sfo3.digitaloceanspaces.com/index.html
```

The page will automatically detect their OS and show the right download button!

## Manual Upload (Alternative)

If you prefer to use the Digital Ocean web interface:

1. Go to https://cloud.digitalocean.com/spaces
2. Select your `mcje-bucket` Space
3. Upload these files:
   - `index.html`
   - `MCJE-Launcher-Installer.dmg` (from `../dist/macos/`)
   - `MCJE-Launcher-Setup.exe` (from `../dist/windows/`)
4. Set each file's permissions to "Public"

## Notes

- The download page automatically detects if the user is on Windows or macOS and shows the appropriate download button
- Both platform downloads are always available via the "Download for:" links
- macOS users see a note about right-clicking to bypass Gatekeeper on first launch
