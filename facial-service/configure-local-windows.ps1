# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
    throw 'Falta .venv de Windows. Ejecute setup-facial-windows.ps1.'
}
# Invocacion directa: conserva stdin/TTY y getpass oculta la contrasena SQL.
& $python (Join-Path $PSScriptRoot 'scripts\configure_local.py')
if ($LASTEXITCODE -ne 0) { throw 'Configuracion no completada. No se muestran datos privados.' }
