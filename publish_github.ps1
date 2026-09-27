$ErrorActionPreference = 'Stop'

$repo = 'fraschizzato/MioTimer'
$description = 'Offline Android timer with random intervals, countdowns, reusable workout sequences and a one-tap home-screen widget.'
$topics = @(
    'android',
    'kotlin',
    'jetpack-compose',
    'timer',
    'countdown',
    'workout-timer',
    'offline-first',
    'android-widget'
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw 'Git is not installed or is not available in PATH.'
}
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'GitHub CLI (gh) is not installed or is not available in PATH.'
}

& gh auth status
if ($LASTEXITCODE -ne 0) {
    throw 'GitHub CLI is not authenticated. Run: gh auth login'
}

if (-not (Test-Path '.git')) {
    & git init
    if ($LASTEXITCODE -ne 0) { throw 'git init failed.' }
}

& git branch -M main
& git add .

Write-Host ''
Write-Host 'Files staged for the initial public release:' -ForegroundColor Cyan
& git status --short
Write-Host ''

$hasHead = (& git rev-parse --verify HEAD 2>$null)
if ($LASTEXITCODE -ne 0) {
    & git commit -m 'Initial public release'
    if ($LASTEXITCODE -ne 0) { throw 'Initial commit failed.' }
}

& gh repo view $repo *> $null
if ($LASTEXITCODE -ne 0) {
    & gh repo create $repo --public --source=. --remote=origin --push
    if ($LASTEXITCODE -ne 0) { throw 'GitHub repository creation failed.' }
} else {
    $origin = (& git remote get-url origin 2>$null)
    if ($LASTEXITCODE -ne 0) {
        & git remote add origin "https://github.com/$repo.git"
    }
    & git push -u origin main
    if ($LASTEXITCODE -ne 0) { throw 'Push failed.' }
}

& gh repo edit $repo --description $description
foreach ($topic in $topics) {
    & gh repo edit $repo --add-topic $topic
}

Write-Host ''
Write-Host "Published: https://github.com/$repo" -ForegroundColor Green
