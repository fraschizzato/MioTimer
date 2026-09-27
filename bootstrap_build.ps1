$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$tools = Join-Path $root '.tools'
$zip = Join-Path $tools 'gradle-8.13-bin.zip'
$gradle = Join-Path $tools 'gradle-8.13\bin\gradle.bat'

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java is not installed or is not available in PATH. JDK 17+ is required.'
}

New-Item -ItemType Directory -Force $tools | Out-Null

if (-not (Test-Path $gradle)) {
    if (-not (Test-Path $zip)) {
        Write-Host 'Downloading Gradle 8.13...'
        Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' -OutFile $zip
    }
    Expand-Archive -Force $zip $tools
}

Push-Location $root
try {
    & $gradle wrapper --gradle-version 8.13
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle wrapper generation failed with exit code $LASTEXITCODE"
    }

    & .\gradlew.bat :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Android build failed with exit code $LASTEXITCODE"
    }

    $apk = 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path $apk)) {
        throw "Build completed but APK not found at $apk"
    }

    Write-Host "APK: $apk" -ForegroundColor Green
} finally {
    Pop-Location
}
