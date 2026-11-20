# Test helper for the Nexus API.
# Load in any PowerShell window with:   . .\scripts\api.ps1   (from repo root)

$global:base = "http://localhost:8080"

function global:Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = $null
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }

    $params = @{
        Method      = $Method
        Uri         = "$global:base$Path"
        Headers     = $headers
        ContentType = "application/json"
    }
    if ($Body) { $params["Body"] = ($Body | ConvertTo-Json -Compress) }

    try {
        $res = Invoke-RestMethod @params
        Write-Host "OK  $Method $Path" -ForegroundColor Green
        return $res
    } catch {
        $resp = $_.Exception.Response
        $code = $resp.StatusCode.value__
        $msg  = $_.ErrorDetails.Message
        if (-not $msg -and $resp) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            $msg = $reader.ReadToEnd()
        }
        Write-Host "HTTP $code  $Method $Path  $msg" -ForegroundColor Red
    }
}

Write-Host "Api helper loaded -> $global:base" -ForegroundColor Cyan