package com.elfefe.common.excel

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType

fun Cell.asString(): String = when (cellType) {
        CellType.NUMERIC -> numericCellValue.toString()
        CellType.STRING -> stringCellValue
        CellType.BOOLEAN -> if (booleanCellValue) booleanCellValue.toString() else ""
        CellType.BLANK, CellType._NONE -> ""
        CellType.FORMULA -> cellFormula
        CellType.ERROR -> errorCellValue.toInt().toString()
        else -> "Unknown"
    }
