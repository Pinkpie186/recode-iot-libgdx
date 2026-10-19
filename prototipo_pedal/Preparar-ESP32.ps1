param(
    [string]$Board = 'esp32:esp32:esp32',
    [switch]$InstallCore
)

$ErrorActionPreference = 'Stop'
$sketch = Join-Path $PSScriptRoot 'firmware\recode_pedal'
$index = 'https://espressif.github.io/arduino-esp32/package_esp32_index.json'

if (-not (Get-Command arduino-cli -ErrorAction SilentlyContinue)) {
    Write-Host 'Arduino CLI nao encontrado. Abra o sketch na Arduino IDE.'
    Write-Host "Em Preferencias, adicione a URL do pacote ESP32: $index"
    Write-Host 'No Gerenciador de Placas, instale esp32 by Espressif Systems.'
    Write-Host 'Depois selecione o modelo exato da placa e use Verificar para compilar.'
    exit 0
}

if ($InstallCore) {
    & arduino-cli core update-index --additional-urls $index
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & arduino-cli core install esp32:esp32 --additional-urls $index
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Compilando firmware para $Board ..."
& arduino-cli compile --fqbn $Board --additional-urls $index $sketch
exit $LASTEXITCODE
