# Compatible con Windows PowerShell 5.1 y PowerShell 7.
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$javaCommand = Get-Command java -CommandType Application -ErrorAction SilentlyContinue
if ($null -eq $javaCommand) { throw 'Falta JDK 21. Configure java en PATH y JAVA_HOME.' }
$java = $javaCommand.Source
# --version escribe en stdout, tambien en Windows PowerShell 5.1.
$javaVersion = & $java --version | Out-String
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch '(?m)^(?:openjdk|java)\s+21(?:[.\s+]|$)') {
    throw 'Se requiere Java 21 en PATH.'
}
if ($env:JAVA_HOME) {
    $homeJava = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (-not (Test-Path -LiteralPath $homeJava -PathType Leaf)) { throw 'JAVA_HOME debe apuntar al JDK 21.' }
    $homeVersion = & $homeJava --version | Out-String
    if ($LASTEXITCODE -ne 0 -or $homeVersion -notmatch '(?m)^(?:openjdk|java)\s+21(?:[.\s+]|$)') {
        throw 'Maven Wrapper necesita JAVA_HOME apuntando al JDK 21.'
    }
}
Push-Location -LiteralPath $PSScriptRoot
try {
    # compile es incremental; no ejecuta pruebas. Maven genera el separador nativo ;.
    & (Join-Path $PSScriptRoot 'mvnw.cmd') '-DskipTests' 'compile' 'dependency:build-classpath' '-Dmdep.includeScope=runtime' '-Dmdep.outputFile=target/runtime-classpath-windows.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Fallo Maven Wrapper; no se iniciara SIGELAB.' }
    $runtime = (Get-Content -LiteralPath (Join-Path $PSScriptRoot 'target\runtime-classpath-windows.txt') -Raw).Trim()
    $classes = Join-Path $PSScriptRoot 'target\classes'
    $classpath = $classes + ';' + $runtime
    & $java '-cp' $classpath 'sigelab.Main'
    if ($LASTEXITCODE -ne 0) { throw 'SIGELAB termino con error.' }
} finally {
    Pop-Location
}
