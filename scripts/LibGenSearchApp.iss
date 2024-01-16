[Setup]
AppName=Infinity Library
AppVersion=1.1.0
DefaultDirName={pf}\InfinityLibrary
DefaultGroupName=serverboi.org
OutputDir=Inno-Output
OutputBaseFilename=InfinityLibrary-setup
Compression=lzma2
SolidCompression=yes

[Files]
Source: "C:\Users\jaked\Documents\Infinity-Library\target\InfinityLibrary.exe"; DestDir: "{app}"
Source: "C:\Users\jaked\Documents\Infinity-Library\jdk-21.0.1+12-jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs createallsubdirs

[Tasks]
Name: "desktopicon"; Description: "Create a &desktop icon"; GroupDescription: "Additional icons:"; Flags: unchecked

[Icons]
Name: "{group}\serverboi.org"; Filename: "{app}\InfinityLibrary.exe"
Name: "{userdesktop}\LibGenSearchApp"; Filename: "{app}\LibGenSearchApp.exe"; Tasks: desktopicon