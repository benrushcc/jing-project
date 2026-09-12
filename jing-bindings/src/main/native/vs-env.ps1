#Requires -Version 7.0
<#
.SYNOPSIS
    Detects a Visual Studio 2017+ installation via vswhere, imports its
    "Developer PowerShell for VS" environment (INCLUDE/LIB/Path/etc.) into the
    current process, and optionally runs a command in that environment.

.DESCRIPTION
    Restores the equivalent of a VsDevCmd.bat session so that CMake + Ninja can
    drive the MSVC toolchain (cl.exe) without manually opening a Developer
    PowerShell. Requires PowerShell 7+ (pwsh) and Visual Studio 2017 or later:
    VS location is resolved exclusively through vswhere.exe. If vswhere is
    missing (VS 2015 and older), the script fails with a clear error.

.PARAMETER Arch
    Target architecture passed to VsDevCmd.bat. Defaults to x64.

.PARAMETER HostArch
    Host architecture passed to VsDevCmd.bat (optional).

.PARAMETER InstallDir
    Visual Studio installation directory override (skips vswhere).

.PARAMETER VsDevCmd
    Full path to VsDevCmd.bat override (skips vswhere).

.PARAMETER CacheFile
    File used to cache the environment dump so later runs skip launching
    cmd.exe. Defaults to .vs-env-<arch>.txt next to this script. The cache is
    auto-invalidated when it does not match the resolved installation.

.PARAMETER Refresh
    Force a fresh dump, ignoring an existing cache.

.PARAMETER Command
    If provided, the script imports the environment and then runs this command
    in a child pwsh process, returning its exit code. The whole sequence runs
    in one process so the environment survives to the build step. Cannot be
    combined with dot-sourcing.

.PARAMETER Info
    Print the detected VS toolchain information and exit.

.EXAMPLE
    pwsh -NoProfile -File .\vs-dev-env.ps1 -Info

.EXAMPLE
    pwsh -NoProfile -File .\vs-dev-env.ps1 -Command 'cmake -S . -B build -G Ninja && cmake --build build && .\build\hello.exe'

.EXAMPLE
    . .\vs-dev-env.ps1; cmake --build build
#>
[CmdletBinding()]
param(
    [ValidateSet('x64', 'x86', 'arm64', 'arm')]
    [string]$Arch = 'x64',
    [ValidateSet('x64', 'x86', '')]
    [string]$HostArch = '',
    [string]$InstallDir = '',
    [string]$VsDevCmd = '',
    [string]$CacheFile = '',
    [switch]$Refresh,
    [string]$Command = '',
    [switch]$Info
)

$ErrorActionPreference = 'Stop'
$dotSourced = $MyInvocation.InvocationName -eq '.'

function Exit-Script {
    param([int]$Code)
    if ($dotSourced) {
        return
    }
    exit $Code
}

function Get-VsDevCmdPath {
    if ($VsDevCmd) {
        if (Test-Path -LiteralPath $VsDevCmd) { return $VsDevCmd }
        throw "VsDevCmd.bat not found: $VsDevCmd"
    }
    if ($InstallDir) {
        $candidate = Join-Path $InstallDir 'Common7\Tools\VsDevCmd.bat'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        throw "No VsDevCmd.bat under -InstallDir: $InstallDir"
    }
    $prog86 = ${env:ProgramFiles(x86)}
    $vswhere = Join-Path $prog86 'Microsoft Visual Studio\Installer\vswhere.exe'
    if (-not (Test-Path -LiteralPath $vswhere)) {
        throw "vswhere.exe not found at: $vswhere. vswhere ships with Visual Studio 2017+. Install VS 2017+ (with the ""Desktop development with C++"" workload) and retry. VS 2015 and older are not supported."
    }
    $installDir = & $vswhere -latest -utf8 -products * -requires 'Microsoft.VisualStudio.Component.VC.Tools.x86.x64' -property installationPath
    if (-not $installDir) {
        throw 'No Visual Studio with the C++ workload was found by vswhere. Install the "Desktop development with C++" workload, or pass -VsDevCmd / -InstallDir explicitly.'
    }
    $candidate = Join-Path $installDir.Trim() 'Common7\Tools\VsDevCmd.bat'
    if (-not (Test-Path -LiteralPath $candidate)) {
        throw "VsDevCmd.bat not found under the vswhere result: $candidate"
    }
    return $candidate
}

