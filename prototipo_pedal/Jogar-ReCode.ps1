$ErrorActionPreference = 'Stop'
$gameRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$desktop = Join-Path $gameRoot 'desktop'
$jarRoot = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1'

if (-not (Get-Command java -ErrorAction SilentlyContinue) -or
    -not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'Instale o JDK 17 para testar o jogo.'
}
if (-not (Test-Path $jarRoot)) {
    throw 'Dependencias ausentes. Execute gradle run -Pkeyboard=true na pasta desktop para baixá-las.'
}

$jars = @(Get-ChildItem $jarRoot -Recurse -Filter '*.jar' | Where-Object {
    $path = $_.FullName.Replace('\', '/')
    $path -match '/gdx/1\.14\.2/|/gdx-backend-lwjgl3/1\.14\.2/|/gdx-platform/1\.14\.2/|/gdx-jnigen-loader/2\.5\.2/|/jSerialComm/2\.11\.4/|/jlayer/1\.0\.1-gdx/|/org\.lwjgl/lwjgl[^/]*/3\.3\.3/'
} | ForEach-Object FullName)
$required = @('gdx-1.14.2.jar', 'gdx-backend-lwjgl3-1.14.2.jar',
    'gdx-platform-1.14.2-natives-desktop.jar', 'gdx-jnigen-loader-2.5.2.jar',
    'jSerialComm-2.11.4.jar', 'jlayer-1.0.1-gdx.jar',
    'lwjgl-3.3.3.jar', 'lwjgl-glfw-3.3.3.jar', 'lwjgl-opengl-3.3.3.jar')
foreach ($name in $required) {
    if (-not ($jars | Where-Object { (Split-Path $_ -Leaf) -eq $name })) {
        throw "Dependencia ausente: $name. Execute gradle run -Pkeyboard=true na pasta desktop."
    }
}

$classes = Join-Path $desktop 'build\playtest-classes'
$resources = Join-Path $desktop 'src\main\resources'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$classpath = (@($classes, $resources) + $jars) -join ';'
$sources = @(Get-ChildItem (Join-Path $desktop 'src\main\java') -Recurse -Filter '*.java' |
    ForEach-Object FullName)
Push-Location $desktop
try {
    Write-Host 'Preparando ReCode...'
    & javac -encoding UTF-8 -cp $classpath -d $classes $sources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host 'Modo teclado: Enter para jogar, F alterna o giro, setas escolhem o lado, Espaco pausa.'
    & java '-Drecode.keyboard=true' '-Drecode.phase=campaign' -cp $classpath br.edu.recode.DesktopLauncher
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
