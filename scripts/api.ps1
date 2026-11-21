# Test helper for the Nexus API.
# Load in any PowerShell window from the repo root:   . .\scripts\api.ps1

$global:base      = "http://localhost:8080"
$global:NexusRoot = Split-Path $PSScriptRoot -Parent      # repo root, captured while the script loads

function global:Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = "$global:base$Path"; Headers = $headers; ContentType = "application/json" }
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

function global:Sql {
    param([string]$Query)
    $envFile = Join-Path $global:NexusRoot ".env"
    $pw = (Select-String -Path $envFile -Pattern '^DB_PASSWORD=').Line.Split('=', 2)[1]
    docker exec nexus-mysql mysql -u nexus "-p$pw" nexus -e $Query
}

function global:Login {
    param([string]$Phone)
    Api POST /auth/otp/send @{ phone = $Phone } | Out-Null      # 429 cooldown is fine: last OTP still works
    $otp = Read-Host "OTP for $Phone (copy from bootRun console)"
    $v = Api POST /auth/otp/verify @{ phone = $Phone; otp = $otp }
    if ($v.newUser) {
        Write-Host "$Phone is not registered yet - sign up first" -ForegroundColor Yellow
        return $null
    }
    return $v.tokens
}

Write-Host "Helpers loaded: Api, Sql, Login  (root: $global:NexusRoot)" -ForegroundColor Cyan