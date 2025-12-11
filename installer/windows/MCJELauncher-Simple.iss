; MCJE Launcher Simple Installer for Inno Setup
; This version doesn't require Launch4j - uses a batch file launcher
; Creates pinnable shortcuts using a workaround

#define MyAppName "MCJE Launcher"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "MCJE"
#define MyAppURL "https://mcje-bucket.sfo3.digitaloceanspaces.com/"

[Setup]
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=..\..\dist\windows
OutputBaseFilename=MCJE-Launcher-Setup
SetupIconFile=resources\icon.ico
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog
UninstallDisplayIcon={app}\resources\icon.ico
; For a quick test without icon, comment out SetupIconFile and UninstallDisplayIcon above
UninstallDisplayName={#MyAppName}
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: checked

[Files]
; Java 21 Runtime (bundled)
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

; Launcher Bootstrap JAR
Source: "..\..\launcher-bootstrap\build\libs\launcher-bootstrap-*.jar"; DestDir: "{app}"; DestName: "launcher-bootstrap.jar"; Flags: ignoreversion

; Launcher batch script
Source: "resources\launcher.bat"; DestDir: "{app}"; Flags: ignoreversion

; Hidden VBS launcher (for pinnable shortcuts)
Source: "resources\launcher.vbs"; DestDir: "{app}"; Flags: ignoreversion

; Icon
Source: "resources\icon.ico"; DestDir: "{app}\resources"; Flags: ignoreversion

[Icons]
; Start Menu shortcut - uses wscript to run VBS without console
Name: "{autoprograms}\{#MyAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launcher.vbs"""; IconFilename: "{app}\resources\icon.ico"; Comment: "Launch MCJE Minecraft Launcher"; AppUserModelID: "MCJE.Launcher"

; Desktop shortcut
Name: "{autodesktop}\{#MyAppName}"; Filename: "wscript.exe"; Parameters: """{app}\launcher.vbs"""; IconFilename: "{app}\resources\icon.ico"; Tasks: desktopicon; Comment: "Launch MCJE Minecraft Launcher"; AppUserModelID: "MCJE.Launcher"

[Run]
Filename: "wscript.exe"; Parameters: """{app}\launcher.vbs"""; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Registry]
Root: HKCU; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "Version"; ValueData: "{#MyAppVersion}"
; Register AppUserModelID for pinning
Root: HKCU; Subkey: "Software\Classes\MCJE.Launcher"; ValueType: string; ValueName: ""; ValueData: "MCJE Launcher"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\Classes\MCJE.Launcher\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\resources\icon.ico"
