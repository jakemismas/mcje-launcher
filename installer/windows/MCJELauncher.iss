; MCJE Launcher Installer Script for Inno Setup
; This script creates a Windows installer that:
; 1. Bundles Java 21 (Adoptium Temurin JRE)
; 2. Installs the launcher bootstrap
; 3. Creates Start Menu and Desktop shortcuts

#define MyAppName "MCJE Launcher"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "MCJE"
#define MyAppURL "https://mcje-bucket.sfo3.digitaloceanspaces.com/"
#define MyAppExeName "MCJE Launcher.exe"

[Setup]
; Application metadata
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

; Installation settings
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
; LicenseFile=..\..\LICENSE.txt  ; Uncomment to show license during install
OutputDir=..\..\dist\windows
OutputBaseFilename=MCJE-Launcher-Setup
SetupIconFile=resources\icon.ico
; For a quick test without icon, comment out SetupIconFile above
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern

; Privilege settings - install for current user by default
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog

; Uninstaller
UninstallDisplayIcon={app}\resources\icon.ico
UninstallDisplayName={#MyAppName}

; Architecture
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"

[Files]
; Java 21 Runtime (bundled)
Source: "jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

; Launcher Bootstrap JAR (copied by build script to fixed name)
Source: "launcher-bootstrap.jar"; DestDir: "{app}"; Flags: ignoreversion

; Launcher executable wrapper
Source: "resources\launcher.exe"; DestDir: "{app}"; DestName: "{#MyAppExeName}"; Flags: ignoreversion

; Icon and resources
Source: "resources\icon.ico"; DestDir: "{app}\resources"; Flags: ignoreversion

[Icons]
; Start Menu shortcut
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\resources\icon.ico"; Comment: "Launch MCJE Minecraft Launcher"

; Desktop shortcut (if selected)
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\resources\icon.ico"; Tasks: desktopicon; Comment: "Launch MCJE Minecraft Launcher"

[Run]
; Option to run after install
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Registry]
; Register application for Add/Remove Programs
Root: HKCU; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "InstallPath"; ValueData: "{app}"; Flags: uninsdeletekey
Root: HKCU; Subkey: "Software\{#MyAppPublisher}\{#MyAppName}"; ValueType: string; ValueName: "Version"; ValueData: "{#MyAppVersion}"

[UninstallDelete]
; Clean up launcher data directory on uninstall (optional - commented out to preserve user data)
; Type: filesandordirs; Name: "{userappdata}\..\Documents\MCJE"

[Code]
// Custom code for installation logic
var
  DownloadPage: TDownloadWizardPage;

function OnDownloadProgress(const Url, FileName: String; const Progress, ProgressMax: Int64): Boolean;
begin
  if Progress = ProgressMax then
    Log(Format('Successfully downloaded file to {tmp}: %s', [FileName]));
  Result := True;
end;

procedure InitializeWizard;
begin
  DownloadPage := CreateDownloadPage(SetupMessage(msgWizardPreparing), SetupMessage(msgPreparingDesc), @OnDownloadProgress);
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;
end;

// Check if running on 64-bit Windows
function IsX64: Boolean;
begin
  Result := Is64BitInstallMode;
end;

// Custom messages
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    Log('Installation completed successfully!');
  end;
end;
