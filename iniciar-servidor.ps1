param([int]$Puerto = 3333, [string]$Datos = '')
$ErrorActionPreference = 'Stop'
if (-not $Datos) { $Datos = Join-Path $PSScriptRoot 'conecta2-servidor/datos' }
& java -jar (Join-Path $PSScriptRoot 'conecta2-servidor/Conecta2-Servidor.jar') $Puerto $Datos
exit $LASTEXITCODE
