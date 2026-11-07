@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0start-facial-windows.ps1" %*
exit /b %ERRORLEVEL%
