' MCJE Launcher - VBScript Launcher (Hidden Console)
' This script launches the Java application without showing a console window

Option Explicit

Dim WshShell, fso, scriptDir, javaExe, bootstrapJar, command

Set WshShell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

' Get the directory of this script
scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)

' Define paths
javaExe = scriptDir & "\jre\bin\javaw.exe"
bootstrapJar = scriptDir & "\launcher-bootstrap.jar"

' Verify Java exists
If Not fso.FileExists(javaExe) Then
    MsgBox "Error: Java runtime not found." & vbCrLf & vbCrLf & _
           "Expected location: " & javaExe & vbCrLf & vbCrLf & _
           "Please reinstall the MCJE Launcher.", vbCritical, "MCJE Launcher Error"
    WScript.Quit 1
End If

' Verify bootstrap JAR exists
If Not fso.FileExists(bootstrapJar) Then
    MsgBox "Error: Launcher bootstrap not found." & vbCrLf & vbCrLf & _
           "Expected location: " & bootstrapJar & vbCrLf & vbCrLf & _
           "Please reinstall the MCJE Launcher.", vbCritical, "MCJE Launcher Error"
    WScript.Quit 1
End If

' Build and execute the command
command = """" & javaExe & """ -jar """ & bootstrapJar & """"
WshShell.Run command, 0, False

Set WshShell = Nothing
Set fso = Nothing
