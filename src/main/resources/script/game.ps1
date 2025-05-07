# ============================================================
# 🚀 自动连接iSCSI、初始化磁盘、固定盘符、创建快捷方式
# ============================================================

# ⚙️ 参数设置
$targetPortal = "192.168.9.141"                   # iSCSI服务器地址
$desiredDriveLetter = "D"                          # 希望挂载到的盘符
$volumeLabel = "GameDisk"                          # 磁盘标签
$exeRelativePath = "Wooduan\SSJ2-WD\WDlauncher.exe" # 盘内程序相对路径
$shortcutName = "启动游戏.lnk"                      # 快捷方式文件名

Write-Host "========== 游戏盘挂载开始 ==========" -ForegroundColor Cyan

# 🚀 动态获取MAC地址生成IQN
try {
    $mac = (Get-NetAdapter | Where-Object { $_.Status -eq "Up" -and $_.MacAddress -ne $null } | Select-Object -First 1).MacAddress
    if (-not $mac) { throw "未获取到有效网卡，请检查网络连接。" }
    $macFormatted = $mac -replace '-', ''
    $targetIQN = "iqn.2005-10.zlb:game-$macFormatted"
    Write-Host "使用MAC地址: $mac -> IQN: $targetIQN"
} catch {
    Write-Host "❌ 错误：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# ➡️ 添加Portal
New-IscsiTargetPortal -TargetPortalAddress $targetPortal -ErrorAction SilentlyContinue

# ➡️ 清理残留连接（如果存在）
$existingSession = Get-IscsiSession | Where-Object { $_.TargetNodeAddress -eq $targetIQN }
if ($existingSession) {
    Write-Host "检测到已有连接，正在断开..."
    Disconnect-IscsiTarget -SessionIdentifier $existingSession.SessionIdentifier -Confirm:$false
    Start-Sleep -Seconds 2
}

# ➡️ 尝试连接Target
$node = Get-IscsiTarget | Where-Object { $_.NodeAddress -eq $targetIQN }
if ($node -and $node.IsConnected -eq $false) {
    Write-Host "正在连接 iSCSI 目标..."
    Connect-IscsiTarget -NodeAddress $targetIQN -IsPersistent $true
    Start-Sleep -Seconds 5
} elseif ($node -and $node.IsConnected -eq $true) {
    Write-Host "目标已连接，跳过连接步骤。"
} else {
    Write-Host "❌ 错误：未找到 iSCSI 目标 IQN！" -ForegroundColor Red
    exit 1
}

# ➡️ 查找磁盘
Start-Sleep -Seconds 2
$disk = Get-Disk | Where-Object { $_.PartitionStyle -eq 'RAW' -or $_.FriendlyName -like "*iSCSI*" }

if (-not $disk) {
    Write-Host "❌ 错误：未检测到iSCSI磁盘，请检查连接状态。" -ForegroundColor Red
    exit 1
}

# ➡️ 初始化新磁盘（如果是RAW）
if ($disk.PartitionStyle -eq 'RAW') {
    Write-Host "初始化新磁盘..."
    Initialize-Disk -Number $disk.Number -PartitionStyle GPT
    $partition = New-Partition -DiskNumber $disk.Number -UseMaximumSize -AssignDriveLetter
    Format-Volume -Partition $partition -FileSystem NTFS -NewFileSystemLabel $volumeLabel -Confirm:$false
} else {
    Write-Host "磁盘已初始化，继续挂载..."
    $partition = Get-Partition -DiskNumber $disk.Number | Where-Object { $_.Type -ne 'Reserved' }
}


# ➡️ 检查挂载是否完成
$targetDriveRoot = "${desiredDriveLetter}:\"
if (-not (Test-Path $targetDriveRoot)) {
    Write-Host "❌ 错误：挂载盘符失败！" -ForegroundColor Red
    exit 1
}

# ============================================================
# 🚀 创建桌面快捷方式
# ============================================================

# 拼接目标程序路径
$targetExePath = Join-Path ("$desiredDriveLetter`:\") $exeRelativePath

# 桌面路径
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath $shortcutName

# 检查程序是否存在
if (-Not (Test-Path $targetExePath)) {
    Write-Host "❌ 错误：程序 $targetExePath 不存在！" -ForegroundColor Red
    exit 1
}

# 创建快捷方式
$wshShell = New-Object -ComObject WScript.Shell
$shortcut = $wshShell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetExePath
$shortcut.WorkingDirectory = Split-Path $targetExePath -Parent
$shortcut.WindowStyle = 1
$shortcut.Description = "启动游戏"
$shortcut.IconLocation = "$targetExePath,0"
$shortcut.Save()

Write-Host "✅ 成功创建桌面快捷方式：$shortcutPath" -ForegroundColor Green

Write-Host "========== 游戏盘挂载完成 ✅ ==========" -ForegroundColor Cyan
