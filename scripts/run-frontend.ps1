# 启动前端开发服务器（自动代理 /api 到 http://localhost:8080）
# 用法：pwsh scripts/run-frontend.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Push-Location (Join-Path $root 'frontend')
try {
    if (-not (Test-Path 'node_modules')) { npm install }
    npm run dev
} finally {
    Pop-Location
}