function Invoke-EnvDump {
    param(
        [string]$Bat,
        [string]$TargetArch,
        [string]$HostArchValue,
        [string]$OutFile
    )
    $tmpBat = Join-Path $env:TEMP ("vsdev-dump-{0}.bat" -f [guid]::NewGuid().ToString('N'))
    $argsLine = '-arch=' + $TargetArch
    if ($HostArchValue) { $argsLine += ' -host_arch=' + $HostArchValue }
    $content = "@echo off`r`n" +
        "chcp 65001 >nul 2>&1`r`n" +
        "cd . >nul 2>&1`r`n" +
        'call "' + $Bat + '" ' + $argsLine + " >nul 2>&1`r`n" +
        'if errorlevel 1 exit /b 1' + "`r`n" +
        'set' + "`r`n"
    Set-Content -LiteralPath $tmpBat -Value $content -Encoding ASCII
    try {
        $lines = @(& cmd.exe /d /c "`"$tmpBat`"")
    }
    finally {
        Remove-Item -LiteralPath $tmpBat -Force -ErrorAction SilentlyContinue
    }
    if ($LASTEXITCODE -ne 0) { return $false }
    if ($OutFile) {
        $header = "# vsenv from $Bat"
        Set-Content -LiteralPath $OutFile -Value (@($header) + $lines) -Encoding utf8
        return $true
    }
    return , $lines
}

function Import-EnvLines {
    param([string[]]$Lines)
    foreach ($line in $Lines) {
        if (-not $line -or $line.StartsWith('#')) { continue }
        $i = $line.IndexOf('=')
        if ($i -gt 0) {
            $name = $line.Substring(0, $i)
            $value = $line.Substring($i + 1)
            [System.Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

$batPath = Get-VsDevCmdPath

if (-not $CacheFile) {
    $cacheName = '.vs-env-' + $Arch
    if ($HostArch) { $cacheName += '-' + $HostArch }
    $CacheFile = Join-Path $PSScriptRoot ($cacheName + '.txt')
}

$needDump = $Refresh -or (-not (Test-Path -LiteralPath $CacheFile))
if (-not $needDump) {
    $cached = [System.IO.File]::ReadAllLines($CacheFile)
    $expectedHeader = "# vsenv from $batPath"
    if ($cached.Count -eq 0 -or $cached[0] -ne $expectedHeader) {
        $needDump = $true
    }
}

if ($needDump) {
    $ok = Invoke-EnvDump -Bat $batPath -TargetArch $Arch -HostArchValue $HostArch -OutFile $CacheFile
    if (-not $ok) {
        throw "VsDevCmd.bat failed. Check that the VC++ workload (Microsoft.VisualStudio.Component.VC.Tools.x86.x64) is installed."
    }
}

$lines = [System.IO.File]::ReadAllLines($CacheFile)
Import-EnvLines -Lines $lines

if (-not (Get-Command cl.exe -ErrorAction SilentlyContinue)) {
    Write-Warning "cl.exe was not found after importing the VS environment; the C++ workload may be missing."
}

Write-Host ("VSDevEnv ready: {0} -arch={1} (VSCMD_VER={2})" -f $env:VSINSTALLDIR, $Arch, $env:VSCMD_VER)

if ($Info) {
    Write-Host "  VsDevCmd.bat : $batPath"
    Write-Host "  MSVC dir     : $($env:VCToolsInstallDir)"
    Write-Host "  MSVC version : $($env:VCToolsVersion)"
    Write-Host "  Windows SDK  : $($env:WindowsSDKVersion)"
    Write-Host "  cl.exe       : $((Get-Command cl.exe -ErrorAction SilentlyContinue).Source)"
    Write-Host "  cache file   : $CacheFile"
    Exit-Script 0
}

if ($Command) {
    if ($dotSourced) {
        throw '-Command cannot be used when dot-sourcing this script. Run it as a file, or dot-source it and run your command in the caller.'
    }
    pwsh -NoProfile -NonInteractive -Command $Command
    Exit-Script $LASTEXITCODE
}