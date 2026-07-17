# Build XrayPulse debug APK using your local JDK / Gradle / Android SDK
$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectRoot

# Prefer Android Studio JBR, then JDK 17
$candidates = @(
    "C:\Program Files\Android\Android Studio1\jbr",
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Java\jdk-17.0.2",
    "C:\Program Files\Java\jdk-17"
)
$javaHome = $candidates | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
if (-not $javaHome) { throw "No suitable JDK found" }
$env:JAVA_HOME = $javaHome
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH

$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

# Use already-downloaded Gradle 8.11.1 if present
$gradleBat = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.11.1-bin" -Recurse -Filter "gradle.bat" -ErrorAction SilentlyContinue |
    Select-Object -First 1 -ExpandProperty FullName

Write-Host "JAVA_HOME=$env:JAVA_HOME"
Write-Host "ANDROID_HOME=$env:ANDROID_HOME"
Write-Host "Gradle=$gradleBat"

if ($gradleBat) {
    & $gradleBat :app:assembleDebug --no-daemon @args
} else {
    & "$ProjectRoot\gradlew.bat" :app:assembleDebug --no-daemon @args
}

$apk = Get-ChildItem "$ProjectRoot\app\build\outputs\apk\debug\*.apk" -ErrorAction SilentlyContinue
if ($apk) {
    Write-Host "APK: $($apk.FullName) ($([math]::Round($apk.Length/1MB, 2)) MB)"
}
