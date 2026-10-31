# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

Push-Location -LiteralPath $PSScriptRoot
try {
    $python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
    $versionCheck = 'import sys; sys.exit(0 if (3,12) <= sys.version_info[:2] <= (3,14) else 2)'
    if (-not (Test-Path -LiteralPath $python -PathType Leaf)) {
        if (Test-Path -LiteralPath (Join-Path $PSScriptRoot '.venv')) {
            throw '.venv no contiene Python de Windows. Revise el entorno manualmente; no se sobrescribio.'
        }
        if (-not (Get-Command py -ErrorAction SilentlyContinue)) {
            throw 'Falta Python 3.12-3.14 con Python Launcher (py). Instale Python 3.12 x64 y vuelva a ejecutar este script.'
        }
        $selected = $null
        foreach ($version in @('-3.12', '-3.13', '-3.14')) {
            # PS 5.1 puede tratar stderr del launcher como error terminante.
            $ErrorActionPreference = 'Continue'
            try { & py $version -c $versionCheck 2>$null }
            finally { $ErrorActionPreference = 'Stop' }
            if ($LASTEXITCODE -eq 0) { $selected = $version; break }
        }
        if ($null -eq $selected) {
            throw 'Falta Python compatible: se admite solamente 3.12-3.14. Instale Python 3.12 x64 con py.'
        }
        & py $selected -m venv (Join-Path $PSScriptRoot '.venv')
        if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear .venv.' }
    }
    & $python -c $versionCheck
    if ($LASTEXITCODE -ne 0) { throw 'El Python de .venv debe ser 3.12-3.14. Revise el entorno manualmente.' }
    & $python (Join-Path $PSScriptRoot 'scripts\doctor.py') --dependencies-only
    if ($LASTEXITCODE -ne 0) {
        & $python -m pip install -r (Join-Path $PSScriptRoot 'requirements.txt')
        if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias. Revise el error de pip.' }
    }
    & $python -m pip check
    if ($LASTEXITCODE -ne 0) { throw 'pip check detecto dependencias incompatibles.' }
    & $python (Join-Path $PSScriptRoot 'scripts\doctor.py') --dependencies-only
    if ($LASTEXITCODE -ne 0) { throw 'El diagnostico de dependencias no paso.' }
    Write-Host 'Dependencias preparadas. Ejecute configure-local-windows.ps1 y prepare-demo-windows.ps1.'
} finally {
    Pop-Location
}
