@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0enroll-user-windows.ps1" %*
exit /b %ERRORLEVEL%
