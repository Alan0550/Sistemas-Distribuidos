param(
    [Parameter(Mandatory = $true)]
    [string]$Url,

    [Parameter(Mandatory = $true)]
    [string]$Secret,

    # Puede ser JSON directo o path a archivo .json
    [Parameter(Mandatory = $true)]
    [string]$Body,

    # Si se activa, envia firma incorrecta para probar rechazo 401
    [switch]$InvalidSignature,

    # Si se activa, envia solo hex sin prefijo sha256=
    [switch]$RawSignatureOnly,

    # Envia OPTIONS en vez de POST (preflight)
    [switch]$Options
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-BodyText {
    param([string]$BodyInput)
    if (Test-Path $BodyInput) {
        return Get-Content -Raw -Path $BodyInput -Encoding UTF8
    }
    return $BodyInput
}

function Get-HmacSha256Hex {
    param(
        [string]$SecretText,
        [string]$PayloadText
    )
    $keyBytes = [System.Text.Encoding]::UTF8.GetBytes($SecretText)
    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($PayloadText)
    $hmac = [System.Security.Cryptography.HMACSHA256]::new($keyBytes)
    try {
        $hash = $hmac.ComputeHash($payloadBytes)
        return -join ($hash | ForEach-Object { $_.ToString("x2") })
    }
    finally {
        $hmac.Dispose()
    }
}

if ($Options) {
    Write-Host "Enviando OPTIONS a $Url ..."
    $opt = Invoke-WebRequest -Method Options -Uri $Url -Headers @{ "Origin" = "http://localhost" }
    Write-Host "Status: $($opt.StatusCode)"
    Write-Host "Body: $($opt.Content)"
    exit 0
}

$bodyText = Get-BodyText -BodyInput $Body
$signatureHex = Get-HmacSha256Hex -SecretText $Secret -PayloadText $bodyText

if ($InvalidSignature) {
    # Rompe la firma adrede para probar rechazo del servidor
    $signatureHex = "00" + $signatureHex.Substring(2)
}

$signatureHeader = if ($RawSignatureOnly) { $signatureHex } else { "sha256=$signatureHex" }

$headers = @{
    "X-Signature" = $signatureHeader
    "Content-Type" = "application/json"
}

Write-Host "URL: $Url"
Write-Host "X-Signature: $signatureHeader"
Write-Host "Body enviado:"
Write-Host $bodyText
Write-Host ""

try {
    $resp = Invoke-WebRequest -Method Post -Uri $Url -Headers $headers -Body $bodyText -ContentType "application/json"
    Write-Host "Status: $($resp.StatusCode)"
    Write-Host "Respuesta:"
    Write-Host $resp.Content
}
catch {
    if ($_.Exception.Response -ne $null) {
        $status = [int]$_.Exception.Response.StatusCode
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $content = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "Status: $status"
        Write-Host "Respuesta error:"
        Write-Host $content
    }
    else {
        throw
    }
}
