$ErrorActionPreference = 'Stop'
$gh = 'C:\Program Files\GitHub CLI\gh.exe'
$rows = @(Import-Csv -LiteralPath (Join-Path $PSScriptRoot 'ReCode_Kanban_campos.csv') -Encoding UTF8)
$lines = & $gh project item-list 8 --owner Pinkpie186 --limit 100 --field Priority --field Size --field Estimate --field 'Start date' --field 'Target date'
if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar campos do Project.' }
$items = @{}
foreach ($line in $lines) {
    $parts = $line -split "`t"
    if ($parts.Count -ne 10) { throw "Linha inesperada na consulta: $line" }
    $items[[int]$parts[2]] = $parts
}
if ($items.Count -ne 23) { throw "Esperados 23 cartões; encontrados $($items.Count)." }

$json = & $gh project item-list 8 --owner Pinkpie186 --limit 100 --format json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Falha ao consultar Status do Project.' }
$statuses = @{}
foreach ($item in $json.items) { $statuses[[int]$item.content.number] = $item.status }

foreach ($row in $rows) {
    $number = [int]$row.issue
    $actual = $items[$number]
    $priority = if ($row.prioridade -eq 'Alta') { 'P1' } else { 'P2' }
    if ($null -eq $actual -or $actual[1] -ne $row.titulo -or $actual[5] -ne $priority -or
        $actual[6] -ne $row.tamanho -or [double]$actual[7] -ne [double]$row.estimativa_horas -or
        $actual[8] -ne $row.inicio -or $actual[9] -ne $row.meta -or
        $statuses[$number] -ne $row.status) {
        throw "Campo divergente na Issue #$number."
    }
}
Write-Host 'Verificados no GitHub: 23 cartões, 115 campos preenchidos, Status preservados.'
