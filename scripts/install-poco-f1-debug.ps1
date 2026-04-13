param(
    [string]$GradleTask = ":app:installDebug"
)

$ErrorActionPreference = "Stop"

function Get-PocoDeviceSerial {
    $deviceLines = adb devices -l | Select-Object -Skip 1
    foreach ($line in $deviceLines) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        if ($line -notmatch "\sdevice\s") {
            continue
        }

        $isPocoModel = ($line -match "model:POCO_F1") -or ($line -match "model:beryllium")
        $isPocoProduct = $line -match "product:beryllium"
        $isPocoDevice = $line -match "device:beryllium"

        if ($isPocoModel -or $isPocoProduct -or $isPocoDevice) {
            return ($line -split "\s+")[0]
        }
    }

    return $null
}

$serial = Get-PocoDeviceSerial
if (-not $serial) {
    Write-Error "No connected Poco F1 detected. Connect Poco F1 and retry."
}

Write-Host "Using Poco F1 device serial: $serial"
$env:ANDROID_SERIAL = $serial

& .\gradlew.bat $GradleTask
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Installed debug build to Poco F1 ($serial)."
