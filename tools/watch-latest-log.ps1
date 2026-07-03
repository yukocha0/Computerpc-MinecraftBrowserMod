param(
	[string]$SourceLog = "C:\Users\yuko0\Downloads\justpc- minecraft fabric - 1.21.11\run\logs\latest.log",
	[string]$OutputLog = "C:\Users\yuko0\Downloads\justpc- minecraft fabric - 1.21.11\run\logs\computerpc-watch.log"
)

$ErrorActionPreference = 'Stop'

$patterns = @(
	'ERROR',
	'WARN',
	'Exception',
	'Failed',
	'MCEF',
	'JCEF',
	'computerpc',
	'NullPointerException',
	'IllegalStateException',
	'RuntimeException',
	"Can't keep up"
)

$regex = [string]::Join('|', $patterns)

"watch-start $(Get-Date -Format s)" | Set-Content $OutputLog
"source=$SourceLog" | Add-Content $OutputLog

if (-not (Test-Path $SourceLog)) {
	"source log missing" | Add-Content $OutputLog
	exit 1
}

Get-Content $SourceLog -Tail 0 -Wait | ForEach-Object {
	if ($_ -match $regex) {
		"[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $_ | Add-Content $OutputLog
	}
}
