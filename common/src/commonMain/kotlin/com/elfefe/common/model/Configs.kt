package com.elfefe.common.model

import com.elfefe.common.controller.getDate

data class Configs(
    var taskFieldOrders: List<TaskFieldOrder> = listOf(
        TaskFieldOrder(name = "title", priority = 0, active = false),
        TaskFieldOrder(name = "description", priority = 0, active = false),
        TaskFieldOrder(name = "deadline", priority = 2, active = true),
        TaskFieldOrder(name = "done", priority = -1, active = true),
        TaskFieldOrder(name = "created", priority = 1, active = true),
        TaskFieldOrder(name = "edited", priority = 0, active = false),
    ),
)