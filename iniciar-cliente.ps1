param([string]$Alias = '')
$ErrorActionPreference = 'Stop'
& java -jar (Join-Path $PSScriptRoot 'conecta2-cliente/Cliente.jar') $Alias
exit $LASTEXITCODE
