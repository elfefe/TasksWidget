package com.elfefe.common.logs


class LogState(val text: String, val status: Status) {
    enum class Status {
        INFO,
        WARNING,
        ERROR
    }
}