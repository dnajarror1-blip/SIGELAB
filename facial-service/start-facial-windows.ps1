# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
    throw 'Falta .venv de Windows. Ejecute prepare-demo-windows.ps1.'
}
Push-Location -LiteralPath $PSScriptRoot
try {
    & $python (Join-Path $PSScriptRoot 'scripts\doctor.py') --configuration-only
    if ($LASTEXITCODE -ne 0) { throw 'No se inicia FaceID: revise configuracion y ODBC con prepare-demo-windows.ps1.' }
    # Primer plano: mantener esta terminal abierta. Un worker, sin reload.
    & $python -m uvicorn app.main:app --host 127.0.0.1 --port 8765 --workers 1
    if ($LASTEXITCODE -ne 0) { throw 'El servicio facial termino con error.' }
} finally {
    Pop-Location
}
