@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0prepare-demo-windows.ps1" %*
exit /b %ERRORLEVEL%
