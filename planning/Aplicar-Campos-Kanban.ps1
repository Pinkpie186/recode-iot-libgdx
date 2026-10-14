param([switch]$Aplicar)

$ErrorActionPreference = 'Stop'
$gh = 'C:\Program Files\GitHub CLI\gh.exe'
$csvPath = Join-Path $PSScriptRoot 'ReCode_Kanban_campos.csv'
$owner = 'Pinkpie186'
$projectNumber = 8
$repo = 'Pinkpie186/recode-iot-libgdx'

if (-not (Test-Path -LiteralPath $gh)) { throw 'GitHub CLI não encontrado.' }
$rows = @(Import-Csv -LiteralPath $csvPath -Encoding UTF8)
if ($rows.Count -ne 23 -or @($rows.issue | Sort-Object -Unique).Count -ne 23) {
    throw 'O planejamento deve conter exatamente 23 Issues distintas.'
}

function Invoke-GhJson {
    param([string[]]$Arguments)
    $output = & $gh @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Falha no GitHub CLI: $output" }
    $output | ConvertFrom-Json
}

$project = Invoke-GhJson -Arguments @('project', 'view', "$projectNumber", '--owner', $owner, '--format', 'json')
if ($project.title -ne 'ReCode Kanban' -or $project.owner.login -ne $owner) {
    throw 'O Project #8 não é o ReCode Kanban esperado.'
}
$fieldsResult = Invoke-GhJson -Arguments @('project', 'field-list', "$projectNumber", '--owner', $owner, '--format', 'json', '--limit', '100')
$itemsResult = Invoke-GhJson -Arguments @('project', 'item-list', "$projectNumber", '--owner', $owner, '--format', 'json', '--limit', '100')
$fields = @{}
foreach ($field in $fieldsResult.fields) { $fields[$field.name] = $field }
foreach ($name in @('Priority', 'Size', 'Estimate', 'Start date', 'Target date')) {
    if (-not $fields.ContainsKey($name)) { throw "Campo ausente no Project: $name" }
}
$items = @{}
foreach ($item in $itemsResult.items) {
    if ($item.content.repository -eq $repo -and $item.content.type -eq 'Issue') {
        $items[[int]$item.content.number] = $item
    }
}
if ($items.Count -ne 23) { throw "Esperadas 23 Issues no Project; encontradas $($items.Count)." }

function Get-OptionId {
    param([string]$FieldName, [string]$OptionName)
    $option = @($fields[$FieldName].options | Where-Object { $_.name -eq $OptionName })
    if ($option.Count -ne 1) { throw "Opção '$OptionName' não encontrada no campo '$FieldName'." }
    $option[0].id
}

foreach ($row in $rows) {
    $issueNumber = [int]$row.issue
    $item = $items[$issueNumber]
    if ($null -eq $item -or $item.title -ne $row.titulo) {
        throw "Issue #$issueNumber ausente ou com título diferente no Project."
    }
    if ([datetime]$row.inicio -gt [datetime]$row.meta -or [datetime]$row.meta -gt [datetime]'2026-11-05') {
        throw "Datas inválidas na Issue #$issueNumber."
    }
    if ($row.prioridade -notin @('Alta', 'Média') -or $row.tamanho -notin @('S', 'M', 'L')) {
        throw "Prioridade ou tamanho inválido na Issue #$issueNumber."
    }
}

Write-Host 'Plano validado: 23 Issues no ReCode Kanban; Status atuais serão preservados.'
if (-not $Aplicar) { exit 0 }

foreach ($row in $rows) {
    $item = $items[[int]$row.issue]
    $priority = if ($row.prioridade -eq 'Alta') { 'P1' } else { 'P2' }
    $updates = @(
        @{ Name = 'Priority'; Flag = '--single-select-option-id'; Value = (Get-OptionId 'Priority' $priority) },
        @{ Name = 'Size'; Flag = '--single-select-option-id'; Value = (Get-OptionId 'Size' $row.tamanho) },
        @{ Name = 'Estimate'; Flag = '--number'; Value = $row.estimativa_horas },
        @{ Name = 'Start date'; Flag = '--date'; Value = $row.inicio },
        @{ Name = 'Target date'; Flag = '--date'; Value = $row.meta }
    )
    foreach ($update in $updates) {
        $editArguments = @('project', 'item-edit', '--project-id', $project.id, '--id', $item.id,
                  '--field-id', $fields[$update.Name].id, $update.Flag, "$($update.Value)")
        $output = & $gh @editArguments 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "Falha na Issue #$($row.issue), campo $($update.Name): $output"
        }
    }
    Write-Host "Atualizada Issue #$($row.issue): P$([int]($priority.Substring(1))), $($row.tamanho), $($row.estimativa_horas) h, $($row.inicio) a $($row.meta)"
}
Write-Host 'Concluído: cinco campos definidos em cada uma das 23 Issues; Status preservados.'
