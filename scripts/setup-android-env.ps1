param(
    [switch]$InstallSdk,
    [switch]$GenerateWrapper,
    [switch]$Build
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$ToolsRoot = Join-Path $ProjectRoot ".tools"
$DownloadsRoot = Join-Path $ToolsRoot "downloads"
$JdkRoot = Join-Path $ToolsRoot "jdk"
$GradleRoot = Join-Path $ToolsRoot "gradle"
$GradleUserHome = Join-Path $ToolsRoot "gradle-user-home"
$AndroidSdkRoot = Join-Path $ToolsRoot "android-sdk"
$GradleVersion = "8.10.2"

function Assert-UnderProjectRoot {
    param([string]$Path)

    $resolvedProjectRoot = [System.IO.Path]::GetFullPath($ProjectRoot)
    $resolvedPath = [System.IO.Path]::GetFullPath($Path)
    if (-not $resolvedPath.StartsWith($resolvedProjectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to modify path outside project root: $resolvedPath"
    }
}

function Ensure-Directory {
    param([string]$Path)

    Assert-UnderProjectRoot $Path
    New-Item -ItemType Directory -Force -Path $Path | Out-Null
}

function Download-File {
    param(
        [string]$Url,
        [string]$OutFile
    )

    Assert-UnderProjectRoot $OutFile
    if (Test-Path -LiteralPath $OutFile) {
        Write-Host "Using cached download: $OutFile"
        return
    }

    Write-Host "Downloading: $Url"
    Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $OutFile
}

function Expand-ZipOnce {
    param(
        [string]$ZipFile,
        [string]$Destination,
        [string]$MarkerFile
    )

    Assert-UnderProjectRoot $Destination
    if (Test-Path -LiteralPath $MarkerFile) {
        Write-Host "Already extracted: $Destination"
        return
    }

    Ensure-Directory $Destination
    Expand-Archive -LiteralPath $ZipFile -DestinationPath $Destination -Force
    New-Item -ItemType File -Force -Path $MarkerFile | Out-Null
}

function Invoke-Checked {
    param(
        [string]$Command,
        [string[]]$Arguments = @()
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Command $($Arguments -join ' ')"
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

Ensure-Directory $ToolsRoot
Ensure-Directory $DownloadsRoot
Ensure-Directory $JdkRoot
Ensure-Directory $GradleRoot
Ensure-Directory $GradleUserHome
Ensure-Directory $AndroidSdkRoot

$JdkZip = Join-Path $DownloadsRoot "temurin-jdk17-windows-x64.zip"
$GradleZip = Join-Path $DownloadsRoot "gradle-$GradleVersion-bin.zip"
$CmdlineToolsZip = Join-Path $DownloadsRoot "android-commandlinetools-win.zip"

Download-File "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" $JdkZip
Download-File "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" $GradleZip
Download-File "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" $CmdlineToolsZip

Expand-ZipOnce $JdkZip $JdkRoot (Join-Path $JdkRoot ".extracted")
Expand-ZipOnce $GradleZip $GradleRoot (Join-Path $GradleRoot ".extracted")

$JdkHome = Get-ChildItem -LiteralPath $JdkRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "bin\java.exe") } |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $JdkHome) {
    throw "JDK extraction succeeded, but java.exe was not found."
}

$GradleBin = Join-Path $GradleRoot "gradle-$GradleVersion\bin\gradle.bat"
if (-not (Test-Path -LiteralPath $GradleBin)) {
    throw "Gradle was not found at $GradleBin"
}

$CmdlineLatest = Join-Path $AndroidSdkRoot "cmdline-tools\latest"
$SdkManager = Join-Path $CmdlineLatest "bin\sdkmanager.bat"
if (-not (Test-Path -LiteralPath $SdkManager)) {
    $TempCmdline = Join-Path $ToolsRoot "tmp-cmdline-tools"
    Assert-UnderProjectRoot $TempCmdline
    if (Test-Path -LiteralPath $TempCmdline) {
        Remove-Item -LiteralPath $TempCmdline -Recurse -Force
    }
    Ensure-Directory $TempCmdline
    Expand-Archive -LiteralPath $CmdlineToolsZip -DestinationPath $TempCmdline -Force

    $ExpandedCmdline = Join-Path $TempCmdline "cmdline-tools"
    if (-not (Test-Path -LiteralPath $ExpandedCmdline)) {
        throw "Android command-line tools archive layout was not recognized."
    }

    Ensure-Directory (Split-Path -Parent $CmdlineLatest)
    Move-Item -LiteralPath $ExpandedCmdline -Destination $CmdlineLatest
    Remove-Item -LiteralPath $TempCmdline -Recurse -Force
}

$env:JAVA_HOME = $JdkHome
$env:ANDROID_HOME = $AndroidSdkRoot
$env:ANDROID_SDK_ROOT = $AndroidSdkRoot
$env:GRADLE_USER_HOME = $GradleUserHome
$env:Path = "$JdkHome\bin;$AndroidSdkRoot\platform-tools;$CmdlineLatest\bin;$env:Path"

$LocalProperties = Join-Path $ProjectRoot "local.properties"
$EscapedSdkDir = ConvertTo-JavaPropertiesValue $AndroidSdkRoot
Set-Content -LiteralPath $LocalProperties -Value "sdk.dir=$EscapedSdkDir" -Encoding ASCII
Write-Host "Wrote local.properties"

Invoke-Checked (Join-Path $JdkHome "bin\java.exe") @("-version")
Invoke-Checked $GradleBin @("--version")
Invoke-Checked $SdkManager @("--version")

if ($InstallSdk) {
    Write-Host "Accepting Android SDK licenses"
    $yesAnswers = [string]::Join([Environment]::NewLine, (1..100 | ForEach-Object { "y" }))
    $yesAnswers | & $SdkManager --sdk_root=$AndroidSdkRoot --licenses
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $SdkManager --licenses"
    }

    Write-Host "Installing Android SDK packages"
    Invoke-Checked $SdkManager @("--sdk_root=$AndroidSdkRoot", "platform-tools", "platforms;android-35", "build-tools;35.0.0")
}

if ($GenerateWrapper) {
    Write-Host "Generating Gradle Wrapper"
    Push-Location $ProjectRoot
    try {
        Invoke-Checked $GradleBin @("wrapper", "--gradle-version", $GradleVersion, "--distribution-type", "bin")
    } finally {
        Pop-Location
    }
}

if ($Build) {
    Write-Host "Building debug APK with project-local Gradle"
    Push-Location $ProjectRoot
    try {
        Invoke-Checked $GradleBin @(":app:assembleDebug")
    } finally {
        Pop-Location
    }
}

Write-Host "Android development environment is ready for this project."
