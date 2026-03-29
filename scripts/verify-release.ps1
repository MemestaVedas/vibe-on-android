Write-Host "Running Android release quality gate..." -ForegroundColor Cyan

$ErrorActionPreference = "Stop"

Push-Location "$PSScriptRoot\.."
try {
    .\gradlew.bat :app:compileDebugKotlin
    .\gradlew.bat :app:lintDebug
    .\gradlew.bat :app:testDebugUnitTest
    Write-Host "Android release quality gate passed." -ForegroundColor Green
}
finally {
    Pop-Location
}
