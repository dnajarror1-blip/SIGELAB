# Equivalente a --username o --id; no crea usuarios ni reemplaza perfiles.
[CmdletBinding(DefaultParameterSetName = 'Username')]
param(
    [Parameter(Mandatory = $true, ParameterSetName = 'Username')]
    [ValidateNotNullOrEmpty()]
    [string]$Username,
    [Parameter(Mandatory = $true, ParameterSetName = 'Id')]
    [ValidateRange(1, 2147483647)]
    [int]$Id
)
# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
    throw 'Falta .venv de Windows. Ejecute setup-facial-windows.ps1.'
}
$enrollmentArgs = @('--username', $Username)
if ($PSCmdlet.ParameterSetName -eq 'Id') { $enrollmentArgs = @('--id', [string]$Id) }
Push-Location -LiteralPath $PSScriptRoot
try {
    & $python (Join-Path $PSScriptRoot 'scripts\enroll_user.py') @enrollmentArgs
    if ($LASTEXITCODE -ne 0) { throw 'Enrollment no completado; no se reintentara automaticamente.' }
} finally {
    Pop-Location
}
