package com.elfefe.common.controller

import java.io.File
import javax.swing.filechooser.FileSystemView


const val APPDATA_ENV = "LOCALAPPDATA"


const val APP_PRIVATE_DIR = "TasksWidget"
const val APP_PUBLIC_DIR = "Tasks"

const val TASKS_FILENAME = "tasks.json"
const val CONFIGS_FILENAME = "configs.json"

val appDataDir = System.getenv(APPDATA_ENV)


val appPrivateDir: File by lazy { File(appDataDir, APP_PRIVATE_DIR).apply { mkdirs() } }
val publicDir: File by lazy {
    File(FileSystemView.getFileSystemView().defaultDirectory.path, APP_PUBLIC_DIR).apply { mkdirs() }
}

val tmpDir: File by lazy { File(appPrivateDir, "tmp").apply { mkdirs() } }

val tasksFile: File by lazy { File(publicDir, TASKS_FILENAME).apply { createNewFile() } }
val configsFile: File by lazy { File(appPrivateDir, CONFIGS_FILENAME).apply { createNewFile() } }