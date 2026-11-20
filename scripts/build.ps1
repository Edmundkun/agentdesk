# AgentDesk 一键打包：前端构建 → 复制到 Spring Boot static → 打可执行 Jar
# 用法：pwsh scripts/build.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'
$staticDir = Join-Path $root 'src\main\resources\static'
$mvn = 'mvn'

Write-Host '[1/4] 安装前端依赖' -ForegroundColor Cyan
Push-Location $frontend
try {
    if (Test-Path 'package-lock.json') { npm ci } else { npm install }
    if ($LASTEXITCODE -ne 0) { throw 'npm install 失败' }

    Write-Host '[2/4] 构建前端' -ForegroundColor Cyan
    npm run build
    if ($LASTEXITCODE -ne 0) { throw 'npm run build 失败' }
} finally {
    Pop-Location
}

Write-Host '[3/4] 复制静态资源到后端' -ForegroundColor Cyan
if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }
New-Item -ItemType Directory -Path $staticDir | Out-Null
Copy-Item (Join-Path $frontend 'dist\*') $staticDir -Recurse

Write-Host '[4/4] Maven 打包' -ForegroundColor Cyan
Push-Location $root
try {
    & $mvn '-B' '-DskipTests' 'clean' 'package'
    if ($LASTEXITCODE -ne 0) { throw 'Maven 打包失败' }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host '构建完成：target\agentdesk.jar' -ForegroundColor Green
Write-Host '启动命令：java -jar target\agentdesk.jar' -ForegroundColor Green
