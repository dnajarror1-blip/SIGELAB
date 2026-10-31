@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0setup-facial-windows.ps1" %*
exit /b %ERRORLEVEL%
