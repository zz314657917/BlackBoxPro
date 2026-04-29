[CmdletBinding()]
param(
    [string]$OutputJar = ''
)

$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)

$sourceRoot = Join-Path $PSScriptRoot 'bc-helper-src'
$sourceFile = Join-Path $sourceRoot 'src/com/blackboxpro/testcellbcbridge/TestCellBcBridgeHelperPlugin.java'
$pluginYaml = Join-Path $sourceRoot 'plugin.yml'
$buildRoot = Join-Path $sourceRoot 'build'
$classesDir = Join-Path $buildRoot 'classes'

if ([string]::IsNullOrWhiteSpace($OutputJar)) {
    $OutputJar = Join-Path $buildRoot 'TestCellBcBridgeHelper.jar'
}

$javac = 'C:/Program Files/Java/jdk1.8.0_481/bin/javac.exe'
$jar = 'C:/Program Files/Java/jdk1.8.0_481/bin/jar.exe'
$apiJar = 'F:/minecraft/test-cells/server-cell-02/catserver.jar'

if (-not (Test-Path -LiteralPath $javac)) { throw "javac not found: $javac" }
if (-not (Test-Path -LiteralPath $jar)) { throw "jar not found: $jar" }
if (-not (Test-Path -LiteralPath $apiJar)) { throw "API jar not found: $apiJar" }

if (Test-Path -LiteralPath $buildRoot) {
    Remove-Item -LiteralPath $buildRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $classesDir -Force | Out-Null

& $javac -encoding UTF-8 -cp $apiJar -d $classesDir $sourceFile
if ($LASTEXITCODE -ne 0) {
    throw 'javac compilation failed.'
}

Copy-Item -LiteralPath $pluginYaml -Destination (Join-Path $classesDir 'plugin.yml') -Force

$outputDir = Split-Path -Parent $OutputJar
if (-not (Test-Path -LiteralPath $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir -Force | Out-Null
}

Push-Location $classesDir
try {
    & $jar cf $OutputJar .
    if ($LASTEXITCODE -ne 0) {
        throw 'jar packaging failed.'
    }
}
finally {
    Pop-Location
}

[pscustomobject]@{
    ok = $true
    outputJar = $OutputJar
    sourceFile = $sourceFile
    apiJar = $apiJar
} | ConvertTo-Json -Depth 5
