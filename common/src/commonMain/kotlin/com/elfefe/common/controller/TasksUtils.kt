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

/**
 * Compare une echeance au format `JJMM` a aujourd'hui.
 *
 * @return negatif si l'echeance est passee, positif si elle est a venir, 0 si
 *   c'est aujourd'hui.
 *
 * Deux bugs corriges ici : le mois n'etait jamais lu (`date.length > 4` est
 * faux pour une date de quatre caracteres), et la comparaison portait sur
 * l'instant complet, heure comprise — deux `getInstance()` a quelques
 * millisecondes d'ecart ne donnent donc jamais 0 pour aujourd'hui. On compare
 * desormais des dates a l'heure remise a zero.
 */
fun deadlineDate(date: String): Int {
    val day = if (date.length >= 2) date.substring(0, 2).toIntOrNull() ?: 0 else 0
    val month = if (date.length >= 4) date.substring(2, 4).toIntOrNull() ?: 0 else 0

    fun Calendar.aMinuit() = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val echeance = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.MONTH, month - 1)
    }.aMinuit()

    return echeance.compareTo(Calendar.getInstance().aMinuit())
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