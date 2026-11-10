param([switch]$ConfigureOnly)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
$dockerRoot='D:/DockerData/nebula-docker'
$compose=Join-Path $dockerRoot 'docker-compose.yml'
$evidence=Join-Path $projectRoot 'nebula-start/target/es-integration-20261107'
New-Item -ItemType Directory -Path $evidence -Force|Out-Null
if(-not(Test-Path -LiteralPath $compose)){throw '正式Docker配置不存在，停止以免覆盖其他环境'}
$original=[IO.File]::ReadAllText($compose)
if(-not(Test-Path "$evidence/docker-compose.before-search.yml")){
 [IO.File]::WriteAllText("$evidence/docker-compose.before-search.yml",$original,[Text.UTF8Encoding]::new($false))
}
$private=Join-Path $dockerRoot '.secrets'
$appPrivate=Join-Path $projectRoot 'nebula-start/.secrets'
New-Item -ItemType Directory -Path $private,$appPrivate -Force|Out-Null
$rootPasswordFile=Join-Path $private 'elastic_password'
$appPasswordFile=Join-Path $appPrivate 'NEBULA_ES_PASSWORD'

function Protect-SearchSecret([string]$Path) {
 # 保留当前用户、系统与管理员的访问权限，不继承磁盘上的宽松文件权限。
 $security=[Security.AccessControl.FileSecurity]::new()
 $security.SetAccessRuleProtection($true,$false)
 $userSid=[Security.Principal.WindowsIdentity]::GetCurrent().User
 foreach($sid in @($userSid,[Security.Principal.SecurityIdentifier]::new('S-1-5-18'),[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544'))){
  $security.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new($sid,'FullControl','Allow'))
 }
 [IO.FileSystemAclExtensions]::SetAccessControl([IO.FileInfo]::new($Path),$security)
}

foreach($file in @($rootPasswordFile,$appPasswordFile)){
 if(-not(Test-Path -LiteralPath $file)){
  $bytes=New-Object byte[] 32
  $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
  try{$rng.GetBytes($bytes)}finally{$rng.Dispose()}
  $value=([BitConverter]::ToString($bytes)).Replace('-','').ToLowerInvariant()
  [IO.File]::WriteAllText($file,$value,[Text.UTF8Encoding]::new($false))
 }
 if([IO.File]::ReadAllText($file).Trim().Length -lt 32){throw '既有ES凭据长度异常，停止；不自动重置已有凭据'}
 Protect-SearchSecret $file
}
$section=@'
  elasticsearch:
    image: elasticsearch:8.19.17
    container_name: nebula-es
    restart: unless-stopped
    mem_limit: 2g
    cpus: 2
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=true
      - xpack.security.http.ssl.enabled=false
      - xpack.security.transport.ssl.enabled=false
      - xpack.ml.enabled=false
      - ingest.geoip.downloader.enabled=false
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
      - ELASTIC_PASSWORD_FILE=/run/secrets/nebula_es_root_password
    # Windows挂载文件的Linux权限会显示777，先在容器内复制为600，再交给官方入口校验。
    entrypoint: ["/bin/tini", "--", "/bin/bash", "-c"]
    command:
      - |
        set -e
        umask 077
        cp /run/secrets/nebula_es_root_password /tmp/nebula_es_password
        chmod 600 /tmp/nebula_es_password
        export ELASTIC_PASSWORD_FILE=/tmp/nebula_es_password
        exec /usr/local/bin/docker-entrypoint.sh eswrapper
    secrets:
      - nebula_es_root_password
    ports:
      - "127.0.0.1:9200:9200"
    volumes:
      - search-data:/usr/share/elasticsearch/data

'@
$servicePattern='(?ms)^  elasticsearch:\r?\n.*?(?=^  [a-zA-Z0-9_-]+:\s*\r?$|^volumes:|^secrets:|\z)'
if(([regex]::Matches($original,$servicePattern)).Count -ne 1){throw 'ES配置段无法唯一识别，停止以免覆盖其它服务'}
$updated=[regex]::Replace($original,$servicePattern,$section)
if($updated -notmatch '(?m)^  search-data:'){
 if($updated -notmatch '(?m)^volumes:'){throw '命名卷配置不存在'}
 $updated=[regex]::Replace($updated,'(?m)^volumes:\s*\r?$',"volumes:`n  search-data:`n    name: nebula-es-data")
}
if($updated -notmatch '(?m)^secrets:'){$updated+="`nsecrets:`n  nebula_es_root_password:`n    file: ./.secrets/elastic_password`n"}
elseif($updated -notmatch '(?m)^  nebula_es_root_password:'){
 $updated=[regex]::Replace($updated,'(?m)^secrets:\s*\r?$',"secrets:`n  nebula_es_root_password:`n    file: ./.secrets/elastic_password")
}
# 先验证候选配置，再原子覆盖正式文件；Redis段与既有卷定义保留。
$candidate=Join-Path $dockerRoot 'docker-compose.search-candidate.yml'
[IO.File]::WriteAllText($candidate,$updated,[Text.UTF8Encoding]::new($false))
docker compose -f $candidate config --quiet
if($LASTEXITCODE -ne 0){throw '候选Compose配置验证失败，正式文件未修改'}
[IO.File]::WriteAllText($compose,$updated,[Text.UTF8Encoding]::new($false))
Remove-Item -LiteralPath $candidate
if($ConfigureOnly){Write-Output '正式ES配置及本地凭据已准备';exit 0}
docker compose -f $compose up -d --no-deps elasticsearch
if($LASTEXITCODE -ne 0){throw '正式ES容器启动失败，保留配置与数据卷'}
$rootPassword=[IO.File]::ReadAllText($rootPasswordFile).Trim()
$headers=@{Authorization='Basic '+[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('elastic:'+$rootPassword))}
$ready=$false
for($attempt=0;$attempt -lt 90;$attempt++){
 try{
  $response=Invoke-RestMethod 'http://127.0.0.1:9200/_cluster/health' -Headers $headers -TimeoutSec 2
  if($response.status -in @('green','yellow')){$ready=$true;break}
 }catch{}
 Start-Sleep -Seconds 1
}
if(-not $ready){throw 'ES尚未就绪，查看容器日志；不输出认证值'}
$role=@{cluster=@();indices=@(@{names=@('nebula-search','nebula-search-*');privileges=@('read','write','create_index','manage','view_index_metadata');allow_restricted_indices=$false})}|ConvertTo-Json -Depth 6
Invoke-RestMethod 'http://127.0.0.1:9200/_security/role/nebula_search' -Method Put -Headers $headers -ContentType 'application/json' -Body $role|Out-Null
$appPassword=[IO.File]::ReadAllText($appPasswordFile).Trim()
$account=@{password=$appPassword;roles=@('nebula_search');enabled=$true}|ConvertTo-Json
Invoke-RestMethod 'http://127.0.0.1:9200/_security/user/nebula_search' -Method Put -Headers $headers -ContentType 'application/json' -Body $account|Out-Null
Write-Output 'nebula-es已就绪；仅监听本机9200，应用使用专用索引账号，凭据未输出'
