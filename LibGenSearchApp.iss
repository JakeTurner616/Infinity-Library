[Setup]
AppName=LibGenSearchApp
AppVersion=1.0.5
DefaultDirName={pf}\LibGenSearchApp
DefaultGroupName=LibGenSearchApp
OutputDir=Output
OutputBaseFilename=LibGenSearchApp-setup
Compression=lzma2
SolidCompression=yes

[Files]
Source: "C:\Users\jaked\Documents\simple-libgen-desktop\LibGenSearchApp.exe"; DestDir: "{app}"
Source: "C:\Users\jaked\Documents\simple-libgen-desktop\jdk-21.0.1+12-jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{group}\LibGenSearchApp"; Filename: "{app}\LibGenSearchApp.exe"