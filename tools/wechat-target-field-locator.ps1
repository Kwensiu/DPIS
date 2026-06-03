param(
    [Parameter(Mandatory = $true)]
    [string] $ApkPath,

    [switch] $Json
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$locator = Join-Path $scriptDir "wechat_target_field_locator.py"
$arguments = @($locator, $ApkPath)
if ($Json) {
    $arguments += "--json"
}
python @arguments
