param(
    [string]$ComposeDirectory = 'D:\DockerData\nebula-docker'
)

$ErrorActionPreference = 'Stop'
$projectDirectory = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$composePath = Join-Path $ComposeDirectory 'docker-compose.yml'
$credentialDirectory = Join-Path $projectDirectory 'nebula-start\.secrets'
$passwordPath = Join-Path $credentialDirectory 'NEBULA_REDIS_PASSWORD'
$redisDirectory = Join-Path $ComposeDirectory '.secrets'
$redisConfigPath = Join-Path $redisDirectory 'redis.conf'
$backupPath = Join-Path $projectDirectory 'nebula-start\target\review-fixes-20261024\external.before\docker-compose.yml'
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Protect-LocalSecretFile([string]$Path) {
    # 仅当前Windows用户、系统和管理员可读写秘密文件，不沿用磁盘的宽松继承权限。
    $security = [System.Security.AccessControl.FileSecurity]::new()
    $security.SetAccessRuleProtection($true,$false)
    $userSid = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($sid in @($userSid, [System.Security.Principal.SecurityIdentifier]::new('S-1-5-18'),
            [System.Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))) {
        $security.AddAccessRule([System.Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','Allow'))
    }
    [System.IO.FileSystemAclExtensions]::SetAccessControl([System.IO.FileInfo]::new($Path),$security)
}

# 只改Redis服务段，保留已有数据卷和Elasticsearch配置，不调用Windows服务管理。
$compose = [System.IO.File]::ReadAllText($composePath)
$match = [regex]::Match($compose, '(?ms)^  redis:\r?\n.*?(?=^  [a-zA-Z0-9_-]+:\r?\n|^volumes:\r?\n|\z)')
if (!$match.Success) { throw '没有找到Redis服务配置，停止以免改错目标。' }
$redisBlock = $match.Value
if ($redisBlock -notmatch 'redis-data:/data') { throw 'Redis数据卷与预期不符，停止以保留数据。' }
New-Item -ItemType Directory -Path (Split-Path -Parent $backupPath) -Force | Out-Null
if (!(Test-Path -LiteralPath $backupPath)) { Copy-Item -LiteralPath $composePath -Destination $backupPath }
New-Item -ItemType Directory -Path $credentialDirectory,$redisDirectory -Force | Out-Null

# 密码仅写入不提交的本地凭据文件；重跑时复用，避免应用和容器凭据不一致。
if (Test-Path -LiteralPath $passwordPath) {
    $redisPassword = [System.IO.File]::ReadAllText($passwordPath).Trim()
    if ($redisPassword -notmatch '^[a-fA-F0-9]{64}$') { throw '已有Redis凭据格式不符，停止以免擅自轮换。' }
} else {
    $redisPassword = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32)).ToLowerInvariant()
    [System.IO.File]::WriteAllText($passwordPath, $redisPassword, $utf8)
}
[System.IO.File]::WriteAllText($redisConfigPath, "appendonly yes`nappendfsync everysec`nrequirepass $redisPassword`n", $utf8)
Protect-LocalSecretFile $passwordPath
Protect-LocalSecretFile $redisConfigPath

$redisBlock = [regex]::Replace($redisBlock, '(?m)^      - "(?:127\.0\.0\.1:)?6379:6379"\r?$', '      - "127.0.0.1:6379:6379"')
if ($redisBlock -notmatch '127\.0\.0\.1:6379:6379') { throw 'Redis端口格式不符合预期，停止以免保留外部监听。' }
if ($redisBlock -notmatch '\.secrets/redis\.conf:') {
    $redisBlock = [regex]::Replace($redisBlock, '(?m)^(      - redis-data:/data)\r?$', '$1' + "`n      - ./.secrets/redis.conf:/usr/local/etc/redis/nebula.conf:ro")
}
$redisBlock = [regex]::Replace($redisBlock, '(?m)^    command:.*\r?$', '    command: redis-server /usr/local/etc/redis/nebula.conf')
$updated = $compose.Substring(0,$match.Index) + $redisBlock + $compose.Substring($match.Index+$match.Length)
[System.IO.File]::WriteAllText($composePath,$updated,$utf8)

& docker compose -f $composePath config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Compose配置检查失败，尚未重建容器。' }
# 仅重建Redis，命名卷继续挂载；不启动或操作Elasticsearch。
& docker compose -f $composePath up -d --no-deps redis
if ($LASTEXITCODE -ne 0) { throw 'Redis配置应用失败，请检查容器状态；数据卷未删除。' }
Write-Output 'Redis已启用本机绑定与认证，应用凭据已保存到本地秘密目录；数据卷保持原有挂载。'
