$TargetFile = "{{startupPath}}"
$WScriptShell = New-Object -ComObject WScript.Shell
$Shortcut = $WScriptShell.CreateShortcut($TargetFile)
$Shortcut.TargetPath = "{{exePath}}"
$Shortcut.Save()
