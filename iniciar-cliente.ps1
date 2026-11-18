param([string]$Servidor = 'localhost', [int]$Puerto = 3333, [string]$Alias = '')
$ErrorActionPreference = 'Stop'
& java -jar (Join-Path $PSScriptRoot 'conecta2-cliente/Conecta2-Cliente.jar') $Servidor $Puerto $Alias
exit $LASTEXITCODE
