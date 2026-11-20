# 启动后端（默认 mock 模型，无需 API Key）
# 用法：pwsh scripts/run-backend.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Push-Location $root
try {
    if (Test-Path 'target\agentdesk.jar') {
        java -jar 'target\agentdesk.jar'
    } else {
        mvn '-B' 'spring-boot:run'
    }
} finally {
    Pop-Location
}
