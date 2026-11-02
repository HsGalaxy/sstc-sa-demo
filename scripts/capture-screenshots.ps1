# ---------------------------------------------------------------------------
# Captures the running dashboard with headless Chrome.
# The app stores its session in localStorage, so a tiny bootstrap page writes the
# token/claims first and then redirects to the requested route.
# ---------------------------------------------------------------------------
param(
  [string]$Base = 'http://localhost:8080',
  [string]$OutDir = 'docs/screenshots',
  [int]$Width = 1560,
  [int]$Height = 1000,
  [string[]]$Routes = @(
    '/',
    '/architecture',
    '/tasks',
    '/files',
    '/okr',
    '/operations',
    '/collaboration',
    '/meetings',
    '/inbox',
    '/toolbox/documents',
    '/toolbox/presentations',
    '/toolbox/repos',
    '/toolbox/environments',
    '/toolbox/files',
    '/identity/users',
    '/identity/policies',
    '/identity/audit'
  )
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$chrome = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
if (-not (Test-Path $chrome)) { $chrome = 'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe' }

$out = Join-Path $root $OutDir
New-Item -ItemType Directory -Force -Path $out | Out-Null

# 1. sign in through the API to obtain a real session token
$login = Invoke-RestMethod -Uri "$Base/api/auth/login" -Method Post -ContentType 'application/json' `
  -Body '{"username":"lead","password":"Lead@123"}'
$userJson = $login.user | ConvertTo-Json -Depth 6 -Compress

# 2. bootstrap page: seed localStorage then go to the route
$bootDir = Join-Path $env:TEMP ('stc-shots-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $bootDir | Out-Null

$index = 0
foreach ($route in $Routes) {
  $index++
  $name = if ($route -eq '/') { 'overview' } else { ($route.Trim('/') -replace '/', '-') }
  $bootstrap = Join-Path $bootDir "boot-$index.html"
  $target = "$Base$route"
  $html = @"
<!doctype html><html><head><meta charset="utf-8"><title>boot</title></head><body>
<script>
localStorage.setItem('stc.token', '$($login.token)');
localStorage.setItem('stc.user', JSON.stringify($userJson));
location.replace('$target');
</script></body></html>
"@
  Set-Content -Path $bootstrap -Value $html -Encoding UTF8

  $shot = Join-Path $out ("{0}.png" -f $name)
  $url = 'file:///' + ($bootstrap -replace '\\', '/')
  $profile = Join-Path $bootDir "profile-$index"
  New-Item -ItemType Directory -Force -Path $profile | Out-Null
  try {
    & $chrome --headless=new --disable-gpu --hide-scrollbars --no-first-run --no-default-browser-check `
      --user-data-dir="$profile" --window-size="$Width,$Height" --virtual-time-budget=9000 `
      --screenshot="$shot" $url 2>$null 1>$null
  } catch {
    # Chrome writes its progress to stderr, which PowerShell surfaces as a native error;
    # the screenshot itself is still produced, so the result is checked below.
  }
  Start-Sleep -Milliseconds 400
  if (Test-Path $shot) {
    Write-Output ("captured {0,-28} -> {1} ({2} bytes)" -f $route, $shot, (Get-Item $shot).Length)
  } else {
    Write-Output ("FAILED   {0}" -f $route)
  }
}

Remove-Item $bootDir -Recurse -Force -ErrorAction SilentlyContinue
