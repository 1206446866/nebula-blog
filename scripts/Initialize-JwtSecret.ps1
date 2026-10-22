[CmdletBinding()]
param([switch]$Rotate)

$ErrorActionPreference = 'Stop'
$secretDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'nebula-start/.secrets'
$secretFile = Join-Path $secretDirectory 'NEBULA_JWT_SECRET'
if ((Test-Path -LiteralPath $secretFile) -and -not $Rotate) {
    throw 'A local JWT key already exists. Use -Rotate only when all current logins should expire.'
}

New-Item -ItemType Directory -Path $secretDirectory -Force | Out-Null
# Limit this local development directory to the current Windows user.
$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
$acl = Get-Acl -LiteralPath $secretDirectory
$acl.SetAccessRuleProtection($true, $false)
foreach ($existingRule in @($acl.Access)) { $acl.RemoveAccessRuleSpecific($existingRule) }
$rule = New-Object System.Security.AccessControl.FileSystemAccessRule(
    $identity, 'FullControl', 'ContainerInherit, ObjectInherit', 'None', 'Allow')
$acl.AddAccessRule($rule)
# Set only the DACL; PowerShell's Set-Acl can also request an audit privilege.
if ('System.IO.FileSystemAclExtensions' -as [type]) {
    [System.IO.FileSystemAclExtensions]::SetAccessControl([System.IO.DirectoryInfo]::new($secretDirectory), $acl)
} else {
    [System.IO.Directory]::SetAccessControl($secretDirectory, $acl)
}

$temporaryFile = Join-Path $secretDirectory ([Guid]::NewGuid().ToString('N') + '.tmp')
$bytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($bytes)
    [System.IO.File]::WriteAllText($temporaryFile, [Convert]::ToBase64String($bytes),
        (New-Object System.Text.UTF8Encoding($false)))
    if (Test-Path -LiteralPath $secretFile) {
        [System.IO.File]::Replace($temporaryFile, $secretFile,
            [System.Management.Automation.Language.NullString]::Value)
    } else {
        [System.IO.File]::Move($temporaryFile, $secretFile)
    }
    Write-Output 'Local JWT key saved without displaying its value. Restart the backend to load it.'
} finally {
    $random.Dispose()
    [Array]::Clear($bytes, 0, $bytes.Length)
    if (Test-Path -LiteralPath $temporaryFile) {
        Remove-Item -LiteralPath $temporaryFile
    }
}
