<#
.SYNOPSIS
  Builds a signed Happit APK and publishes it as a GitHub release.
  Installed apps see it under Profile > Updates (and get a prompt on launch).

.EXAMPLE
  ./release.ps1 -Version 1.1.0 -Notes "New: weekly goals"
  ./release.ps1 -Version 1.0.0 -NoBump   # publish the version already in build.gradle.kts
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$Notes = "",
    [switch]$NoBump
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'F:\claude\android-sdk\jdk-21' }

if (-not (Test-Path (Join-Path $root 'keystore.properties'))) {
    throw 'keystore.properties is missing. Releases must be signed with the original key or phones will refuse the update.'
}

$gradleFile = Join-Path $root 'app\build.gradle.kts'
if (-not $NoBump) {
    $text = [IO.File]::ReadAllText($gradleFile)
    $code = [int]([regex]::Match($text, 'versionCode = (\d+)').Groups[1].Value) + 1
    $text = $text -replace 'versionCode = \d+', "versionCode = $code"
    $text = $text -replace 'versionName = "[^"]*"', "versionName = `"$Version`""
    [IO.File]::WriteAllText($gradleFile, $text)
    Write-Host "Version $Version (code $code)"
}

Push-Location $root
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }

    New-Item -ItemType Directory -Force (Join-Path $root 'release') | Out-Null
    $apk = Join-Path $root "release\Happit-v$Version.apk"
    Copy-Item (Join-Path $root 'app\build\outputs\apk\release\app-release.apk') $apk -Force

    if (-not $NoBump) {
        git add app/build.gradle.kts
        git commit -m "Release v$Version"
    }
    git tag "v$Version"
    git push
    git push origin "v$Version"

    if ($Notes) {
        gh release create "v$Version" $apk --title "Happit v$Version" --notes $Notes
    } else {
        gh release create "v$Version" $apk --title "Happit v$Version" --generate-notes
    }
} finally {
    Pop-Location
}
