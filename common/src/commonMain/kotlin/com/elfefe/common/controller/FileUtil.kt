package com.elfefe.common.controller

import java.io.File
import javax.swing.filechooser.FileSystemView
import kotlin.system.exitProcess


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
val appDir: File by lazy { File(installDir, "TasksWidget.exe") }
val userDir: File by lazy { File(userPath) }
val appPrivateDir: File by lazy { File(appDataPath, APP_PRIVATE_DIR).apply { mkdirs() } }
val publicDir: File by lazy {
    File(FileSystemView.getFileSystemView().defaultDirectory.path, APP_PUBLIC_DIR).apply { mkdirs() }
}

val tmpDir: File by lazy { File(appPrivateDir, "tmp").apply { mkdirs() } }

val tasksFile: File by lazy { File(publicDir, TASKS_FILENAME).apply { createNewFile() } }
val configsFile: File by lazy { File(appPrivateDir, CONFIGS_FILENAME).apply { createNewFile() } }

val exePath: String = installDir + File.separator + "TasksWidget.exe"

fun isAdmin(): Boolean {
    return try {
        ProcessBuilder("net", "session")
            .redirectErrorStream(true)
            .start()
            .waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

fun askForAdminRights() {
    val command = arrayOf(
        "powershell",
        "-Command",
        "Start-Process",
        "'${exePath}'",
        "-Verb",
        "runAs"
    )

    ProcessBuilder(*command)
        .inheritIO()
        .start()

    exitProcess(0)
}

