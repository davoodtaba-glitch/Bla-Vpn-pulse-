# Download geoip.dat / geosite.dat into app assets (optional but recommended)
$ErrorActionPreference = "Stop"
$assets = Join-Path $PSScriptRoot "..\app\src\main\assets"
New-Item -ItemType Directory -Force -Path $assets | Out-Null

$files = @{
  "geoip.dat"   = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat"
  "geosite.dat" = "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat"
}

foreach ($name in $files.Keys) {
  $url = $files[$name]
  $out = Join-Path $assets $name
  Write-Host "Downloading $name ..."
  Invoke-WebRequest -Uri $url -OutFile $out
  Write-Host "  -> $out"
}

Write-Host "Done."
