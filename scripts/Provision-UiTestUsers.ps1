#Requires -Version 7

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$accounts = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'src/test/resources/test-users.properties') |
    ConvertFrom-StringData
$api = Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'src/main/resources/api.properties') |
    ConvertFrom-StringData
$baseUrl = if ($env:CONDUIT_API_URL) { $env:CONDUIT_API_URL } else { $api['conduit.apiUrl'] }
$registrationUrl = $baseUrl.TrimEnd('/') + $api['api.users.path.registration']

foreach ($alias in @('testUser', 'testReader')) {
    $emailVariable = $accounts["$alias.email.env"]
    $passwordVariable = $accounts["$alias.password.env"]
    $existingEmail = [Environment]::GetEnvironmentVariable($emailVariable)
    $existingPassword = [Environment]::GetEnvironmentVariable($passwordVariable)
    if ([string]::IsNullOrWhiteSpace($existingEmail) -ne [string]::IsNullOrEmpty($existingPassword)) {
        throw "Set both $emailVariable and $passwordVariable for $alias."
    }
    if (![string]::IsNullOrWhiteSpace($existingEmail)) {
        continue
    }

    $username = 'qa' + [Guid]::NewGuid().ToString('N').Substring(0, 16)
    $email = "$username@example.test"
    $password = 'Qa-' + [Guid]::NewGuid().ToString('N')
    if ($env:GITHUB_ACTIONS -eq 'true') {
        Write-Output "::add-mask::$password"
    }

    $registration = @{ user = @{ username = $username; email = $email; password = $password } } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri $registrationUrl -Method Post -ContentType 'application/json' `
        -Body $registration -SkipHttpErrorCheck -StatusCodeVariable registrationStatus
    if ($registrationStatus -ne [int][Net.HttpStatusCode]::Created) {
        throw "Could not provision $alias (HTTP $registrationStatus)."
    }
    [Environment]::SetEnvironmentVariable($emailVariable, $email, 'Process')
    [Environment]::SetEnvironmentVariable($passwordVariable, $password, 'Process')
    Write-Output "Provisioned $alias for this shell."
}
