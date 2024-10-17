package com.elfefe.common.controller

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.*


fun getDate(): String {
    val date = Calendar.getInstance()
    return date.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0') + (date.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
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
        return Calendar.getInstance().apply {
            val day = date.substring(0, 2).toInt()
            val month = date.substring(2, 4).toInt()
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.MONTH, month - 1)
        }.compareTo(Calendar.getInstance())
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