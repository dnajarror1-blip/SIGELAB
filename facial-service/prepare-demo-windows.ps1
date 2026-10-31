# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

& (Join-Path $PSScriptRoot 'setup-facial-windows.ps1')
$root = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $root '.env') -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $PSScriptRoot '.env') -PathType Leaf)) {
    throw 'NO LISTO: ejecute configure-local-windows.ps1 para preparar ambos .env.'
}
$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
Push-Location -LiteralPath $PSScriptRoot
try {
    & $python (Join-Path $PSScriptRoot 'scripts\doctor.py') --database --model --camera
    if ($LASTEXITCODE -ne 0) { throw 'NO LISTO: corrija el diagnostico y repita la preparacion.' }
} finally {
    Pop-Location
}
Write-Host 'READY FOR DEMO'
