package com.elfefe.common.controller

import kotlinx.coroutines.*

class ConfigsAutoLoader(private val scope: CoroutineScope) {
    private var autoLoaderJob: Job? = null

    fun launch() {
        if (autoLoaderJob?.isActive == true) return

        autoLoaderJob = scope.launch {
            while (autoLoaderJob?.isActive == true) {
                Tasks.Configs.loadConfigs()
                Tasks.refresh()
                delay(50)
            }
        }
    }
}