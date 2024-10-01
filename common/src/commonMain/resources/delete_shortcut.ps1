$ShortcutPath = "{{startupPath}}"

if (Test-Path $ShortcutPath) {
    Remove-Item $ShortcutPath
    Write-Host "Shortcut deleted successfully."
} else {
    Write-Host "Shortcut does not exist."
}