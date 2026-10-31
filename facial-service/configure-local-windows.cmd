@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0configure-local-windows.ps1" %*
exit /b %ERRORLEVEL%
