package com.elfefe.common.controller

import androidx.compose.ui.res.useResource
import java.io.File
import javax.swing.filechooser.FileSystemView


const val APPDATA_ENV = "LOCALAPPDATA"
const val USERHOME_ENV = "HOMEPATH"

const val IS_DEV = "IS_DEV"


const val APP_PRIVATE_DIR = "TasksWidget"
const val APP_PUBLIC_DIR = "Tasks"

const val TASKS_FILENAME = "tasks.json"
const val CONFIGS_FILENAME = "configs.json"

val isDevEnvironment = System.getenv(IS_DEV) == "true"

val appDataPath = System.getenv(APPDATA_ENV)
val userPath = System.getenv(USERHOME_ENV)

val installDir: String by lazy { File("").absolutePath }
val appFile: File by lazy { File(installDir, "TasksWidget.exe") }
val userDir: File by lazy { File(userPath) }
val appPrivateDir: File by lazy { File(appDataPath, APP_PRIVATE_DIR).apply { mkdirs() } }
val publicDir: File by lazy {
    File(FileSystemView.getFileSystemView().defaultDirectory.path, APP_PUBLIC_DIR).apply { mkdirs() }
}

val tmpDir: File by lazy { File(appPrivateDir, "tmp").apply { mkdirs() } }

val tasksFile: File by lazy { File(publicDir, TASKS_FILENAME).apply { createNewFile() } }
val configsFile: File by lazy { File(appPrivateDir, CONFIGS_FILENAME).apply { createNewFile() } }

val exePath: String = installDir + File.separator + "TasksWidget.exe"

val createShortcutScript: File by lazy { File(appPrivateDir, "create_shortcut.ps1") }
val deleteShortcutScript: File by lazy { File(appPrivateDir, "delete_shortcut.ps1") }
val startupAppFile: File by lazy { File(appDataPath.replace("Local", "Roaming"), "Microsoft\\Windows\\Start Menu\\Programs\\Startup\\TasksWidget.lnk") }

fun generatePowerShellScript() {
    createShortcutScript.writeText(
        useResource(createShortcutScript.name)
            { it.readAllBytes().decodeToString() }
            .replace("{{exePath}}", appFile.absolutePath)
            .replace("{{startupPath}}", startupAppFile.absolutePath)
    )

    deleteShortcutScript.writeText(
        useResource(deleteShortcutScript.name)
            { it.readAllBytes().decodeToString() }
            .replace("{{startupPath}}", startupAppFile.absolutePath)
    )
}

fun createShortcutWithAdminRights(): String {
    if (!createShortcutScript.exists()) return "PowerShell script not found"

    val scriptPath = createShortcutScript.absolutePath

    val command = arrayOf(
        "powershell",
        "-Command",
        "Start-Process",
        "powershell",
        "-ArgumentList",
        "'-ExecutionPolicy Bypass -File $scriptPath'",
        "-Verb",
        "runAs"
    )

    val result = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()

    return if (result == 0) "Shortcut created successfully" else "Failed to create shortcut, status code $result"
}

fun deleteShortcutWithAdminRights(): String {
    if (!deleteShortcutScript.exists()) return "PowerShell script not found"

    val scriptPath = deleteShortcutScript.absolutePath

    val command = arrayOf(
        "powershell",
        "-Command",
        "Start-Process",
        "powershell",
        "-ArgumentList",
        "'-ExecutionPolicy Bypass -File $scriptPath'",
        "-Verb",
        "runAs"
    )


    val result = ProcessBuilder(*command)
        .inheritIO()
        .start()
        .waitFor()

    return if (result == 0) "Shortcut deleted successfully" else "Failed to delete shortcut, status code $result"
}

