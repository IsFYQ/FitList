# Configures Supabase Auth: custom SMTP (163) + redirect URLs for this app.
# Reads secrets from ../secrets.properties (gitignored). Does not print passwords.

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$secretsFile = Join-Path $repoRoot "secrets.properties"

if (-not (Test-Path $secretsFile)) {
    Write-Error "Missing secrets.properties. Copy secrets.properties.example and fill in values."
}

function Read-PropertiesFile {
    param([string]$Path)
    $props = @{}
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        $props[$key] = $value
    }
    return $props
}

function Require-Property {
    param($Props, [string]$Name)
    $value = $Props[$Name]
    if ([string]::IsNullOrWhiteSpace($value)) {
        Write-Error "Missing required property in secrets.properties: $Name"
    }
    return $value
}

function Invoke-SupabaseAuthPatch {
    param(
        [string]$AccessToken,
        [string]$ProjectRef,
        [hashtable]$Payload,
        [string]$StepName
    )
    $uri = "https://api.supabase.com/v1/projects/$ProjectRef/config/auth"
    $json = $Payload | ConvertTo-Json -Depth 4 -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    Write-Host "Applying $StepName ..."
    try {
        Invoke-RestMethod -Method Patch -Uri $uri -Headers @{
            Authorization = "Bearer $AccessToken"
            "Content-Type" = "application/json; charset=utf-8"
        } -Body $bytes | Out-Null
        Write-Host "$StepName OK."
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $detail = ""
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $detail = $reader.ReadToEnd()
        }
        Write-Error "Supabase API failed during '$StepName' (HTTP $status).`n$detail"
    }
}

$secrets = Read-PropertiesFile -Path $secretsFile
$accessToken = Require-Property $secrets "SUPABASE_ACCESS_TOKEN"
$projectRef = Require-Property $secrets "SUPABASE_PROJECT_REF"
$smtpAdminEmail = Require-Property $secrets "SMTP_ADMIN_EMAIL"
$smtpHost = Require-Property $secrets "SMTP_HOST"
$smtpPort = Require-Property $secrets "SMTP_PORT"
$smtpUser = Require-Property $secrets "SMTP_USER"
$smtpPass = Require-Property $secrets "SMTP_PASS"
$smtpSenderName = if ($secrets.ContainsKey("SMTP_SENDER_NAME") -and $secrets["SMTP_SENDER_NAME"]) {
    $secrets["SMTP_SENDER_NAME"]
} else {
    "Health Check-in"
}

Write-Host "Updating Supabase Auth config for project: $projectRef"

Invoke-SupabaseAuthPatch -AccessToken $accessToken -ProjectRef $projectRef -StepName "SMTP settings" -Payload @{
    external_email_enabled = $true
    mailer_secure_email_change_enabled = $true
    mailer_autoconfirm = $false
    mailer_allow_unverified_email_sign_ins = $true
    smtp_admin_email = $smtpAdminEmail
    smtp_host = $smtpHost
    smtp_port = $smtpPort
    smtp_user = $smtpUser
    smtp_pass = $smtpPass
    smtp_sender_name = $smtpSenderName
}

Invoke-SupabaseAuthPatch -AccessToken $accessToken -ProjectRef $projectRef -StepName "redirect URLs" -Payload @{
    uri_allow_list = "healthcheckin://reset-password"
}

Write-Host "SMTP configured: $smtpAdminEmail via $smtpHost`:$smtpPort"
Write-Host "Redirect URL: healthcheckin://reset-password"

$gradleProps = Join-Path $repoRoot "gradle.properties"
$supabaseUrl = $secrets["SUPABASE_URL"]
$anonKey = $secrets["SUPABASE_ANON_KEY"]
if ($supabaseUrl -and $anonKey) {
    $lines = @()
    if (Test-Path $gradleProps) {
        $lines = Get-Content $gradleProps | Where-Object {
            $_ -notmatch '^\s*SUPABASE_URL=' -and $_ -notmatch '^\s*SUPABASE_ANON_KEY='
        }
    }
    $lines += "SUPABASE_URL=$supabaseUrl"
    $lines += "SUPABASE_ANON_KEY=$anonKey"
    Set-Content -Path $gradleProps -Value $lines -Encoding UTF8
    Write-Host "Updated gradle.properties with Supabase client settings."
}
