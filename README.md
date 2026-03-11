# MCJE Launcher

Custom Minecraft Java Edition modpack launcher built on top of [SKCraft Launcher](https://github.com/SKCraft/Launcher). Dark Minecraft themed UI, bundles Java 21, and updates itself automatically.

## Project Structure

```
launcher/              Core library. Auth, downloads, game launching
launcher-fancy/        The actual UI people see. Dark theme, Substance L&F
launcher-bootstrap/    Tiny updater. Downloads and runs the latest launcher JAR
launcher-builder/      CLI for building modpack zips
creator-tools/         GUI for building modpacks (way easier than the CLI)
installer/             Windows and macOS installer scripts
```

## How it all fits together

```
Installer (ships Java 21 + bootstrap JAR)
  > Bootstrap grabs latest launcher from DigitalOcean Spaces
    > launcher-fancy.jar runs, shows modpack list
      > User hits play, Minecraft launches
```

On first run the bootstrap copies its bundled JRE into the launcher data folder. After that the launcher JAR updates itself, so you only need to rebuild the installer if you change the bootstrap or the bundled Java version.

## Building

You need JDK 8+ installed. Gradle wrapper is included, you don't need to install Gradle.

```bash
./gradlew build
```

JARs show up in `<module>/build/libs/`. They're all fat JARs (shadow plugin), so everything is bundled.

| Module | JAR | Main Class |
|--------|-----|------------|
| launcher | launcher.jar | com.skcraft.launcher.Launcher |
| launcher-fancy | launcher-fancy.jar | com.skcraft.launcher.FancyLauncher |
| launcher-bootstrap | launcher-bootstrap.jar | com.skcraft.launcher.Bootstrap |
| launcher-builder | launcher-builder.jar | com.skcraft.launcher.builder.PackageBuilder |
| creator-tools | creator-tools.jar | com.skcraft.launcher.creator.Creator |

## Config files you care about

### `launcher/src/main/resources/com/skcraft/launcher/launcher.properties`

Where the launcher looks for modpacks, news, and updates. Also has the Microsoft OAuth client ID (registered in Azure AD).

```properties
newsUrl=https://mcje-bucket.sfo3.digitaloceanspaces.com/news.html?version=%s
packageListUrl=https://mcje-bucket.sfo3.digitaloceanspaces.com/packages.json?key=%s
selfUpdateUrl=https://mcje-bucket.sfo3.digitaloceanspaces.com/latest.json
microsoftClientId=d18bb4d8-a27f-4451-a87f-fe6de4436813
```

### `launcher-bootstrap/src/main/resources/com/skcraft/launcher/bootstrap.properties`

Where the bootstrap stores user data and where it checks for launcher updates.

```properties
homeFolderWindows=MCJE
homeFolderLinux=MCJE
homeFolder=.MCJE
launcherClass=com.skcraft.launcher.FancyLauncher
latestUrl=https://mcje-bucket.sfo3.digitaloceanspaces.com/latest.json
```

### Where user data lives

| Platform | Path |
|----------|------|
| Windows | `%LOCALAPPDATA%\MCJE` |
| macOS | `~/.MCJE` |
| Linux | `$XDG_DATA_HOME/MCJE` or `~/.local/share/MCJE` |

## Hosting your files

This project is currently set up to use a DigitalOcean Spaces bucket, but you can use anything that serves static files over HTTPS (AWS S3, Cloudflare R2, GitHub Pages, your own web server, whatever). The launcher doesn't care where the files live, it just hits the URLs in the config.

To switch to a different host, update the URLs in these two files and rebuild:

1. `launcher/src/main/resources/com/skcraft/launcher/launcher.properties` (newsUrl, packageListUrl, selfUpdateUrl)
2. `launcher-bootstrap/src/main/resources/com/skcraft/launcher/bootstrap.properties` (latestUrl)

Also update the `url` inside your `latest.json` to point to wherever you're hosting the launcher JAR.

### What needs to be hosted

Your host needs these files:

```
latest.json        Tells the bootstrap what version to download
launcher.jar       The launcher-fancy shadow JAR
packages.json      List of available modpacks
news.html          Shows up in the launcher news tab
[modpack files]    Whatever modpacks you've built
```

`latest.json` looks like this:

```json
{
  "version": "4.8",
  "url": "https://mcje-bucket.sfo3.digitaloceanspaces.com/launcher.jar"
}
```

## Pushing a launcher update to users

1. `./gradlew :launcher-fancy:build`
2. Upload `launcher-fancy/build/libs/launcher-fancy.jar` to the bucket as `launcher.jar`
3. Bump the version in `latest.json`
4. Done. Users pick it up next time they open the launcher

## Building modpacks

Use the GUI, it's way easier:

```bash
java -jar creator-tools/build/libs/creator-tools.jar
```

Or the CLI if you want:

```bash
java -jar launcher-builder/build/libs/launcher-builder.jar \
  --version "1.0" \
  --manifest-dest output/manifest.json \
  -input src/
```

Modpack folder layout:

```
src/
  config/
  mods/
  resourcepacks/
loaders/          Drop Forge/Fabric/LiteLoader installers here
```

## Building installers

Full instructions in [installer/README.md](installer/README.md).

Short version:
- **Windows:** Need Inno Setup 6 installed. Run `installer/windows/build-installer.ps1` in PowerShell
- **macOS:** Run `installer/macos/build-installer.sh`

Both download Java 21, build the bootstrap, and spit out an installer.
- Windows: `dist/windows/MCJE-Launcher-Setup.exe`
- macOS: `dist/macos/MCJE-Launcher-Installer.dmg`

## When you inevitably forget how all this works

1. `./gradlew build` to make sure everything compiles
2. `launcher-fancy` = the launcher people actually use (dark Minecraft UI)
3. `launcher-bootstrap` = the thing the installer ships, it just downloads and runs launcher-fancy
4. All the URLs live in `launcher.properties` and `bootstrap.properties` (see above)
5. The Microsoft OAuth client ID is in `launcher.properties`, it's registered in Azure AD
6. To push a launcher update: build launcher-fancy, upload JAR to bucket, bump `latest.json`
7. To push modpack changes: build with creator-tools, upload files, update `packages.json`
8. You only need to rebuild installers if the bootstrap code or Java version changes

## License

GNU Lesser General Public License, version 3. See [LICENSE.txt](LICENSE.txt).
