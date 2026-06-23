param(
    [string] $Device = "",
    [string] $OutputDirectory = "",
    [string] $Pattern = "io.github.kwensiu.dpis|DPIS|Auto hot reload|hot reload|status=",
    [int] $Tail = 200
)

$ErrorActionPreference = "Stop"

function Resolve-Adb {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -eq $adb) {
        throw "adb was not found on PATH."
    }
    return $adb.Source
}

function Convert-SafeFileName {
    param([string] $Name)
    return ($Name -replace '[\\/:*?"<>|]', '_')
}

function Remove-NulBytes {
    param([byte[]] $Bytes)
    $buffer = New-Object System.Collections.Generic.List[byte]
    foreach ($byte in $Bytes) {
        if ($byte -ne 0) {
            [void] $buffer.Add($byte)
        }
    }
    return $buffer.ToArray()
}

if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $stamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $OutputDirectory = Join-Path $env:TEMP "dpis_lsposed_logs_$stamp"
}

$adbPath = Resolve-Adb
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null

$adbArgs = @()
if (-not [string]::IsNullOrWhiteSpace($Device)) {
    $adbArgs += @("-s", $Device)
}

$remoteListCommand = "ls -t /data/adb/lspd/log/modules_*.log /data/adb/lspd/log/verbose_*.log 2>/dev/null | head -n 2"
$remoteFiles = & $adbPath @adbArgs shell su 0 sh -c $remoteListCommand |
    ForEach-Object { $_.Trim() } |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

if ($remoteFiles.Count -eq 0) {
    throw "No LSPosed modules_*.log or verbose_*.log files found on device $Device."
}

$summaryPath = Join-Path $OutputDirectory "summary.txt"
$summaryLines = New-Object System.Collections.Generic.List[string]
$summaryLines.Add("device=$Device")
$summaryLines.Add("pulledAt=$(Get-Date -Format o)")
$summaryLines.Add("pattern=$Pattern")
$summaryLines.Add("")

foreach ($remoteFile in $remoteFiles) {
    $remoteName = $remoteFile -replace '^.*/', ''
    $safeName = Convert-SafeFileName $remoteName
    $localPath = Join-Path $OutputDirectory $safeName
    $remoteCatCommand = "cat '$remoteFile'"
    & $adbPath @adbArgs exec-out su 0 sh -c $remoteCatCommand > $localPath

    $bytes = [System.IO.File]::ReadAllBytes($localPath)
    $cleanBytes = Remove-NulBytes $bytes
    $text = [System.Text.Encoding]::UTF8.GetString($cleanBytes)
    $matches = $text -split "`r?`n" |
        Select-String -Pattern $Pattern |
        Select-Object -Last $Tail

    $summaryLines.Add("## $remoteFile")
    $summaryLines.Add("local=$localPath")
    foreach ($match in $matches) {
        $summaryLines.Add($match.Line)
    }
    $summaryLines.Add("")
}

[System.IO.File]::WriteAllLines($summaryPath, $summaryLines, [System.Text.UTF8Encoding]::new($false))

Write-Output "Pulled LSPosed logs to: $OutputDirectory"
Write-Output "Summary: $summaryPath"
Get-Content $summaryPath
