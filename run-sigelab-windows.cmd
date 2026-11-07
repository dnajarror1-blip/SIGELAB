@echo off
powershell.exe -NoLogo -NoProfile -File "%~dp0run-sigelab-windows.ps1" %*
exit /b %ERRORLEVEL%
