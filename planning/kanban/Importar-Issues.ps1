param(
    [switch]$Executar,
    [string]$Token = $env:GH_TOKEN
)

$ErrorActionPreference = 'Stop'
$repo = 'Pinkpie186/recode-iot-libgdx'
$baseUrl = "https://api.github.com/repos/$repo"
$cardsPath = Join-Path $PSScriptRoot 'cartoes.json'

if (-not (Test-Path -LiteralPath $cardsPath)) {
    throw 'Arquivo cartoes.json não encontrado ao lado do importador.'
}

$cards = Get-Content -LiteralPath $cardsPath -Raw -Encoding UTF8 | ConvertFrom-Json
$cards = @($cards)
if ($cards.Count -ne 23) {
    throw "O pacote deve conter 23 cartões; encontrados: $($cards.Count)."
}

$titles = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($card in $cards) {
    if ([string]::IsNullOrWhiteSpace($card.titulo) -or
        [string]::IsNullOrWhiteSpace($card.area) -or
        $card.prioridade -notin @('Alta', 'Média') -or
        @($card.checklist).Count -lt 1) {
        throw 'Há um cartão sem título, área, prioridade válida ou checklist.'
    }
    if (-not $titles.Add([string]$card.titulo)) {
        throw "Título duplicado no pacote: $($card.titulo)"
    }
}
if ($cards[0].titulo -ne 'Definir os requisitos e escopo do ReCode') {
    throw 'O primeiro cartão deve corresponder à Issue #1 existente.'
}

Write-Host "Pacote válido: $($cards.Count) cartões para $repo."
if (-not $Executar) {
    Write-Host 'Prévia apenas: nenhuma Issue foi criada ou alterada.'
    Write-Host 'Para importar as 22 tarefas restantes, execute novamente com -Executar.'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Token)) {
    $Token = $env:GITHUB_TOKEN
}
if ([string]::IsNullOrWhiteSpace($Token) -and (Get-Command gh -ErrorAction SilentlyContinue)) {
    $gitHubCliToken = & gh auth token 2>$null
    if ($LASTEXITCODE -eq 0 -and $gitHubCliToken) {
        $Token = ($gitHubCliToken -join '').Trim()
    }
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    $secureToken = Read-Host -Prompt 'Token GitHub (não aparece na tela)' -AsSecureString
    $tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureToken)
    try {
        $Token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer)
        $secureToken.Dispose()
    }
}
if ([string]::IsNullOrWhiteSpace($Token)) {
    throw 'Informe um token GitHub com permissão Issues: Read and write.'
}

$headers = @{
    Authorization = "Bearer $Token"
    Accept = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent' = 'ReCode-Kanban-Importer'
}

function Invoke-GitHubApi {
    param([string]$Method, [string]$Url, [object]$Payload = $null)
    $params = @{ Method = $Method; Uri = $Url; Headers = $headers; ErrorAction = 'Stop' }
    if ($null -ne $Payload) {
        $params.ContentType = 'application/json; charset=utf-8'
        $params.Body = ConvertTo-Json -InputObject $Payload -Depth 10 -Compress
    }
    Invoke-RestMethod @params
}

$firstIssue = Invoke-GitHubApi -Method GET -Url "$baseUrl/issues/1"
if ($firstIssue.title -ne $cards[0].titulo -or $null -ne $firstIssue.pull_request) {
    throw 'A Issue #1 não corresponde ao primeiro cartão. Importação interrompida sem alterá-la.'
}

$existingTitles = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
$page = 1
do {
    $batch = Invoke-GitHubApi -Method GET -Url "$baseUrl/issues?state=all&per_page=100&page=$page"
    $batch = @($batch)
    foreach ($issue in $batch) {
        if ($null -eq $issue.pull_request) {
            [void]$existingTitles.Add([string]$issue.title)
        }
    }
    $page++
} while ($batch.Count -eq 100)

$created = 0
$skipped = 1
Write-Host 'Issue #1 preservada; o importador não a modifica.'
foreach ($card in $cards | Select-Object -Skip 1) {
    if ($existingTitles.Contains([string]$card.titulo)) {
        Write-Host "Já existe, ignorada: $($card.titulo)"
        $skipped++
        continue
    }

    $lines = @("Área: $($card.area)", "Prioridade: $($card.prioridade)", '', 'Checklist:')
    $lines += @($card.checklist | ForEach-Object { "- [ ] $_" })
    $body = $lines -join "`n"
    $newIssue = Invoke-GitHubApi -Method POST -Url "$baseUrl/issues" -Payload @{ title = $card.titulo; body = $body }
    Write-Host "Criada #$($newIssue.number): $($card.titulo)"
    [void]$existingTitles.Add([string]$card.titulo)
    $created++
}

Write-Host "Concluído: $created criada(s), $skipped já existente(s), total de 23 cartões."
