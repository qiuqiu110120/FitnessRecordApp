param(
    [string]$DestinationRoot = "D:\Project",
    [string]$ProjectFolderName = "FitnessRecordApp",
    [switch]$CopyUserGradleCache,
    [switch]$CleanSourceHeavyArtifacts,
    [switch]$CleanOnly
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$SourceRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$SourceRootPath = [System.IO.Path]::GetFullPath($SourceRoot)
$DestinationRootPath = [System.IO.Path]::GetFullPath($DestinationRoot)
$DestinationPath = Join-Path $DestinationRootPath $ProjectFolderName
$DestinationPath = [System.IO.Path]::GetFullPath($DestinationPath)

function Assert-UnderPath {
    param(
        [string]$Path,
        [string]$ParentPath
    )

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullParent = [System.IO.Path]::GetFullPath($ParentPath)
    if (-not $fullParent.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $fullParent = $fullParent + [System.IO.Path]::DirectorySeparatorChar
    }

    if (-not $fullPath.StartsWith($fullParent, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside expected directory. Path: $fullPath Parent: $fullParent"
    }
}

function Invoke-RobocopyChecked {
    param([string[]]$Arguments)

    & robocopy @Arguments
    $exitCode = $LASTEXITCODE
    if ($exitCode -gt 7) {
        throw "Robocopy failed with exit code $exitCode"
    }
}

function ConvertTo-JavaPropertiesValue {
    param([string]$Value)

    $builder = [System.Text.StringBuilder]::new()
    foreach ($char in $Value.ToCharArray()) {
        $code = [int]$char
        if ($char -eq [char]92) {
            [void]$builder.Append("\\")
        } elseif ($char -eq [char]58) {
            [void]$builder.Append("\:")
        } elseif ($code -lt 32 -or $code -gt 126) {
            [void]$builder.Append(("\u{0:x4}" -f $code))
        } else {
            [void]$builder.Append($char)
        }
    }

    $builder.ToString()
}

$destinationDrive = [System.IO.Path]::GetPathRoot($DestinationRootPath)
if (-not (Test-Path -LiteralPath $destinationDrive)) {
    throw "Destination drive does not exist: $destinationDrive"
}

if ($DestinationPath.Equals($SourceRootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Destination path is the same as source path."
}

if (-not $CleanOnly) {
New-Item -ItemType Directory -Force -Path $DestinationRootPath | Out-Null
New-Item -ItemType Directory -Force -Path $DestinationPath | Out-Null
Assert-UnderPath $DestinationPath $DestinationRootPath

Write-Host "Migrating project from $SourceRootPath to $DestinationPath"
Invoke-RobocopyChecked @(
    $SourceRootPath,
    $DestinationPath,
    "/E",
    "/R:2",
    "/W:2",
    "/XD",
    ".gradle",
    ".kotlin",
    "build",
    "/XF",
    "local.properties"
)

$destinationGeneratedDirs = @(
    (Join-Path $DestinationPath ".gradle"),
    (Join-Path $DestinationPath ".kotlin"),
    (Join-Path $DestinationPath "app\build")
)

foreach ($dir in $destinationGeneratedDirs) {
    if (Test-Path -LiteralPath $dir) {
        Assert-UnderPath $dir $DestinationPath
        Remove-Item -LiteralPath $dir -Recurse -Force
    }
}

$DestinationToolsRoot = Join-Path $DestinationPath ".tools"
$DestinationGradleUserHome = Join-Path $DestinationToolsRoot "gradle-user-home"
New-Item -ItemType Directory -Force -Path $DestinationGradleUserHome | Out-Null

if ($CopyUserGradleCache) {
    $UserGradleHome = Join-Path $env:USERPROFILE ".gradle"
    if (Test-Path -LiteralPath $UserGradleHome) {
        Write-Host "Copying user Gradle cache to project-local Gradle home"
        Invoke-RobocopyChecked @(
            $UserGradleHome,
            $DestinationGradleUserHome,
            "/E",
            "/R:2",
            "/W:2",
            "/XD",
            "daemon",
            "workers",
            "native",
            "/XF",
            "*.lock",
            "*.lck",
            "*.part"
        )
    }
}

$DestinationSdkRoot = Join-Path $DestinationToolsRoot "android-sdk"
$EscapedSdkDir = ConvertTo-JavaPropertiesValue $DestinationSdkRoot
Set-Content -LiteralPath (Join-Path $DestinationPath "local.properties") -Value "sdk.dir=$EscapedSdkDir" -Encoding ASCII
}

if ($CleanSourceHeavyArtifacts) {
    $SourceGradleBin = Join-Path $SourceRootPath ".tools\gradle\gradle-8.10.2\bin\gradle.bat"
    $SourceJdkRoot = Join-Path $SourceRootPath ".tools\jdk"
    if ((Test-Path -LiteralPath $SourceGradleBin) -and (Test-Path -LiteralPath $SourceJdkRoot)) {
        $SourceJdkHome = Get-ChildItem -LiteralPath $SourceJdkRoot -Directory |
            Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
            Select-Object -First 1 -ExpandProperty FullName

        $previousJavaHome = $env:JAVA_HOME
        $previousGradleUserHome = $env:GRADLE_USER_HOME
        try {
            if ($SourceJdkHome) {
                $env:JAVA_HOME = $SourceJdkHome
            }
            $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE ".gradle"
            & $SourceGradleBin --stop | Out-Host
        } catch {
            Write-Host "Gradle daemon stop skipped: $($_.Exception.Message)"
        } finally {
            $env:JAVA_HOME = $previousJavaHome
            $env:GRADLE_USER_HOME = $previousGradleUserHome
        }
    }

    Write-Host "Cleaning heavy artifacts from C drive"
    $sourceCleanupTargets = @(
        (Join-Path $SourceRootPath ".tools"),
        (Join-Path $SourceRootPath ".gradle"),
        (Join-Path $SourceRootPath ".kotlin"),
        (Join-Path $SourceRootPath "app\build")
    )

    foreach ($target in $sourceCleanupTargets) {
        if (Test-Path -LiteralPath $target) {
            Assert-UnderPath $target $SourceRootPath
            Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue
        }
    }

    $expectedUserGradleRoot = Join-Path $env:USERPROFILE ".gradle"
    $userGradleGeneratedDirs = @(
        (Join-Path $expectedUserGradleRoot "caches"),
        (Join-Path $expectedUserGradleRoot "wrapper"),
        (Join-Path $expectedUserGradleRoot "daemon"),
        (Join-Path $expectedUserGradleRoot "native"),
        (Join-Path $expectedUserGradleRoot "jdks"),
        (Join-Path $expectedUserGradleRoot "kotlin"),
        (Join-Path $expectedUserGradleRoot "kotlin-profile"),
        (Join-Path $expectedUserGradleRoot "notifications")
    )

    foreach ($target in $userGradleGeneratedDirs) {
        if (Test-Path -LiteralPath $target) {
            Assert-UnderPath $target $expectedUserGradleRoot
            Remove-Item -LiteralPath $target -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "Migration complete: $DestinationPath"
