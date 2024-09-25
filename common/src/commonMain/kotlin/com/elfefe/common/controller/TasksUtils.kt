package com.elfefe.common.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.*


fun getDate(): String {
    val date = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_MONTH).toString() + (date.get(Calendar.MONTH) + 1).toString()
}

fun fromDate(date: String): Long {
    val dateOrder = listOf(Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR)
    date.split("/").let {
        return Calendar.getInstance().apply {
            for (i in it.indices) set(dateOrder[i], it[i].toInt())
        }.timeInMillis
    }
}

fun deadlineDate(date: String): Int {
    val dateOrder = listOf(Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR)
    date.split("/").let {
        return Calendar.getInstance().apply {
            for (i in it.indices) set(dateOrder[i], it[i].toInt() - if (i == 1) 1 else 0)
        }.compareTo(Calendar.getInstance())
    }
}



@Composable
fun Int.scaledSp(): TextUnit {
    val value: Int = this
    return with(LocalDensity.current) {
        val fontScale = this.fontScale
        val textSize = value / fontScale
        textSize.sp
    }
}