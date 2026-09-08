param(
    [Parameter(Mandatory = $true)]
    [string]$Package,
    [string]$Device = "",
    [int]$FontPercent = 0,
    [switch]$RestartTarget,
    [switch]$DeployProxy,
    [int]$TailLines = 800,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Continue"

function Write-Section($Title) {
    Add-Content -Path $script:ReportPath -Value ""
    Add-Content -Path $script:ReportPath -Value "## $Title"
}

function Run-Host($Label, [string[]]$CommandLine) {
    $output = ""
    $exitCode = 999
    try {
        $command = $CommandLine[0]
        $commandArgs = @($CommandLine | Select-Object -Skip 1)
        $output = (& $command @commandArgs 2>&1 | Out-String).TrimEnd()
        $exitCode = $LASTEXITCODE
    } catch {
        $output = $_.Exception.Message
    }
    Add-Content -Path $script:ReportPath -Value ""
    Add-Content -Path $script:ReportPath -Value "### $Label"
    Add-Content -Path $script:ReportPath -Value "exit=$exitCode"
    Add-Content -Path $script:ReportPath -Value '```text'
    Add-Content -Path $script:ReportPath -Value $output
    Add-Content -Path $script:ReportPath -Value '```'
    return [pscustomobject]@{ Label = $Label; ExitCode = $exitCode; Output = $output }
}

function AdbArgs([string[]]$Extra) {
    $args = @("adb")
    if ($Device -ne "") {
        $args += @("-s", $Device)
    }
    return $args + $Extra
}

function Run-Adb($Label, [string[]]$Extra) {
    return Run-Host $Label (AdbArgs $Extra)
}

function Run-Su($Label, [string]$Command) {
    return Run-Adb $Label @("shell", "su", "-c", $Command)
}

function Get-JavaHashHex([string]$Text) {
    $hash = [int64]0
    foreach ($ch in $Text.ToCharArray()) {
        $hash = (($hash * 31) + [int][char]$ch) -band 0xffffffffL
    }
    return "{0:x8}" -f $hash
}

function Resolve-NativeLibraryDir([string]$Dump) {
    foreach ($line in $Dump -split "`r?`n") {
        if ($line -match "nativeLibraryDir=([^\s]+)") {
            return $Matches[1]
        }
    }
    return ""
}

function Add-Verdict($Layer, $Status, $Reason) {
    $script:Verdicts += [pscustomobject]@{
        Layer = $Layer
        Status = $Status
        Reason = $Reason
    }
}

if ($OutDir -eq "") {
    $OutDir = Join-Path (Get-Location) "reports/device"
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$safePkg = $Package -replace "[^A-Za-z0-9_.-]", "_"
$script:ReportPath = Join-Path $OutDir "hyperos-smoke-$safePkg-$timestamp.md"
$script:Verdicts = @()

Set-Content -Path $script:ReportPath -Value "# DPIS HyperOS Smoke Report"
Add-Content -Path $script:ReportPath -Value ""
Add-Content -Path $script:ReportPath -Value "- package: $Package"
Add-Content -Path $script:ReportPath -Value "- device: $(if ($Device -ne '') { $Device } else { 'default adb device' })"
Add-Content -Path $script:ReportPath -Value "- expected font percent: $(if ($FontPercent -gt 0) { $FontPercent } else { 'not specified' })"
Add-Content -Path $script:ReportPath -Value "- restart target: $RestartTarget"
Add-Content -Path $script:ReportPath -Value "- deploy proxy: $DeployProxy"

$hash = Get-JavaHashHex $Package
$fontProp = "debug.dpis.font.$hash"
$forceFontProp = "debug.dpis.forcefont.$hash"
$rustBinProp = "debug.dpis.rustbin.$hash"
$viewportProp = "debug.dpis.viewport.$hash"

Add-Content -Path $script:ReportPath -Value "- java hash: $hash"
Add-Content -Path $script:ReportPath -Value "- font property: $fontProp"
Add-Content -Path $script:ReportPath -Value "- force font property: $forceFontProp"
Add-Content -Path $script:ReportPath -Value "- rust binary property: $rustBinProp"
Add-Content -Path $script:ReportPath -Value "- viewport property: $viewportProp"

Write-Section "Device"
$devices = Run-Adb "adb devices" @("devices")
$root = Run-Su "root id" "id"
if ($devices.Output -match "`tdevice") {
    Add-Verdict "device" "ok" "adb device is connected"
} else {
    Add-Verdict "device" "fail" "no adb device in device state"
}
if ($root.Output -match "uid=0") {
    Add-Verdict "root" "ok" "su returned uid=0"
} else {
    Add-Verdict "root" "fail" "su did not return uid=0"
}

Write-Section "Package"
$dump = Run-Adb "dumpsys package" @("shell", "dumpsys", "package", $Package)
$nativeDir = Resolve-NativeLibraryDir $dump.Output
if ($nativeDir -ne "") {
    Add-Verdict "package" "ok" "nativeLibraryDir=$nativeDir"
} else {
    Add-Verdict "package" "warn" "nativeLibraryDir not found in dumpsys"
}

Write-Section "DPIS Config"
$configRegex = "$([regex]::Escape($Package))|font[.]hyperos|target_packages|viewport[.]$([regex]::Escape($Package))|font[.]$([regex]::Escape($Package))|target[.]$([regex]::Escape($Package))|global[.]log_enabled"
$configCmd = "grep -n -E `"$configRegex`" /data/user/0/io.github.kwensiu.dpis/shared_prefs/dpi_config.xml 2>/dev/null || true"
$config = Run-Su "dpi_config.xml matching entries" $configCmd
if ($config.Output -match [regex]::Escape($Package)) {
    Add-Verdict "config" "ok" "target package appears in DPIS config"
} else {
    Add-Verdict "config" "warn" "target package was not found in matching DPIS config lines"
}

Write-Section "Runtime Properties"
$props = Run-Su "DPIS runtime properties" "echo font=$($fontProp):`$(getprop $fontProp); echo forcefont=$($forceFontProp):`$(getprop $forceFontProp); echo rustbin=$($rustBinProp):`$(getprop $rustBinProp); echo global_forcefont=debug.dpis.forcefont:`$(getprop debug.dpis.forcefont); echo viewport=$($viewportProp):`$(getprop $viewportProp)"
$fontValue = ""
$forceValue = ""
$rustValue = ""
$globalForceValue = ""
$viewportValue = ""
foreach ($line in @($props.Output -split "`r?`n")) {
    if ($line -match "^font=.*?:(.*)$") { $fontValue = $Matches[1].Trim() }
    elseif ($line -match "^forcefont=.*?:(.*)$") { $forceValue = $Matches[1].Trim() }
    elseif ($line -match "^rustbin=.*?:(.*)$") { $rustValue = $Matches[1].Trim() }
    elseif ($line -match "^global_forcefont=.*?:(.*)$") { $globalForceValue = $Matches[1].Trim() }
    elseif ($line -match "^viewport=.*?:(.*)$") { $viewportValue = $Matches[1].Trim() }
}
if ($FontPercent -gt 0 -and ($fontValue -eq "$FontPercent" -or $forceValue -eq "$FontPercent" -or $globalForceValue -eq "$FontPercent")) {
    Add-Verdict "font-property" "ok" "expected font percent is published"
} elseif ($fontValue -ne "" -or $forceValue -ne "" -or $globalForceValue -ne "") {
    Add-Verdict "font-property" "warn" "font properties exist but do not match expected value"
} else {
    Add-Verdict "font-property" "warn" "font properties are empty"
}
if ($rustValue -ne "" -and $rustValue -ne "0") {
    Add-Verdict "rustbin-property" "ok" "rust binary property is set"
} else {
    Add-Verdict "rustbin-property" "warn" "rust binary property is empty or zero"
}

Write-Section "Native Proxy"
if ($nativeDir -ne "") {
    $proxyPath = "$nativeDir/libdpis_native.so"
    if ($nativeDir.EndsWith("/lib")) {
        $proxyPath = "$nativeDir/arm64/libdpis_native.so"
    }
    if ($DeployProxy) {
        $targetPath = $proxyPath
        if ($nativeDir.EndsWith("/lib")) {
            $targetPath = "$nativeDir/arm64/libdpis_native.so"
        }
        $deployCommand = "module_so=/data/app/~~RtCgevRSg-JC2QSGsuQnHA==/io.github.kwensiu.dpis-Z2y1TY6ymqwojHgKzB3XZA==/lib/arm64/libdpis_native.so; " +
                "test -f `"$module_so`" || exit 2; " +
                "mkdir -p `"$nativeDir/arm64`" 2>/dev/null || true; " +
                "target=`"$targetPath`"; " +
                "cp -f `"$module_so`" `"$target`" || cat `"$module_so`" > `"$target`" || exit 1; " +
                "chown system:system `"$target`" 2>/dev/null || true; " +
                "chmod 755 `"$target`" 2>/dev/null || true; " +
                "chcon u:object_r:apk_data_file:s0 `"$target`" 2>/dev/null || true; " +
                "echo module_so=`"$module_so`"; echo target=`"$target`"; ls -l `"$target`"; wc -c `"$target`""
        Run-Su "deploy native proxy" $deployCommand
    }
    $proxy = Run-Su "target native proxy" "ls -l '$proxyPath' 2>/dev/null; wc -c '$proxyPath' 2>/dev/null || true"
    if ($proxy.Output -match "libdpis_native\.so" -and $proxy.Output -match "\d+") {
        Add-Verdict "native-proxy" "ok" "target native proxy exists"
    } else {
        Add-Verdict "native-proxy" "fail" "target native proxy is missing"
    }
}

if ($RestartTarget) {
    Write-Section "Target Restart"
    Run-Su "force-stop target" "am force-stop $Package"
    Start-Sleep -Seconds 1
    Run-Adb "launch target with am start" @("shell", "am", "start", "-a", "android.intent.action.MAIN", "-c", "android.intent.category.LAUNCHER", "-p", $Package)
    Start-Sleep -Seconds 5
}

Write-Section "Logcat"
$logcat = Run-Adb "recent DPIS logcat" @("logcat", "-d", "-t", "$TailLines")
$log = $logcat.Output
if ($log -match "HyperOS Rust process hook ready") {
    Add-Verdict "system-server-hook" "ok" "RustProcess hook was installed"
} else {
    Add-Verdict "system-server-hook" "warn" "no recent RustProcess hook ready log"
}
if ($log -match "HyperOS Rust process env apply: package=$([regex]::Escape($Package))") {
    Add-Verdict "rust-env-apply" "ok" "Rust process environment was rewritten"
} elseif ($log -match "HyperOS Rust process proxy missing: package=$([regex]::Escape($Package))") {
    Add-Verdict "rust-env-apply" "fail" "Rust hook ran but proxy was missing"
} else {
    Add-Verdict "rust-env-apply" "warn" "no recent Rust env apply log for target"
}
if ($log -match "DPIS_NATIVE: HyperOS proxy constructor: process=$([regex]::Escape($Package))" -and
        $log -match "DPIS_NATIVE:.*(ParagraphBuilder::Create|pushStyle|multiplier=)") {
    Add-Verdict "native-font" "ok" "native font paragraph path appears to be hit"
} else {
    Add-Verdict "native-font" "warn" "no recent native paragraph font log"
}
if ($log -match "ForceTextSize|TextPaint\.setTextSize|Paint\.setTextSize|TextView span|WebView font") {
    Add-Verdict "java-font" "ok" "Java font fallback path appears to be hit"
} else {
    Add-Verdict "java-font" "warn" "no recent Java font fallback log"
}
if ($log -match "system_server .*apply: package=$([regex]::Escape($Package))|视口\s+$([regex]::Escape($Package))|viewport") {
    Add-Verdict "viewport" "ok" "recent viewport/system_server activity found"
} else {
    Add-Verdict "viewport" "warn" "no recent viewport activity found"
}

Write-Section "Verdict"
foreach ($v in $script:Verdicts) {
    Add-Content -Path $script:ReportPath -Value "- [$($v.Status)] $($v.Layer): $($v.Reason)"
}

Write-Section "Likely Next Step"
if (@($script:Verdicts | Where-Object { $_.Layer -eq "device" -and $_.Status -eq "fail" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "No usable adb device was detected. Connect/unlock the device, authorize adb, then rerun the smoke test."
} elseif (@($script:Verdicts | Where-Object { $_.Layer -eq "root" -and $_.Status -eq "fail" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "Root access failed. Fix su/root first; HyperOS property and proxy checks are not reliable."
} elseif (@($script:Verdicts | Where-Object { $_.Layer -eq "native-proxy" -and $_.Status -eq "fail" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "Native proxy is missing. Save/restart through DPIS or deploy proxy before judging native font behavior."
} elseif (@($script:Verdicts | Where-Object { $_.Layer -eq "rust-env-apply" -and $_.Status -eq "fail" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "Rust hook ran but proxy path was missing. Check target native lib directory and proxy deployment."
} elseif (@($script:Verdicts | Where-Object { $_.Layer -eq "rust-env-apply" -and $_.Status -eq "warn" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "No recent Rust env apply was observed. Rerun with -RestartTarget; if still absent, system_server hook may be stale or disabled."
} elseif (@($script:Verdicts | Where-Object { $_.Layer -eq "native-font" -and $_.Status -eq "warn" }).Count -gt 0) {
    Add-Content -Path $script:ReportPath -Value "Rust/proxy layers may be ready but native paragraph logs are absent. Open a text-heavy page in the target app, then rerun log collection."
} else {
    Add-Content -Path $script:ReportPath -Value "Core layers look present in recent evidence. Compare visual result or capture screenshots for final validation."
}

Write-Output $script:ReportPath
