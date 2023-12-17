package com.elfefe.common.model

import com.elfefe.common.controller.getDate

data class Task(
    var title: String = "",
    var description: String = "",
    var deadline: String = getDate(),
    var done: Boolean = false,
    val created: Long = System.currentTimeMillis(),
    var edited: Long = System.currentTimeMillis()
)

data class TaskFieldOrder(
    val name: String,
    var priority: Int,
    var active: Boolean
)
