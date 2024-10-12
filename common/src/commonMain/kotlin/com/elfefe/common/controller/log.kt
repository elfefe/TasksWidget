package com.elfefe.common.controller

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Any.log(message: String) {
    val log = "${javaClass.simpleName}: $message"
    println(log)
    logsFile.appendText("${Instant                             // Represents a moment as seen in UTC, that is, with an offset from UTC of zero hours-minutes-seconds.
        .ofEpochMilli(System.currentTimeMillis())              // Returns a `long` value, a count of milliseconds since the epoch reference of 1970-01-01T00:00Z.                              // Returns an `Instant` object.
        .atZone(ZoneId.systemDefault())                        // Returns a `ZonedDateTime` object.
        .toLocalDateTime()                                         // Returns a `LocalDate` object.
        .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))   // Returns a `DateTimeFormatter` object.
    } - $log\n")
}