package com.elfefe.common.model

data class Configs(
    var taskFieldsOrder: List<TaskFieldOrder> = listOf(
        TaskFieldOrder(name = "title", priority = 0, active = false),
        TaskFieldOrder(name = "description", priority = 0, active = false),
        TaskFieldOrder(name = "deadline", priority = 2, active = true),
        TaskFieldOrder(name = "done", priority = -1, active = true),
        TaskFieldOrder(name = "created", priority = 0, active = true),
        TaskFieldOrder(name = "edited", priority = 0, active = false),
    ),
)