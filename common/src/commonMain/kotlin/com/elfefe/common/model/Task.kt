package com.elfefe.common.model

import com.elfefe.common.getDate

data class Task(
    var title: String = "",
    var description: String = "",
    var deadline: String = getDate(),
    var done: Boolean = false
)
