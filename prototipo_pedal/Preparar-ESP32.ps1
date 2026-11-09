param(
    [string]$Board = 'esp32:esp32:esp32',
    [switch]$InstallCore
)

$ErrorActionPreference = 'Stop'
$sketch = Join-Path $PSScriptRoot 'firmware\recode_pedal'
$index = 'https://espressif.github.io/arduino-esp32/package_esp32_index.json'

$cliCommand = Get-Command arduino-cli -ErrorAction SilentlyContinue
$cli = if ($cliCommand) { $cliCommand.Source } else { $null }
if (-not $cli) {
    $packages = Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages'
    $bundledCli = Get-ChildItem -Path $packages -Directory -Filter 'ArduinoSA.IDE.stable_*' -ErrorAction SilentlyContinue |
        ForEach-Object { Join-Path $_.FullName 'resources\app\lib\backend\resources\arduino-cli.exe' } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if ($bundledCli) { $cli = $bundledCli }
}

if (-not $cli) {
    Write-Host 'Arduino CLI nao encontrado. Abra o sketch na Arduino IDE.'
    Write-Host "Em Preferencias, adicione a URL do pacote ESP32: $index"
    Write-Host 'No Gerenciador de Placas, instale esp32 by Espressif Systems.'
    Write-Host 'Depois selecione o modelo exato da placa e use Verificar para compilar.'
    exit 0
}

if ($InstallCore) {
    & $cli core update-index --additional-urls $index
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & $cli core install esp32:esp32 --additional-urls $index
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Compilando firmware para $Board ..."
& $cli compile --fqbn $Board --additional-urls $index $sketch
exit $LASTEXITCODE
