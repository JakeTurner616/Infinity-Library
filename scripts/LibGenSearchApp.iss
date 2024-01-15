[Setup]
AppName=LibGenSearchApp
AppVersion=1.0.6
DefaultDirName={pf}\LibGenSearchApp
DefaultGroupName=LibGenSearchApp
OutputDir=Output
OutputBaseFilename=LibGenSearchApp-setup
Compression=lzma2
SolidCompression=yes

[Files]
Source: "C:\Users\jaked\Documents\simple-libgen-desktop\LibGenSearchApp.exe"; DestDir: "{app}"
Source: "C:\Users\jaked\Documents\simple-libgen-desktop\jdk-21.0.1+12-jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs createallsubdirs

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Icons]
Name: "{group}\LibGenSearchApp"; Filename: "{app}\LibGenSearchApp.exe"
Name: "{userdesktop}\LibGenSearchApp"; Filename: "{app}\LibGenSearchApp.exe"; Tasks: desktopicon
