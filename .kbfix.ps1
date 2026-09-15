$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$K = 'D:\workspace\AI-Fullstack-Knowledge'
$enc = New-Object System.Text.UTF8Encoding($false)
$base = Join-Path $K '00-索引'

Write-Host '=== 链接完整性（正确解析相对路径）==='
$mt = [System.IO.File]::ReadAllText((Join-Path $base '问题总索引.md'), [System.Text.Encoding]::UTF8)
$missing = 0; $total = 0
foreach ($m in [regex]::Matches($mt, '\[\[(\.\./[^|\]]+)\|')) {
  $total++
  $rel = $m.Groups[1].Value -replace '/', '\'
  # 相对 00-索引 目录解析
  $p = [System.IO.Path]::GetFullPath((Join-Path $base $rel)) + '.md'
  if (-not (Test-Path -LiteralPath $p)) { Write-Host ("  断链: {0}" -f $m.Groups[1].Value); $missing++ }
}
Write-Host ("  检查 {0} 条链接，断链 {1} 条" -f $total, $missing)

Write-Host ''
Write-Host '=== 整理索引文件中多余的空行 ==='
$files = @(
  (Join-Path $K '00-索引\问题总索引.md'),
  (Join-Path $K '01-开发环境\问题索引.md'),
  (Join-Path $K '03-后端开发\问题索引.md'),
  (Join-Path $K '04-前端开发\问题索引.md'),
  (Join-Path $K '05-数据库与检索\问题索引.md'),
  (Join-Path $K '07-安全与权限\问题索引.md')
)
foreach ($f in $files) {
  $t = [System.IO.File]::ReadAllText($f, [System.Text.Encoding]::UTF8)
  $orig = $t
  # 连续两个以上空行压成一个（保留段落感但不产生列表断裂）
  $t = [regex]::Replace($t, '(\r?\n){3,}', "`r`n`r`n")
  if ($t -ne $orig) { [System.IO.File]::WriteAllText($f, $t, $enc); Write-Host ("  已整理 {0}" -f (Split-Path $f -Leaf)) }
  else { Write-Host ("  无需整理 {0}" -f (Split-Path $f -Leaf)) }
}

Write-Host ''
Write-Host '=== 07-安全与权限/问题索引.md 复查 ==='
Get-Content (Join-Path $K '07-安全与权限\问题索引.md') -Encoding UTF8

Write-Host ''
Write-Host '=== 清理最后的临时脚本 ==='
$f = 'D:\workspace\AI全栈心理健康助手\.kb-verify.ps1'
if (Test-Path -LiteralPath $f) { Remove-Item -LiteralPath $f -Force; Write-Host '  已删除 .kb-verify.ps1' }
Set-Location 'D:\workspace\AI全栈心理健康助手'
Write-Host ''
Write-Host '=== 项目仓库是否已被我弄脏（应无 .kb-* / probe* / verify* 残留）==='
git status --short | Where-Object { $_ -match 'kb-|probe|verify' }
$leftover = @(git status --short | Where-Object { $_ -match 'kb-|probe|verify' }).Count
Write-Host ("  残留临时文件: {0}" -f $leftover)
