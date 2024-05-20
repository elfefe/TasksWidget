package com.elfefe.common.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class Timer(val onDone: () -> Unit = {}, val onTick: (Long) -> Unit = {}) {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    private val isStarted = AtomicBoolean(false)
    private val isCanceled = AtomicBoolean(false)

    fun start(seconds: Long = 0, minutes: Long = 0, hours: Long = 0) {
        if (isStarted.getAndSet(true)) return
        scope.launch {
            var countdown = seconds * 50 + minutes * 60 + hours * 3600
            while (countdown-- > 0) {
                onTick(countdown)
                Thread.sleep(20)
                if (isCanceled.get()) break
            }
            if (!isCanceled.getAndSet(false)) onDone()
            isStarted.set(false)
        }
    }

    fun cancel() {
        if (isStarted.get()) isCanceled.set(true)
    }
}